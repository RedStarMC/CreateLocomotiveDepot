package top.redstarmc.mod.createlocomotivedepot.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;
import top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four.FourSignalBlock;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;


public class CLDBlocks {

    private static final CreateRegistrate REGISTRATE = CreateLocomotiveDepot.registrate();

//    public static final BlockEntry<ThreeAspectPassingSignalBlock> THREE_ASPECT_PASSING_SIGNAL = REGISTRATE
//            .block("three_aspect_passing_signal_block", ThreeAspectPassingSignalBlock::new)
//            .properties(p -> p.mapColor(MapColor.PODZOL)
//                    .noOcclusion()
//                    .sound(SoundType.NETHERITE_BLOCK))
//            .blockstate((c, p) -> p.getVariantBuilder(c.get())
//                    .forAllStates(state -> ConfiguredModel.builder()
//                            .modelFile(p.models()
//                                    .cubeAll(c.getName() + "_" + state.getValue(SignalBlock.TYPE).getSerializedName(),
//                                            p.modLoc("block/three_aspect_passing_signal_box")))
//                            .build()))
//            .transform(pickaxeOnly())
//            .lang("Three Aspect Passing Signal Block")
//            .loot(RegistrateBlockLootTables :: dropSelf) // 可不写
//            .item(TrackTargetingBlockItem.ofType(EdgePointType.SIGNAL))
//            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/three_aspect_passing_signal_box")))
////            .tab()
//            .build()
    /// /            .recipe((ctx, provider) -> {
    /// /
    /// /            })
//            .register();

    public static final BlockEntry<FourSignalBlock> FOUR_SIGNAL = REGISTRATE
            .block("four_signal_block", FourSignalBlock :: new)
            .properties(p -> p.mapColor(MapColor.PODZOL)
                    .noOcclusion()
                    .sound(SoundType.NETHER_BRICKS))
            .transform(pickaxeOnly())
            .loot(RegistrateBlockLootTables :: dropSelf)
            .register();

    public static void register() {
    }


}
