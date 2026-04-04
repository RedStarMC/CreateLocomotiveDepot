package top.redstarmc.mod.createlocomotivedepot;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four.FourSignalBoundary;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDBlockEntities;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDBlocks;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDCreativeModeTabs;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDItems;

@Mod(CreateLocomotiveDepot.MOD_ID)
public class CreateLocomotiveDepot {

    public static final String MOD_ID = "createlocomotivedepot";

    public static final String MOD_NAME = "Create: Locomotive Depot";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public static EdgePointType<FourSignalBoundary> FOUR_SIGNAL;

    static {
        REGISTRATE.setTooltipModifierFactory(item ->
                new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                        .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
        );
    }

    //  类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别 IEventBus 或 ModContaine r等参数类型，并自动传递它们。
    public CreateLocomotiveDepot(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} Loading...", MOD_NAME);

        FOUR_SIGNAL = EdgePointType.register(asResource("four_signal"), FourSignalBoundary :: new);
        CreateLocomotiveDepot.LOGGER.info("Registered FOUR_SIGNAL with id: {}", FOUR_SIGNAL.getId());

        modEventBus.addListener(this::commonSetup);
        REGISTRATE.registerEventListeners(modEventBus);

        CLDBlocks.register();
        CLDItems.register();
        CLDBlockEntities.register();
        CLDCreativeModeTabs.register();

        // 注册事件监听器
        // 请注意，只有当我们希望 *this* 类（CreateLocomotiveDepot）直接响应事件时，这才是必要的。
        // 如果此类中没有@SubscribeEvent注释函数，请不要添加此行，如下面的onServerStarting（）。
//        NeoForge.EVENT_BUS.register(this);

        // 注册我们的 mod 的 ModConfigSpec，以便 FML 可以为我们创建和加载配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }


    public static CreateRegistrate registrate() {
        return REGISTRATE;
        //
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //
    }
}
