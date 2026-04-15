package top.redstarmc.mod.createlocomotivedepot.registry;

import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
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

    public static final BlockEntry<FourSignalBlock> FOUR_SIGNAL = REGISTRATE
            .block("four_signal_block", FourSignalBlock :: new)
            .properties(p -> p.mapColor(MapColor.PODZOL)
                    .noOcclusion()
                    .sound(SoundType.NETHER_BRICKS))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().getExistingFile(prov.modLoc("block/four_signal_block"))
            ))
            .transform(pickaxeOnly())
            .loot(RegistrateBlockLootTables :: dropSelf)
            .lang("Four Signal Block")
            .item((block, properties) -> new TrackTargetingBlockItem(block, properties, CreateLocomotiveDepot.FOUR_SIGNAL))
//            .tab(CreativeModeTabs.BUILDING_BLOCKS)
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/four_signal_block")))
            .build()
            .register();

    public static void register() {
    }


}
