package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDPartialModels;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDPartialModels.LampModels;

import java.util.function.Consumer;

/**
 * 四显示信号机的 Flywheel 可视化。
 *
 * <p>四个灯位各自持有一个 {@link TransformedInstance}，档位变化时只替换模型
 * （{@code stealInstance}），不重建实例。渲染逻辑与
 * {@link FourSignalRenderer}（BER 回退路径）共用
 * {@link CLDPartialModels#forAspect(FourAspectState)} 的映射，
 * 保证两条路径显示一致。</p>
 */
public class FourSignalVisual extends AbstractBlockEntityVisual<FourSignalBlockEntity> implements SimpleTickableVisual {

    private final TransformedInstance green;
    private final TransformedInstance greenYellow;
    private final TransformedInstance yellow;
    private final TransformedInstance red;
    private final TransformedInstance signalOverlay;

    /** 上一次的灯位模型组合，用于判断是否需要 stealInstance。 */
    private LampModels previousModels;
    private OverlayState previousOverlayState;

    public FourSignalVisual(VisualizationContext ctx, FourSignalBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        LampModels initial = CLDPartialModels.forAspect(FourAspectState.INVALID);

        green = createLamp(ctx, initial.green());
        greenYellow = createLamp(ctx, initial.greenYellow());
        yellow = createLamp(ctx, initial.yellow());
        red = createLamp(ctx, initial.red());

        signalOverlay = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TRACK_SIGNAL_OVERLAY))
                .createInstance();

        setupVisual();
    }

    private static TransformedInstance createLamp(VisualizationContext ctx, PartialModel model) {
        return ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .createInstance();
    }

    @Override
    public void tick(Context context) {
        setupVisual();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(green, greenYellow, yellow, red, signalOverlay);
    }

    @Override
    protected void _delete() {
        green.delete();
        greenYellow.delete();
        yellow.delete();
        red.delete();
        signalOverlay.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(green);
        consumer.accept(greenYellow);
        consumer.accept(yellow);
        consumer.accept(red);
    }

    private void setupVisual() {
        setupLamps();
        setupOverlay();
    }

    private void setupLamps() {
        FourAspectState aspect = blockEntity.getAspect();
        float renderTime = AnimationTickHolder.getRenderTime(blockEntity.getLevel());

        // INVALID 时红灯闪烁：在半数周期内退回"熄灭"模型
        LampModels models = CLDPartialModels.forAspect(aspect);
        if ( aspect == FourAspectState.INVALID && renderTime % 40 >= 20 )
            models = CLDPartialModels.forAspect(FourAspectState.RED).toDimmed();

        if ( models != previousModels ) {
            previousModels = models;
            stealModel(green, models.green());
            stealModel(greenYellow, models.greenYellow());
            stealModel(yellow, models.yellow());
            stealModel(red, models.red());
        }

        // 灯位高度已烘焙进各自模型，这里只需摆到方块位置
        place(green);
        place(greenYellow);
        place(yellow);
        place(red);

        // 点亮的灯位使用全亮光照，远处也可辨识（与原版信号机一致）
        applyLit(green, models.green().equals(CLDPartialModels.LAMP_GREEN_LIT));
        applyLit(greenYellow, models.greenYellow().equals(CLDPartialModels.LAMP_GREEN_YELLOW_LIT));
        applyLit(yellow, models.yellow().equals(CLDPartialModels.LAMP_YELLOW_LIT));
        applyLit(red, models.red().equals(CLDPartialModels.LAMP_RED_LIT));
    }

    private void place(TransformedInstance instance) {
        instance.setIdentityTransform()
                .translate(getVisualPosition())
                .setChanged();
    }

    private void applyLit(TransformedInstance instance, boolean lit) {
        if ( lit )
            instance.light(LightTexture.FULL_BLOCK);
    }

    /**
     * 用另一个 {@link PartialModel} 替换实例的模型。
     *
     * <p>Flywheel 的做法是向目标模型的 instancer 申请 {@code stealInstance}，
     * 把已有实例"偷"过去，从而避免删除再新建（参考 Create 的
     * {@code SignalVisual.setupVisual} 与 {@code NixieTubeRenderer}）。</p>
     */
    private void stealModel(TransformedInstance instance, PartialModel model) {
        instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .stealInstance(instance);
    }

    private void setupOverlay() {
        TrackTargetingBehaviour<FourSignalBoundary> target = blockEntity.edgePoint;
        if ( target == null ) {
            signalOverlay.setZeroTransform()
                    .setChanged();
            previousOverlayState = null;
            return;
        }

        BlockPos targetPosition = target.getGlobalPosition();
        Level level = blockEntity.getLevel();
        if ( level == null ) {
            signalOverlay.setZeroTransform()
                    .setChanged();
            previousOverlayState = null;
            return;
        }

        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();
        OverlayState overlayState = blockEntity.getOverlay();

        if ( ! (block instanceof ITrackBlock trackBlock) || overlayState == OverlayState.SKIP ) {
            signalOverlay.setZeroTransform()
                    .setChanged();
            previousOverlayState = null;
            return;
        }

        if ( overlayState != previousOverlayState ) {
            previousOverlayState = overlayState;

            PartialModel partial;
            RenderedTrackOverlayType type;
            if ( overlayState == OverlayState.DUAL ) {
                type = RenderedTrackOverlayType.DUAL_SIGNAL;
                partial = AllPartialModels.TRACK_SIGNAL_DUAL_OVERLAY;
            } else {
                type = RenderedTrackOverlayType.SIGNAL;
                partial = AllPartialModels.TRACK_SIGNAL_OVERLAY;
            }

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
