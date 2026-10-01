package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.google.common.base.Predicates;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.SignalPropagator;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;

/**
 * 四显示档位的<b>前方闭塞分区链</b>构建与求值。
 *
 * <h4>闭塞分区在 Create 中的定义</h4>
 * <p>信号边界两侧各持有一个 {@link SignalEdgeGroup}（{@code SignalBoundary.groups}），
 * 无信号边界的长轨道段由 {@code EdgeData.singleSignalGroup} 承载。
 * "前方第 k 个闭塞分区"即沿信号链前推 k 跳所对应的组。</p>
 *
 * <h4>侧向索引约定（关键）</h4>
 * <p>Create 中 {@code SignalPropagator.walkSignals(graph, signal, front)} 的 BFS 在检测到
 * 下一个信号边界 B 时，回调参数是 {@code Pair.of(currentNode, B)}，其中
 * {@code currentNode} 是<b>沿 B 所在边行进的起点节点</b>。
 * {@code propagateSignalGroup} 对该回调执行
 * {@code B.setGroupAndUpdate(currentNode, G)}，即 {@code B.setGroup(B.isPrimary(currentNode), G)}。
 * 因此：</p>
 * <pre>
 * B 面向来路的一侧（即与 X 同处一个闭塞分区的一侧） = B.isPrimary(currentNode)
 * B 越过之后的一侧（即前方下一个闭塞分区）        = !B.isPrimary(currentNode)
 * </pre>
 * <p>本类的 {@code facingUs} 即 {@code B.isPrimary(currentNode)}。</p>
 */
public final class FourSignalChain {

    /** 链上最多关心的闭塞分区数：3 个空闲即判定绿灯，无需继续前推。 */
    public static final int MAX_BLOCKS = 3;

    private FourSignalChain() {
    }

    /**
     * 从信号的一侧构建前方闭塞分区链。
     *
     * <p>只读遍历：{@code walkSignals} 传 {@code forCollection = true}，
     * 因此不会触发 {@code notifyTrains} / 边数据重同步等副作用。</p>
     */
    public static Chain build(TrackGraph graph, FourSignalBoundary signal, boolean side) {
        UUID first = signal.groups.get(side);
        if ( first == null )
            return Chain.brokenChain();

        List<UUID> blocks = new ArrayList<>(MAX_BLOCKS);
        blocks.add(first);

        Set<VisitKey> visited = new HashSet<>();
        List<Step> frontier = List.of(new Step(signal, side));

        MutableBoolean ranOut = new MutableBoolean(false);
        MutableBoolean broken = new MutableBoolean(false);

        // 已有 1 个分区，再前推 MAX_BLOCKS - 1 跳即可判定绿灯
        for ( int depth = 0; depth < MAX_BLOCKS - 1; depth++ ) {
            LinkedHashSet<UUID> nextBlocks = new LinkedHashSet<>();
            List<Step> nextFrontier = new ArrayList<>();

            for ( Step step : frontier ) {
                MutableBoolean reachedBoundary = new MutableBoolean(false);

                SignalPropagator.walkSignals(graph, step.signal(), step.side(), pair -> {
                    SignalBoundary boundary = pair.getSecond();
                    // 面向来路的一侧（见类注释的索引约定）
                    boolean facingUs = boundary.isPrimary(pair.getFirst());
                    reachedBoundary.setTrue();

                    if ( ! visited.add(new VisitKey(boundary.id, facingUs)) )
                        return false; // 环线，避免无限往返

                    // 越过该边界之后的闭塞分区
                    UUID beyond = boundary.groups.get(! facingUs);
                    if ( beyond == null )
                        broken.setTrue();
                    else
                        nextBlocks.add(beyond);

                    // 只有本模组的信号机才能继续按四显示规则前推
                    if ( boundary instanceof FourSignalBoundary four )
                        nextFrontier.add(new Step(four, ! facingUs));
                    return false;

                }, Predicates.alwaysFalse(), true);

                if ( ! reachedBoundary.booleanValue() )
                    ranOut.setTrue();
            }

            if ( nextBlocks.isEmpty() ) {
                ranOut.setTrue();
                break;
            }

            for ( UUID group : nextBlocks ) {
                if ( blocks.size() >= MAX_BLOCKS )
                    break;
                blocks.add(group);
            }

            if ( nextFrontier.isEmpty() ) {
                ranOut.setTrue();
                break;
            }
            frontier = nextFrontier;
        }

        return new Chain(List.copyOf(blocks), ranOut.booleanValue(), broken.booleanValue());
    }

    /**
     * 主算法：按前方连续空闲闭塞分区数量换算档位。
     *
     * @return 档位；链不可用（断裂）时返回 {@code null}，由调用方回退到级联规则
     */
    public static FourAspectState resolve(FourSignalBoundary signal, Chain chain) {
        if ( chain == null || chain.broken() )
            return null;

        Map<UUID, SignalEdgeGroup> allGroups = Create.RAILWAYS.signalEdgeGroups;
        int free = 0;

        for ( UUID groupId : chain.blockGroups() ) {
            SignalEdgeGroup group = allGroups.get(groupId);
            if ( group == null ) {
                // 紧邻前方分区都查不到：与原版 tickState 一致，判为 INVALID
                if ( free == 0 )
                    return FourAspectState.INVALID;
                // 更远处查不到：视为链断裂，交给级联规则
                return null;
            }
            // 与原版 SignalBoundary.tickState 使用完全相同的占用判定
            if ( group.isOccupiedUnless(signal) )
                break;
            free++;
        }

        // 已探明分区全空闲且前方再无信号机 -> 无限空闲 = 绿灯
        if ( free == chain.blockGroups()
                .size() && chain.ranOutOfSignals() )
            return FourAspectState.GREEN;

        return FourAspectState.fromFreeBlocks(free);
    }

    /**
     * 兜底：链断裂 / 分岔歧义时退化为「下游最严格档位降一级」。
     *
     * <p>取值方式与原版 {@code SignalBoundary.resolveSignalChain} 保持一致：
     * {@code collectChainedSignals} 返回的布尔值是下游信号<b>越过之后那一侧</b>的索引，
     * 因此直接用它读下游信号的档位。</p>
     */
    public static FourAspectState resolveByCascade(TrackGraph graph, FourSignalBoundary signal, boolean side) {
        Map<UUID, Boolean> downstream = SignalPropagator.collectChainedSignals(graph, signal, side);
        if ( downstream.isEmpty() )
            return FourAspectState.GREEN;

        FourAspectState worst = null;
        boolean sawAny = false;

        for ( Map.Entry<UUID, Boolean> entry : downstream.entrySet() ) {
            TrackEdgePoint point = graph.getPoint(EdgePointType.SIGNAL, entry.getKey());
            // 下游是原版信号机：原版只有红/黄/绿，没有绿黄，不参与四显示降级
            if ( ! (point instanceof FourSignalBoundary other) )
                continue;
            if ( other.blockEntities.get(entry.getValue())
                    .isEmpty() )
                continue;
            sawAny = true;
            worst = FourAspectState.mostRestrictive(worst, other.cachedAspects.get(entry.getValue()));
        }

        if ( ! sawAny || worst == null )
            return FourAspectState.GREEN;

        return FourAspectState.fromNextState(worst);
    }

    /** 链上的一步：从哪个信号、哪一侧继续前推。 */
    private record Step(SignalBoundary signal, boolean side) {

    }

    /** 环线去重键：同一个信号的同一侧只推进一次。 */
    private record VisitKey(UUID id, boolean side) {

    }

    /**
     * 前方闭塞分区链。
     *
     * @param blockGroups     有序闭塞分区组 id，index 0 为紧邻前方
     * @param ranOutOfSignals 链自然终止（前方已无更多信号机）
     * @param broken          链断裂（某信号边界缺少面向来路那一侧的组），需回退级联规则
     */
    public record Chain(List<UUID> blockGroups, boolean ranOutOfSignals, boolean broken) {

        /** 链不可用（存在信号边界但缺少组），需回退到级联规则。 */
        public static Chain brokenChain() {
            return new Chain(List.of(), false, true);
        }

    }

}
