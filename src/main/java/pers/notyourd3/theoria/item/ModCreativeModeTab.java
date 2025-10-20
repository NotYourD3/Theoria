package pers.notyourd3.theoria.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import pers.notyourd3.theoria.Theoria;
import pers.notyourd3.theoria.theory.TheoryManager;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Theoria.MODID);

    public static final Supplier<CreativeModeTab> THEORIA_TAB = CREATIVE_MODE_TABS.register("trail_of_light_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.theoria"))
                    .icon(() -> ModItems.KNOWLEDGE_SCROLL.get().getDefaultInstance())
                    .displayItems(((itemDisplayParameters, output) -> {
                        output.accept(ModItems.KNOWLEDGE_SCROLL.get().getDefaultInstance());
                        List<ResourceLocation> allTheories = TheoryManager.getInstance().getAllTheoryIds();
                        for (ResourceLocation theoryId : allTheories) {
                            ItemStack scrollStack = ModItems.KNOWLEDGE_SCROLL.get().getDefaultInstance();
                            scrollStack.set(ModDataComponents.SAVED_THEORY.get(),new ModDataComponents.savedTheory(theoryId));
                            output.accept(scrollStack);
                        }
                    }))
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
