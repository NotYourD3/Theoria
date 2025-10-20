package pers.notyourd3.theoria.theory;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public record TheoryCategory(ResourceLocation id,ResourceLocation background, ResourceLocation icon, Component title) {
    public static final Codec<TheoryCategory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("background").forGetter(TheoryCategory::background),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(TheoryCategory::icon),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(TheoryCategory::title)
    ).apply(instance, (background, icon, title) -> new TheoryCategory(null, background, icon, title)));

    public static TheoryCategory fromJson(ResourceLocation res, JsonObject json) {
        ResourceLocation background = ResourceLocation.parse(json.get("background").getAsString());
        ResourceLocation icon = ResourceLocation.parse(json.get("icon").getAsString());
        Component title = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json.get("title")).getOrThrow();
        return new TheoryCategory(res, background, icon, title);
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, TheoryCategory> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TheoryCategory::id,
            ResourceLocation.STREAM_CODEC, TheoryCategory::background,
            ResourceLocation.STREAM_CODEC, TheoryCategory::icon,
            ComponentSerialization.STREAM_CODEC, TheoryCategory::title,
            TheoryCategory::new
    );
}
