package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FourSignalVisual extends AbstractBlockEntityVisual<FourSignalBlockEntity> implements SimpleTickableVisual {

    private final TransformedInstance signalOverlay;
    private SignalBlockEntity.OverlayState previousOverlayState;

    public FourSignalVisual(VisualizationContext ctx, FourSignalBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        signalOverlay = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(com.simibubi.create.AllPartialModels.TRACK_SIGNAL_OVERLAY))
                .createInstance();

        setupVisual();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(signalOverlay);
        //
    }

    @Override
    public void updateLight(float partialTick) {
        relight(signalOverlay);
        //
    }

    @Override
    protected void _delete() {
        signalOverlay.delete();
        //
    }

    @Override
    public void tick(Context context) {
        setupVisual();
        //
    }

    private void setupVisual() {
        com.simibubi.create.content.trains.track.TrackTargetingBehaviour<FourSignalBoundary> target = blockEntity.edgePoint;
        net.minecraft.core.BlockPos targetPosition = target.getGlobalPosition();
        Level level = blockEntity.getLevel();
        BlockState trackState = null;
        if ( level != null ) {
            trackState = level.getBlockState(targetPosition);
        }
        Block block = null;
        if ( trackState != null ) {
            block = trackState.getBlock();
        }

        if ( ! (block instanceof ITrackBlock trackBlock) ) {
            signalOverlay.setZeroTransform().setChanged();
            previousOverlayState = null;
            return;
        }

        SignalBlockEntity.OverlayState overlayState = blockEntity.getOverlay(); // 从方块实体获得应该渲染的位置
        if ( overlayState == SignalBlockEntity.OverlayState.SKIP ) {
            signalOverlay.setZeroTransform().setChanged();
            previousOverlayState = null;
            return;
        }

        if ( overlayState != previousOverlayState ) {
            previousOverlayState = overlayState;

            PartialModel partial;
            TrackTargetingBehaviour.RenderedTrackOverlayType type;
            if ( overlayState == SignalBlockEntity.OverlayState.DUAL ) {
                type = TrackTargetingBehaviour.RenderedTrackOverlayType.DUAL_SIGNAL;
                partial = com.simibubi.create.AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY;
            } else {
                type = TrackTargetingBehaviour.RenderedTrackOverlayType.SIGNAL;
                partial = com.simibubi.create.AllPartialModels.TRACK_SIGNAL_OVERLAY;
            }

            // 替换实例模型（注意：为了简单，我们可以每次都新建，但更好的是更换模型）
            // 简单做法：先删除旧实例再新建？不推荐。我们使用 instancerProvider 重新获取实例
            // 由于模型改变，需要 stealInstance。这里参考原版 SignalVisual
            instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(partial))
                    .stealInstance(signalOverlay);

            signalOverlay.setIdentityTransform()
                    .translate(targetPosition.subtract(renderOrigin()));

            trackBlock.prepareTrackOverlay(signalOverlay, level, targetPosition, trackState,
                    target.getTargetBezier(), target.getTargetDirection(), type);

            signalOverlay.setChanged();
        }
    }

}
