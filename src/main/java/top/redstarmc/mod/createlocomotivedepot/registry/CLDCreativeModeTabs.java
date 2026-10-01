package top.redstarmc.mod.createlocomotivedepot.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

/**
 * 本模组的创造模式物品栏。
 *
 * <p>用原生 {@link DeferredRegister} 注册（与 Create 自身
 * {@code AllCreativeModeTabs} 的做法一致），而不使用 Registrate 的
 * {@code defaultCreativeTab}——后者会自动为标签页生成一条英文语言键
 * （值为 {@code "Main"}），与我们想要的显示名冲突。</p>
 */
public class CLDCreativeModeTabs {

    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateLocomotiveDepot.MOD_ID);

    /** 本模组自己的标签页。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateLocomotiveDepot.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(CLDBlocks.FOUR_SIGNAL.get()
                            .asItem()))
                    .displayItems((parameters, output) -> output.accept(CLDBlocks.FOUR_SIGNAL.get()
                            .asItem()))
                    .build());

    public static void register(IEventBus modEventBus) {
        CreateLocomotiveDepot.LOGGER.info("Registering CLDCreativeModeTabs...");
        REGISTER.register(modEventBus);
    }

}
