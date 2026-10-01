package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDPartialModels;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDPartialModels.LampModels;

/**
 * 四显示信号机的原版 BER 渲染（Flywheel 不可用时的回退路径）。
 *
 * <p>与 {@link FourSignalVisual} 共用 {@link CLDPartialModels#forAspect} 的
 * 档位→灯位映射，两条渲染路径的显示结果一致。</p>
 *
 * <p>灯光使用 {@link RenderTypes#additive()} 加色混合，与原版信号机与
 * 翻牌显示器灯管的观感一致。</p>
 */
public class FourSignalRenderer extends SafeBlockEntityRenderer<FourSignalBlockEntity> {

    /** 未使用的模型常量引用，保留以便将来新增灯位形态（如双面信号机）。 */
    @SuppressWarnings("unused")
    private static final PartialModel[] OVERLAY_MODELS = {
            AllPartialModels.TRACK_SIGNAL_OVERLAY, AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY
    };

    public FourSignalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(FourSignalBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 接管时不再走 BER，避免重复渲染
        if ( VisualizationManager.supportsVisualization(be.getLevel()) )
            return;

        FourAspectState aspect = be.getAspect();
        float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());

        LampModels models = CLDPartialModels.forAspect(aspect);
        // INVALID 时红灯闪烁：半数周期退回全熄灭模型
        if ( aspect == FourAspectState.INVALID && renderTime % 40 >= 20 )
            models = CLDPartialModels.forAspect(FourAspectState.RED).toDimmed();

        BlockState blockState = be.getBlockState();
        RenderType additive = RenderTypes.additive();

        renderLamp(models.green(), blockState, ms, buffer, additive);
        renderLamp(models.greenYellow(), blockState, ms, buffer, additive);
        renderLamp(models.yellow(), blockState, ms, buffer, additive);
        renderLamp(models.red(), blockState, ms, buffer, additive);

        renderOverlay(be, ms, buffer, light, overlay);
    }

    /** 渲染单个灯位；灯位高度已写进模型，这里只需平移到方块位置。 */
    private void renderLamp(PartialModel model, BlockState blockState, PoseStack ms, MultiBufferSource buffer,
                            RenderType type) {
        CachedBuffers.partial(model, blockState)
                .light(LightTexture.FULL_BLOCK)
                .disableDiffuse()
                .renderInto(ms, buffer.getBuffer(type));
    }

    private void renderOverlay(FourSignalBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light,
                               int overlay) {
        OverlayState overlayState = be.getOverlay();
        if ( overlayState == OverlayState.SKIP )
            return;

        TrackTargetingBehaviour<FourSignalBoundary> target = be.edgePoint;
        if ( target == null )
            return;

        BlockPos pos = be.getBlockPos();
        BlockPos targetPosition = target.getGlobalPosition();
        Level level = be.getLevel();
        if ( level == null )
            return;

        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();
        if ( ! (block instanceof ITrackBlock) )
            return;

        ms.pushPose();
        TransformStack.of(ms)
                .translate(targetPosition.subtract(pos));
        RenderedTrackOverlayType type =
                overlayState == OverlayState.DUAL ? RenderedTrackOverlayType.DUAL_SIGNAL : RenderedTrackOverlayType.SIGNAL;
        TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(),
                ms, buffer, light, overlay, type, 1);
        ms.popPose();
    }

}
