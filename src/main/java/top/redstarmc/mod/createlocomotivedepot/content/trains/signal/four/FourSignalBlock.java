package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDBlockEntities;

public class FourSignalBlock extends Block implements IBE<FourSignalBlockEntity>, ProperWaterloggedBlock {

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
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED, WATERLOGGED));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if ( level.isClientSide ) return;
        boolean powered = state.getValue(POWERED);
        boolean hasSignal = level.hasNeighborSignal(pos);
        if ( powered == hasSignal ) return;
        level.setBlock(pos, state.setValue(POWERED, hasSignal), UPDATE_CLIENTS);
    }

    @Override
    public Class<FourSignalBlockEntity> getBlockEntityClass() {
        return FourSignalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FourSignalBlockEntity> getBlockEntityType() {
        return CLDBlockEntities.FOUR_SIGNAL.get();
    }

}