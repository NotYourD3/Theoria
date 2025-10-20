package pers.notyourd3.theoria.item;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import pers.notyourd3.theoria.Theoria;

import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Theoria.MODID);
    public static final Supplier<DataComponentType<savedTheory>> SAVED_THEORY = DATA_COMPONENTS.registerComponentType(
            "saved_theory",
            builder ->builder
                    .persistent(RecordCodecBuilder.create(instance -> instance.group(ResourceLocation.CODEC.fieldOf("saved_theory").forGetter(savedTheory::location)).apply(instance, savedTheory::new)))
                    .networkSynchronized(StreamCodec.composite(ResourceLocation.STREAM_CODEC,savedTheory::location,savedTheory::new))
    );
    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
    public record savedTheory(ResourceLocation location){}
}
