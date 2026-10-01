package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 四显示信号边界。
 *
 * <p><b>架构</b>：本类直接继承 Create 的 {@link SignalBoundary}，因此</p>
 * <ul>
 *   <li>闭塞分组（{@code groups}）、组传播（{@code SignalPropagator}）、
 *       占用判定（{@code SignalEdgeGroup}）全部沿用 Create 原版实现，不重复造轮子；</li>
 *   <li>所有以 {@code instanceof SignalBoundary} 或 {@code SignalBoundary} 形参形式与信号交互的
 *       Create 代码（列车停车、导航寻路惩罚、组预留、列车地图）都能自动识别本类型；</li>
 *   <li>四显示档位是本类新增的 {@link #cachedAspects}，<b>只影响显示</b>，
 *       不参与 Create 的通行判定。</li>
 * </ul>
 *
 * <p><b>为什么能共用闭塞系统</b>：Create 判断列车能否通过时读的是
 * {@code SignalEdgeGroup.isOccupiedUnless(...)}（由 {@code groups} 决定），
 * 而 {@code groups} 由 {@code SignalPropagator} 依据
 * {@code EdgeData.singleSignalGroup} / {@code next(EdgePointType.SIGNAL, ...)} 建立。
 * 这两处对 {@code EdgePointType.SIGNAL} 的硬编码由本模组的 Mixin 补齐，
 * 详见 {@code top.redstarmc.mod.createlocomotivedepot.mixin}。</p>
 */
public class FourSignalBoundary extends SignalBoundary {

    /**
     * 每侧缓存的四显示档位。继承自父类的 {@code cachedStates} 仍是原版三态，
     * 仅用于满足 Create 的既有接口；本模组的显示一律读 {@link #cachedAspects}。
     */
    public Couple<FourAspectState> cachedAspects;

    /** 每侧的前方闭塞分区链缓存，结构变化时置空重算。 */
    private Couple<FourSignalChain.Chain> chains;

    public FourSignalBoundary() {
        super();
        cachedAspects = Couple.create(() -> FourAspectState.INVALID);
        chains = Couple.create(null, null);
    }

    // ---------- 状态求值 ----------

    /**
     * 计算两侧的四显示档位。
     *
     * <p>求值顺序与父类 {@code tickState} 保持一致：</p>
     * <ol>
     *   <li>红石强制红 —— 与父类共用 {@code blockEntities} 中的供电标记；</li>
     *   <li>两侧同组（废弃/重复的边界）→ INVALID；</li>
     *   <li>本侧紧邻闭塞分区被占用 → RED；</li>
     *   <li>否则按「前方空闲闭塞分区数」定档，链不可用时退化为下游级联降级。</li>
     * </ol>
     */
    private void tickAspects(TrackGraph graph) {
        for ( boolean side : Iterate.trueAndFalse ) {
            if ( blockEntities.get(side).isEmpty() )
                continue;

            // 1) 红石强制红
            if ( isForcedRed(side) ) {
                cachedAspects.set(side, FourAspectState.RED);
                continue;
            }

            // 2) 两侧同组 -> 边界未被正确分隔，视为无效
            java.util.UUID group = groups.get(side);
            if ( java.util.Objects.equals(group, groups.get(! side)) ) {
                cachedAspects.set(side, FourAspectState.INVALID);
                continue;
            }
            if ( group == null ) {
                cachedAspects.set(side, FourAspectState.INVALID);
                continue;
            }

            // 3)+4) 分区计数为主
            FourSignalChain.Chain chain = chains.get(side);
            if ( chain == null ) {
                chain = FourSignalChain.build(graph, this, side);
                chains.set(side, chain);
            }

            FourAspectState resolved = FourSignalChain.resolve(this, chain);
            // 链断裂 / 分岔歧义 -> 回退到「下游最严格档位降一级」
            if ( resolved == null )
                resolved = FourSignalChain.resolveByCascade(graph, this, side);

            cachedAspects.set(side, resolved);
        }
    }

    /** 使某一侧的档位与链缓存在下一 tick 重算。 */
    public void invalidateAspects() {
        chains.set(true, null);
        chains.set(false, null);
    }

    /** 取某个信号机方块所在一侧的档位。 */
    public FourAspectState getAspectFor(BlockPos blockEntityPos) {
        for ( boolean side : Iterate.trueAndFalse ) {
            if ( blockEntities.get(side).containsKey(blockEntityPos) )
                return cachedAspects.get(side);
        }
        return FourAspectState.INVALID;
    }

    @Nullable
    public FourAspectState getAspect(boolean primary) {
        return cachedAspects.get(primary);
    }

    /** 更新某个四显示信号灯方块上报的红石供电状态。 */
    public void updateBlockEntityPower(FourSignalBlockEntity blockEntity) {
        for ( boolean side : Iterate.trueAndFalse )
            blockEntities.get(side)
                    .computeIfPresent(blockEntity.getBlockPos(), (p, c) -> blockEntity.getReportedPower());
        invalidateAspects();
    }

    // ---------- 方块绑定 ----------

    @Override
    public void invalidate(LevelAccessor level) {
        super.invalidate(level);
        invalidateAspects();
    }

    @Override
    public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
        // 只与同类型共存；与原版信号机放在同一位置会被拒绝，避免两套信号叠加
        return otherType == getType();
    }

    @Override
    public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
        // 不调用 super：父类把 reportedPower 的读取硬编码到了 SignalBlockEntity。
        // 本模组的信号灯方块没有 SignalBlock.TYPE 属性，types 保持默认 ENTRY_SIGNAL 即可。
        Map<BlockPos, Boolean> side = blockEntities.get(front);
        side.put(blockEntity.getBlockPos(),
                blockEntity instanceof FourSignalBlockEntity four && four.getReportedPower());
        invalidateAspects();
    }

    @Override
    public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
        super.blockEntityRemoved(blockEntityPos, front);
        invalidateAspects();
    }

    /**
     * 轨道覆盖层状态。
     *
     * <p>父类实现在内层循环里提前 return，只有每侧的第一个方块能命中；
     * 这里给出正确实现，保证同一侧挂多个信号灯方块时都返回正确结果。</p>
     */
    @Override
    public OverlayState getOverlayFor(BlockPos blockEntity) {
        for ( boolean side : Iterate.trueAndFalse ) {
            if ( blockEntities.get(side).containsKey(blockEntity) )
                return blockEntities.get(! side).isEmpty() ? OverlayState.RENDER : OverlayState.DUAL;
        }
        return OverlayState.SKIP;
    }

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);

        if ( preTrains ) {
            // 分组刚被重建，链缓存失效
            chains.set(true, null);
            chains.set(false, null);
            return;
        }

        tickAspects(graph);
    }

    // ---------- 序列化 ----------

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean migration, DimensionPalette dimensions) {
        super.read(nbt, registries, migration, dimensions);
        if ( migration )
            return;

        for ( int i = 1; i <= 2; i++ )
            cachedAspects.set(i == 1, NBTHelper.readEnum(nbt, "Aspect" + i, FourAspectState.class));

        invalidateAspects();
    }

    @Override
    public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.read(buffer, dimensions);
        for ( int i = 1; i <= 2; i++ )
            cachedAspects.set(i == 1, FourAspectState.values()[buffer.readVarInt()]);
        invalidateAspects();
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
        super.write(nbt, registries, dimensions);
        for ( int i = 1; i <= 2; i++ )
            NBTHelper.writeEnum(nbt, "Aspect" + i, cachedAspects.get(i == 1));
    }

    @Override
    public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.write(buffer, dimensions);
        for ( int i = 1; i <= 2; i++ )
            buffer.writeVarInt(cachedAspects.get(i == 1)
                    .ordinal());
    }

    // ---------- 便捷访问 ----------

    /** 某一侧的紧邻前方闭塞分区组 id。 */
    @Nullable
    public java.util.UUID getOwnGroup(boolean side) {
        return groups.get(side);
    }

    /** 某一侧是否挂有信号灯方块。 */
    public boolean hasLamps(boolean side) {
        return ! blockEntities.get(side).isEmpty();
    }

    /** 两侧的方块坐标快照，供渲染/调试使用。 */
    public Couple<Map<BlockPos, Boolean>> lampsSnapshot() {
        return blockEntities.map(HashMap :: new);
    }

}
