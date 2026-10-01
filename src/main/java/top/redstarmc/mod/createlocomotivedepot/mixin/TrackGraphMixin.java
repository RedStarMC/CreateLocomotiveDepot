package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgePointStorage;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 让 Create 按 {@code EdgePointType.SIGNAL} 查找信号时，也能找到本模组的四显示信号边界。
 *
 * <p><b>为什么必须做</b>：Create 有 4 处关键路径用
 * {@code graph.getPoint(EdgePointType.SIGNAL, id)} 反查信号对象：</p>
 * <ul>
 *   <li>{@code Navigation#tick}（:110）与 {@code Navigation#currentSignalResolved}（:307）——
 *       列车停下等红灯后，下一 tick 用它确认"我等的那个信号还在不在"。
 *       返回 {@code null} 会被判定为"信号已消失"并立即放行，
 *       列车将<b>无视本模组的红灯直接通过</b>。</li>
 *   <li>{@code Train#updateNavigationTarget}（:568）—— 判断等待中的信号是否为链式信号。</li>
 *   <li>{@code RailwaySavedData#load}（:70，走 {@code getPoints}）—— 存档加载后重建
 *       {@code SignalEdgeGroup.adjacent} 邻接关系。</li>
 *   <li>{@code TrainMapSync}（:303）—— 列车地图显示"正在等待哪架信号机"。</li>
 * </ul>
 *
 * <p>补齐后上述路径无需修改即可识别本模组信号机，与原版信号机混用亦成立。
 * 原版信号始终优先，因此对纯原版存档行为零影响。</p>
 */
@Mixin(value = TrackGraph.class, remap = false)
public abstract class TrackGraphMixin {

    /**
     * 读内部存储而不调用 {@code getPoint}/{@code getPoints}，避免与下面的注入互相递归。
     * {@code edgePoints} 是包级私有字段，经 {@link TrackGraphAccessor} 访问。
     */
    private EdgePointStorage cld$storage() {
        return ((TrackGraphAccessor) (Object) this).cld$getEdgePointStorage();
    }

    @Inject(method = "getPoint", at = @At("HEAD"), cancellable = true)
    private void cld$getPoint(EdgePointType<?> type, UUID id, CallbackInfoReturnable<TrackEdgePoint> cir) {
        if ( type != EdgePointType.SIGNAL )
            return;

        // 原版信号优先，行为不变
        if ( cld$get(EdgePointType.SIGNAL, id) != null )
            return;

        TrackEdgePoint four = cld$get(CreateLocomotiveDepot.FOUR_SIGNAL, id);
        if ( four != null )
            cir.setReturnValue(four);
    }

    @Inject(method = "getPoints", at = @At("HEAD"), cancellable = true)
    private void cld$getPoints(EdgePointType<?> type, CallbackInfoReturnable<Collection<TrackEdgePoint>> cir) {
        if ( type != EdgePointType.SIGNAL )
            return;

        Collection<TrackEdgePoint> four = cld$values(CreateLocomotiveDepot.FOUR_SIGNAL);
        if ( four.isEmpty() )
            return; // 没有本模组信号机时保持原样，避免多余分配

        Collection<TrackEdgePoint> vanilla = cld$values(EdgePointType.SIGNAL);

        List<TrackEdgePoint> merged = new ArrayList<>(vanilla.size() + four.size());
        merged.addAll(vanilla);
        merged.addAll(four);
        cir.setReturnValue(merged);
    }

    /** {@code EdgePointStorage.get} 是泛型的，这里统一擦除为 {@code TrackEdgePoint}。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private TrackEdgePoint cld$get(EdgePointType<?> type, UUID id) {
        return cld$storage()
                .get((EdgePointType) type, id);
    }

    /** {@code EdgePointStorage.values} 是泛型的，这里统一擦除为 {@code TrackEdgePoint} 集合。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<TrackEdgePoint> cld$values(EdgePointType<?> type) {
        return (Collection<TrackEdgePoint>) (Collection) cld$storage()
                .values((EdgePointType) type);
    }

}
