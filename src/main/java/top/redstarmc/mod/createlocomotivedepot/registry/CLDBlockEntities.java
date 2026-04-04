package top.redstarmc.mod.createlocomotivedepot.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;
import top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four.FourSignalBlockEntity;
import top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four.FourSignalVisual;

public class CLDBlockEntities {

    private static final CreateRegistrate REGISTRATE = CreateLocomotiveDepot.registrate();

//    public static final BlockEntityEntry<ThreeAspectPassingSignalBlockEntity> THREE_ASPECT_PASSING_SIGNAL = REGISTRATE
//            .blockEntity("three_aspect_passing_signal" , ThreeAspectPassingSignalBlockEntity::new)
//            .validBlocks(CLDBlocks.THREE_ASPECT_PASSING_SIGNAL)
//            .renderer(() -> SignalRenderer ::new)
//            .register();

    public static final BlockEntityEntry<FourSignalBlockEntity> FOUR_SIGNAL = REGISTRATE
            .blockEntity("four_signal", FourSignalBlockEntity :: new)
            .visual(() -> FourSignalVisual :: new)
            .validBlocks(CLDBlocks.FOUR_SIGNAL)
            .register();

    public static void register() {
        CreateLocomotiveDepot.LOGGER.info("Registering CLDBlockEntities...");
        //
    }

}
