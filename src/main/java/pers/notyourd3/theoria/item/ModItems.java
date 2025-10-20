package pers.notyourd3.theoria.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import pers.notyourd3.theoria.Theoria;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Theoria.MODID);

    public static final DeferredItem<Item> KNOWLEDGE_SCROLL = ITEMS.registerItem("knowledge_scroll", properties -> new KnowledgeScrollItem(properties.stacksTo(1)));
    public static void register(IEventBus eventbus) {
        ITEMS.register(eventbus);
    }
}
