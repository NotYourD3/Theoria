package pers.notyourd3.theoria.theory.provider; 

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import pers.notyourd3.theoria.Theoria;
import pers.notyourd3.theoria.theory.Theory;
import pers.notyourd3.theoria.theory.TheoryCategory; 
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TheoryProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;
    private final String modId;


    private final Map<ResourceLocation, Theory> theories = new HashMap<>();
    private final Map<ResourceLocation, TheoryCategory> categories = new HashMap<>();

    public TheoryProvider(PackOutput output, String modId) {
        this.output = output;
        this.modId = modId;
    }
    public TheoryProvider(PackOutput output) {
        this.output = output;
        this.modId = Theoria.MODID;
    }


    // --- 核心生成方法 ---

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        addEntries();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        categories.forEach((id, category) -> {
            Path path = this.output.getOutputFolder()
                    .resolve("data/" + id.getNamespace() + "/theory/" + id.getPath() + ".json");

            JsonElement json = TheoryCategory.CODEC.encodeStart(JsonOps.INSTANCE, category)
                    .getOrThrow();

            futures.add(DataProvider.saveStable(cachedOutput, json, path));
        });

        theories.forEach((id, theory) -> {
            Path path = this.output.getOutputFolder()
                    .resolve("data/" + id.getNamespace() + "/theory/" + id.getPath() + ".json");

            JsonElement json = Theory.CODEC.encodeStart(JsonOps.INSTANCE, theory)
                    .getOrThrow(); // 假设你在 Theory 中添加了 CODEC

            futures.add(DataProvider.saveStable(cachedOutput, json, path));
        });

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }


    protected void addEntries() {
        ResourceLocation THEORIA = ResourceLocation.fromNamespaceAndPath(modId, "theoria");
        ResourceLocation STARTER_CAT_ID = ResourceLocation.fromNamespaceAndPath(modId, "starter_root");

        // 示例：添加一个 Theory Category
        TheoryCategory starterCategory = new TheoryCategory(
                STARTER_CAT_ID,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/advancements/backgrounds/stone.png"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "grass_block"), // 替换成实际的图标
                Component.translatable("theoria.theory_category.starter_root") // 使用 translatable
        );
        categories.put(starterCategory.id(), starterCategory);

        // --- 合成线: 工作台 -> 熔炉 -> 铁锭 ---

        // 1. 基础合成 (工作台)
        Theory basicTheory = new Theory(
                ResourceLocation.fromNamespaceAndPath(modId, "basic_crafting"),
                STARTER_CAT_ID,
                Component.translatable("theoria.theory.basic_crafting.title"), // 使用 translatable
                Component.translatable("theoria.theory.basic_crafting.subtitle"), // 使用 translatable
                Component.translatable("theoria.theory.basic_crafting.description"), // 使用 translatable
                ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"),
                100, 40,
                List.of(), // 无依赖
                "ACEF85", "task"
        );
        theories.put(basicTheory.id(), basicTheory);

        // 2. 冶炼 (熔炉) - 依赖 基础合成
        Theory smeltingTheory = new Theory(
                ResourceLocation.fromNamespaceAndPath(modId, "smelting"),
                STARTER_CAT_ID,
                Component.translatable("theoria.theory.smelting.title"), // 使用 translatable
                Component.translatable("theoria.theory.smelting.subtitle"), // 使用 translatable
                Component.translatable("theoria.theory.smelting.description"), // 使用 translatable
                ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
                150, 40,
                List.of(basicTheory.id()), // 依赖于基础合成
                "A5E5E5", "goal"
        );
        theories.put(smeltingTheory.id(), smeltingTheory);

        // 3. 炼铁 (铁锭) - 依赖 冶炼
        Theory ironTheory = new Theory(
                ResourceLocation.fromNamespaceAndPath(modId, "iron_smelting"),
                STARTER_CAT_ID,
                Component.translatable("theoria.theory.iron_smelting.title"), // 使用 translatable
                Component.translatable("theoria.theory.iron_smelting.subtitle"), // 使用 translatable
                Component.translatable("theoria.theory.iron_smelting.description"), // 使用 translatable
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot"),
                200, 40,
                List.of(smeltingTheory.id()), // 依赖于冶炼
                "F5C93F", "challenge"
        );
        theories.put(ironTheory.id(), ironTheory);


        // --- 畜牧线: 耕地 -> 繁殖 ---

        // 4. 耕地 (锄头) - 依赖 基础合成
        Theory farmingTheory = new Theory(
                ResourceLocation.fromNamespaceAndPath(modId, "basic_farming"),
                STARTER_CAT_ID,
                Component.translatable("theoria.theory.basic_farming.title"), // 使用 translatable
                Component.translatable("theoria.theory.basic_farming.subtitle"), // 使用 translatable
                Component.translatable("theoria.theory.basic_farming.description"), // 使用 translatable
                ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_hoe"),
                100, 100,
                List.of(basicTheory.id()), // 依赖于基础合成
                "597BA6", "task"
        );
        theories.put(farmingTheory.id(), farmingTheory);

        // 5. 畜牧 (小麦) - 依赖 耕地
        Theory breedingTheory = new Theory(
                ResourceLocation.fromNamespaceAndPath(modId, "animal_breeding"),
                STARTER_CAT_ID,
                Component.translatable("theoria.theory.animal_breeding.title"), // 使用 translatable
                Component.translatable("theoria.theory.animal_breeding.subtitle"), // 使用 translatable
                Component.translatable("theoria.theory.animal_breeding.description"), // 使用 translatable
                ResourceLocation.fromNamespaceAndPath("minecraft", "wheat"),
                150, 100,
                List.of(farmingTheory.id()), // 依赖于耕地
                "89B958", "goal"
        );
        theories.put(breedingTheory.id(), breedingTheory);
    }

    @Override
    public String getName() {
        return "Theoria Theories and Categories";
    }
}