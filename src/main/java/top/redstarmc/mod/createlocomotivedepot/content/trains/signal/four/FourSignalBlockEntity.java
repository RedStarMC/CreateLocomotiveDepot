package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.signal.SignalBlock;
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

public class FourSignalBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<FourSignalBoundary> edgePoint;

    private SignalBlockEntity.OverlayState overlay;
    private FourAspectState state;
    private boolean lastReportedPower;
    private int switchToRedAfterTrainEntered;

    public FourSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.overlay = SignalBlockEntity.OverlayState.SKIP;
        this.lastReportedPower = false;
        this.state = FourAspectState.INVALID;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        edgePoint = new TrackTargetingBehaviour<>(this, CreateLocomotiveDepot.FOUR_SIGNAL);
        behaviours.add(edgePoint);
    }

    @Override
    public void tick() {
        super.tick();
        if ( level != null && level.isClientSide ) return;

        FourSignalBoundary boundary = getSignal();
        if ( boundary == null ) {
            enterState(FourAspectState.INVALID);
            setOverlay(SignalBlockEntity.OverlayState.RENDER);
            return;
        }


//        // 根据当前方块位置确定属于哪一侧
//        boolean primary = boundary.isPrimaryForPos(worldPosition);
//        if ( ! primary && ! boundary.blockEntities.getSecond().containsKey(worldPosition) ) {
//            // 尚未注册，跳过 TODO
//            return;
//        }

        getBlockState().getOptionalValue(SignalBlock.POWERED).ifPresent(powered -> {
            if ( lastReportedPower == powered )
                return;
            lastReportedPower = powered;
            boundary.updateBlockEntityPower(this);
            notifyUpdate();
        });

        enterState(boundary.getStateFor(worldPosition));
        setOverlay(boundary.getOverlayFor(worldPosition));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        NBTHelper.writeEnum(tag, "State", state);
        NBTHelper.writeEnum(tag, "Overlay", overlay);
        tag.putBoolean("Power", lastReportedPower);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        state = NBTHelper.readEnum(tag, "State", FourAspectState.class);
        overlay = NBTHelper.readEnum(tag, "Overlay", SignalBlockEntity.OverlayState.class);
        lastReportedPower = tag.getBoolean("Power");
        invalidateRenderBoundingBox();
    }

    public boolean getReportedPower() {
        return lastReportedPower;
        //
    }

    public void enterState(FourAspectState state) {
        if ( switchToRedAfterTrainEntered > 0 )
            switchToRedAfterTrainEntered--;
        if ( this.state == state )
            return;
        if ( state == FourAspectState.RED && switchToRedAfterTrainEntered > 0 )
            return;
        this.state = state;
        switchToRedAfterTrainEntered = state == FourAspectState.GREEN || state == FourAspectState.YELLOW || state == FourAspectState.GREEN_YELLOW ? 15 : 0;
        notifyUpdate();
    }

    @Nullable
    public FourSignalBoundary getSignal() {
        return edgePoint.getEdgePoint();
    }

    public FourAspectState getState() {
        return state;
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

}