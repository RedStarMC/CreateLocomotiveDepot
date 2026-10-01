package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

import java.util.List;

/**
 * 四显示信号机的方块实体。
 *
 * <p>负责把 {@link FourSignalBoundary} 算出的四显示档位同步到客户端，
 * 并提供轨道覆盖层状态。所有真正的闭塞逻辑都在边界对象里。</p>
 */
public class FourSignalBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<FourSignalBoundary> edgePoint;

    private SignalBlockEntity.OverlayState overlay;
    private FourAspectState aspect;
    private boolean lastReportedPower;

    /** 刚变绿/黄/绿黄后的抑制计数，避免列车压过时灯色抖动（与原版同机制）。 */
    private int switchToRedAfterTrainEntered;

    public FourSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.overlay = SignalBlockEntity.OverlayState.SKIP;
        this.aspect = FourAspectState.INVALID;
        this.lastReportedPower = false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        edgePoint = new TrackTargetingBehaviour<>(this, CreateLocomotiveDepot.FOUR_SIGNAL);
        behaviours.add(edgePoint);
    }

    @Override
    public void tick() {
        super.tick();
        if ( level == null || level.isClientSide )
            return;

        FourSignalBoundary boundary = getSignal();
        if ( boundary == null ) {
            enterAspect(FourAspectState.INVALID);
            setOverlay(SignalBlockEntity.OverlayState.RENDER);
            return;
        }

        // 红石供电变化 -> 通知边界重算（红石强制红）
        getBlockState().getOptionalValue(FourSignalBlock.POWERED)
                .ifPresent(powered -> {
                    if ( lastReportedPower == powered )
                        return;
                    lastReportedPower = powered;
                    boundary.updateBlockEntityPower(this);
                    notifyUpdate();
                });

        enterAspect(boundary.getAspectFor(worldPosition));
        setOverlay(boundary.getOverlayFor(worldPosition));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        NBTHelper.writeEnum(tag, "Aspect", aspect);
        NBTHelper.writeEnum(tag, "Overlay", overlay);
        tag.putBoolean("Power", lastReportedPower);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        aspect = NBTHelper.readEnum(tag, "Aspect", FourAspectState.class);
        overlay = NBTHelper.readEnum(tag, "Overlay", SignalBlockEntity.OverlayState.class);
        lastReportedPower = tag.getBoolean("Power");
        invalidateRenderBoundingBox();
    }

    public boolean getReportedPower() {
        return lastReportedPower;
    }

    @Nullable
    public FourSignalBoundary getSignal() {
        return edgePoint == null ? null : edgePoint.getEdgePoint();
    }

    public FourAspectState getAspect() {
        return aspect;
    }

    public SignalBlockEntity.OverlayState getOverlay() {
        return overlay;
    }

    public void setOverlay(SignalBlockEntity.OverlayState state) {
        if ( this.overlay == state )
            return;
        this.overlay = state;
        notifyUpdate();
    }

    /**
     * 切换四显示档位。
     *
     * <p>抑制规则与原版 {@code SignalBlockEntity.enterState} 相同：进入通行档位
     * （绿 / 绿黄 / 黄）后 15 tick 内不接受转红，避免列车正好压过信号机时的闪烁。</p>
     */
    public void enterAspect(FourAspectState aspect) {
        if ( switchToRedAfterTrainEntered > 0 )
            switchToRedAfterTrainEntered--;

        if ( this.aspect == aspect )
            return;
        if ( aspect == FourAspectState.RED && switchToRedAfterTrainEntered > 0 )
            return;

        this.aspect = aspect;
        switchToRedAfterTrainEntered =
                aspect == FourAspectState.GREEN || aspect == FourAspectState.YELLOW || aspect == FourAspectState.GREEN_YELLOW
                        ? 15 : 0;
        notifyUpdate();
    }

}
