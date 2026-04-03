package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

import java.util.List;

public class FourSignalBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<FourSignalBoundary> edgePoint;
    private boolean lastReportedPower; // 用于红石强制模式

    public FourSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

        FourSignalBoundary boundary = edgePoint.getEdgePoint();
        if ( boundary == null ) return;

        // 根据当前方块位置确定属于哪一侧
        boolean primary = boundary.isPrimaryForPos(worldPosition);
        if ( ! primary && ! boundary.blockEntities.getSecond().containsKey(worldPosition) ) {
            // 尚未注册，跳过
            return;
        }

        boolean powered = getBlockState().getValue(SignalBlock.POWERED);
        if ( lastReportedPower != powered ) {
            lastReportedPower = powered;
            boundary.blockEntities.get(primary).put(worldPosition, powered);
            boundary.queueUpdateForSide(primary);
            notifyUpdate();
        }
    }

    public boolean getReportedPower() {
        return lastReportedPower;
    }

    public FourAspectState getDisplayState() {
        FourSignalBoundary boundary = edgePoint.getEdgePoint();
        if ( boundary == null ) return FourAspectState.INVALID;
        boolean primary = boundary.blockEntities.getFirst().containsKey(worldPosition);
        if ( primary ) return boundary.cachedStates.getFirst();
        else if ( boundary.blockEntities.getSecond().containsKey(worldPosition) )
            return boundary.cachedStates.getSecond();
        else return FourAspectState.INVALID;
    }

}