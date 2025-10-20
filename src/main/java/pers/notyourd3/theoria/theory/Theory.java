package pers.notyourd3.theoria.theory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public record Theory(ResourceLocation id, ResourceLocation categoryId, Component title, Component subTitle, Component description, ResourceLocation icon, int x, int y, List<ResourceLocation> dependencies,String color,String type) {
    public static final Codec<Theory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("category").forGetter(Theory::categoryId),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(Theory::title),
            ComponentSerialization.CODEC.fieldOf("subTitle").forGetter(Theory::subTitle),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Theory::description),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(Theory::icon),
            Codec.INT.fieldOf("x").forGetter(Theory::x),
            Codec.INT.fieldOf("y").forGetter(Theory::y),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dependencies", List.of()).forGetter(Theory::dependencies),
            Codec.STRING.fieldOf("color").forGetter(Theory::color)
            , Codec.STRING.fieldOf("type").forGetter(Theory::type)
    ).apply(instance, (categoryId, title, subTitle, description, icon, x, y, dependencies,color,type) -> new Theory(
            null,
            categoryId,
            title,
            subTitle,
            description,
            icon,
            x,
            y,
            dependencies,
            color,
            type
    )));

    public static Theory fromJson(ResourceLocation res,JsonObject json){
        ResourceLocation categoryId;
        try {
            categoryId = ResourceLocation.parse(GsonHelper.getAsString(json, "category"));
        } catch (Exception e) {
            throw new RuntimeException("Missing or invalid 'category' field in theory JSON: " + res, e);
        }
        Component title = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json.get("title")).getOrThrow();
        Component subTitle = json.has("subTitle") ? ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json.get("subTitle")).getOrThrow() : null;
        Component description = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json.get("description")).getOrThrow();
        ResourceLocation icon = ResourceLocation.parse(json.get("icon").getAsString());
        int x = GsonHelper.getAsInt(json, "x");
        int y = GsonHelper.getAsInt(json, "y");

        List<ResourceLocation> dependencies = new ArrayList<>();
        String color = json.has("color") ? json.get("color").getAsString() : "597BA6";
        String type = json.has("type") ? json.get("type").getAsString() : "task";
        if (json.has("dependencies")) {
            JsonArray dependenciesArray = GsonHelper.getAsJsonArray(json, "dependencies");
            for (JsonElement element : dependenciesArray) {
                if (element.isJsonPrimitive()) {
                    dependencies.add(ResourceLocation.parse(element.getAsString()));
                }
            }
        }
        return new Theory(res, categoryId, title, subTitle, description, icon, x, y, dependencies, color,type);
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, Theory> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Theory::id,
            ResourceLocation.STREAM_CODEC, Theory::categoryId,
            ComponentSerialization.STREAM_CODEC, Theory::title,
            ComponentSerialization.STREAM_CODEC, Theory::subTitle,
            ComponentSerialization.STREAM_CODEC, Theory::description,
            ResourceLocation.STREAM_CODEC, Theory::icon,
            ByteBufCodecs.INT, Theory::x,
            ByteBufCodecs.INT, Theory::y,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Theory::dependencies,
            ByteBufCodecs.STRING_UTF8, Theory::color,
            ByteBufCodecs.STRING_UTF8, Theory::type,
            Theory::new
    );

}