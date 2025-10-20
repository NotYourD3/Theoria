package pers.notyourd3.theoria.theory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerTheoryData extends SavedData {
    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC =
            ResourceLocation.CODEC.listOf().flatXmap(
                    list -> DataResult.success(Sets.newHashSet(list)), // Decode: List -> Set
                    set -> DataResult.success(List.copyOf(set))        // Encode: Set -> List
            );
    public static final Codec<Map<UUID, Set<ResourceLocation>>> THEORY_MAP_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.flatXmap(
                            str -> {
                                try { return DataResult.success(UUID.fromString(str)); }
                                catch (IllegalArgumentException e) { return DataResult.error(() -> "Invalid UUID: " + str); }
                            },
                            uuid -> DataResult.success(uuid.toString())
                    ),
                    RESOURCE_SET_CODEC
            );
    public static final Codec<PlayerTheoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            THEORY_MAP_CODEC.fieldOf("unlocked_theories").forGetter(data -> data.theories)
    ).apply(instance, PlayerTheoryData::new));
    private final Map<UUID, Set<ResourceLocation>> theories;
    public static final SavedDataType<PlayerTheoryData> ID = new SavedDataType<>(
            "theories",
            PlayerTheoryData::new,
            CODEC
    );

    public PlayerTheoryData() {
        // 构造函数 1: 首次创建时使用可变的 HashMap
        this(Maps.newHashMap());
    }

    public PlayerTheoryData(Map<UUID, Set<ResourceLocation>> theories) {
        // 修正后的构造函数 2: 从 Codec 加载时，强制创建 Map 的可变副本。
        // 这解决了加载数据为 ImmutableMap 导致的问题。
        this.theories = Maps.newHashMap(theories);
    }

    public Map<UUID, Set<ResourceLocation>> getTheories() {
        return theories; // 返回这个可变的 Map 实例
    }
}
