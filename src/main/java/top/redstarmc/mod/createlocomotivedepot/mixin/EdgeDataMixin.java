package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

import java.util.List;

/**
 * 让 Create 的 {@link EdgeData} 认识本模组的四显示信号边界。
 *
 * <p>Create 6.0.9 的 {@code EdgeData} 有两处对 {@code EdgePointType.SIGNAL} 的硬编码，
 * 若不补齐，本模组的信号机对闭塞系统将完全不生效：</p>
 *
 * <ol>
 *   <li><b>{@code addPoint} / {@code removePoint}</b>：只在类型等于 {@code EdgePointType.SIGNAL} 时
 *       切换 {@code singleSignalGroup}（{@code null} = 有信号边界，{@code passiveGroup} = 无）。
 *       本模组点类型是 {@code FOUR_SIGNAL}，于是整条边会被当成"无信号边"参与原版蔓延，
 *       闭塞分区无从建立。</li>
 *   <li><b>{@code next(EdgePointType, double)} / {@code get(EdgePointType, double)}</b>：
 *       供 {@code SignalPropagator.walkSignals}（{}:205）、
 *       {@code EdgeData.getGroupAtPosition}（:109）、
 *       {@code Train.collectInitiallyOccupiedSignalBlocks}（:1023-1031）查找信号边界。
 *       不补齐则分组蔓延会"穿过"本模组信号机。</li>
 * </ol>
 *
 * <p>策略：凡是 Create 以 {@code EdgePointType.SIGNAL} 询问的地方，把继承自
 * {@link SignalBoundary} 的本模组边界也视为信号边界。原版信号优先，行为不变；
 * 两者因此可以混处在同一条闭塞链上。</p>
 */
@Mixin(value = EdgeData.class, remap = false)
public abstract class EdgeDataMixin {

    @Shadow
    private TrackEdge edge;

    @Shadow
    private List<TrackEdgePoint> points;

    /** 一个边点是否算作"信号边界"。本模组边界继承 SignalBoundary，因此一并命中。 */
    private static boolean cld$isSignalLike(TrackEdgePoint point) {
        return point instanceof SignalBoundary;
    }

    // ---------- 1) 维护 singleSignalGroup ----------

    @Inject(method = "addPoint", at = @At("HEAD"))
    private void cld$addPoint(TrackGraph graph, TrackEdgePoint point, CallbackInfo ci) {
        if ( point.getType() == EdgePointType.SIGNAL )
            return; // 原版逻辑已处理
        if ( ! cld$isSignalLike(point) )
            return;
        ((EdgeData) (Object) this).setSingleSignalGroup(graph, null);
    }

    @Inject(method = "removePoint", at = @At("TAIL"))
    private void cld$removePoint(TrackGraph graph, TrackEdgePoint point, CallbackInfo ci) {
        if ( point.getType() == EdgePointType.SIGNAL )
            return; // 原版逻辑已处理
        if ( ! cld$isSignalLike(point) )
            return;

        EdgeData self = (EdgeData) (Object) this;
        if ( ! self.hasSignalBoundaries() )
            return; // 只剩本模组信号机时，父类可能没把 singleSignalGroup 置回 null
        if ( points.stream()
                .anyMatch(EdgeDataMixin :: cld$isSignalLike) )
            return;

        self.setSingleSignalGroup(graph, EdgeData.passiveGroup);
    }

    // ---------- 2) 按 EdgePointType.SIGNAL 查找时纳入本模组边界 ----------

    /**
     * 精确描述符：{@code next(EdgePointType, double)} 与 {@code next(double)} 重载，
     * 必须用完整描述符消歧。
     */
    @Inject(
            method = "next(Lcom/simibubi/create/content/trains/graph/EdgePointType;D)Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cld$next(EdgePointType<?> type, double minPosition, CallbackInfoReturnable<TrackEdgePoint> cir) {
        if ( type != EdgePointType.SIGNAL )
            return;
        if ( cir.getReturnValue() != null )
            return; // 原版信号优先，语义不变

        TrackEdgePoint four = cld$find(EdgePointType.SIGNAL, minPosition, true);
        if ( four != null )
            cir.setReturnValue(four);
    }

    @Inject(
            method = "get(Lcom/simibubi/create/content/trains/graph/EdgePointType;D)Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void cld$get(EdgePointType<?> type, double exactPosition, CallbackInfoReturnable<TrackEdgePoint> cir) {
        if ( type != EdgePointType.SIGNAL )
            return;
        if ( cir.getReturnValue() != null )
            return;

        TrackEdgePoint four = cld$find(EdgePointType.SIGNAL, exactPosition - .5f, false);
        if ( four != null && Mth.equal(four.getLocationOn(edge), exactPosition) )
            cir.setReturnValue(four);
    }

    /**
     * 按类型与位置在 {@code points} 中查找。
     *
     * @param fourSignals 为 {@code true} 时查本模组类型，否则沿用传入类型
     */
    private TrackEdgePoint cld$find(EdgePointType<?> type, double minPosition, boolean fourSignals) {
        EdgePointType<?> lookup = fourSignals ? CreateLocomotiveDepot.FOUR_SIGNAL : type;
        for ( TrackEdgePoint point : points ) {
            if ( point.getType() == lookup && point.getLocationOn(edge) > minPosition )
                return point;
        }
        return null;
    }

}
