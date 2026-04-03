package top.redstarmc.mod.createlocomotivedepot.registry;

import com.simibubi.create.AllItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

public class CLDCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateLocomotiveDepot.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateLocomotiveDepot.MOD_ID + ".main"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS) // 放在原版蛋Tab后面
                    .icon(() -> new ItemStack(AllItems.WRENCH.asItem()))
                    .displayItems((parameters, output) -> {
                        // 手动添加
                    })
                    .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

}
