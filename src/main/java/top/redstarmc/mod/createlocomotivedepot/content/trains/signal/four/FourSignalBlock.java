package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDBlockEntities;

/**
 * 四显示信号机方块。
 *
 * <p>红石语义与原版信号机一致：邻近红石信号将本侧强制置红；
 * 比较器输出在红灯时给出 15。</p>
 */
public class FourSignalBlock extends Block implements IBE<FourSignalBlockEntity>, ProperWaterloggedBlock, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FourSignalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withWater(super.getStateForPlacement(context), context)
                .setValue(POWERED, context.getLevel()
                        .hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED, WATERLOGGED));
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos,
                                boolean moved) {
        if ( level.isClientSide )
            return;
        boolean powered = state.getValue(POWERED);
        boolean hasSignal = level.hasNeighborSignal(pos);
        if ( powered == hasSignal )
            return;
        level.setBlock(pos, state.setValue(POWERED, hasSignal), Block.UPDATE_CLIENTS);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<FourSignalBlockEntity> getBlockEntityClass() {
        return FourSignalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FourSignalBlockEntity> getBlockEntityType() {
        return CLDBlockEntities.FOUR_SIGNAL.get();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos)
                .filter(be -> be.getAspect() == FourAspectState.RED)
                .map(be -> 15)
                .orElse(0);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // 暂无需要切换的模式；保留 IWrenchable 以便与其他 Create 方块行为一致
        return InteractionResult.PASS;
    }

}
