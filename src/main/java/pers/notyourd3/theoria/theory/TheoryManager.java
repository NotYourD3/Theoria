package pers.notyourd3.theoria.theory;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import pers.notyourd3.theoria.event.TheoryUnlockedEvent;
import pers.notyourd3.theoria.network.SyncPlayerTheoriesPayload;
import pers.notyourd3.theoria.network.SyncTheoriesPayload;

import java.util.*;
import java.util.stream.Collectors;

public class TheoryManager {
    private static final TheoryManager INSTANCE = new TheoryManager();
    boolean processed = false;
    private PlayerTheoryData savedData;

    private Map<ResourceLocation, Set<Theory>> theories;//to sync
    private Map<ResourceLocation, TheoryCategory> categories;//to sync
    private Map<Theory, List<Theory>> theoryDependencies;
    private Map<Theory,List<ResourceLocation>> dependenciesCache;
    private Map<ResourceLocation, Theory> theoryCache;
    private Map<UUID,Set<ResourceLocation>> playerTheories;

    private TheoryManager() {
        this.theories = Maps.newHashMap();
        this.theoryCache = Maps.newHashMap();
        this.categories = Maps.newHashMap();
        this.theoryDependencies = Maps.newHashMap();
        this.dependenciesCache = Maps.newHashMap();
        this.playerTheories = Maps.newHashMap();
    }
    public void loadTheoryData(MinecraftServer server) {
        this.savedData = server.overworld().getDataStorage().computeIfAbsent(PlayerTheoryData.ID);
        this.playerTheories = this.savedData.getTheories();
    }

    public static TheoryManager getInstance() {
        return INSTANCE;
    }
    public void addTheory(Theory theory){
        theoryCache.put(theory.id(), theory); // 使用理论的 ID 作为键
        dependenciesCache.put(theory, theory.dependencies());
    }
    public void addCategory(TheoryCategory category){
        categories.put(category.id(), category); // 使用ID作为键
    }
    public boolean applyTheoryToPlayer(Player player, ResourceLocation theoryId) {
        Theory theory = theoryCache.get(theoryId);
        if (theory != null && theory.dependencies() != null && !theory.dependencies().isEmpty()) {
            Set<ResourceLocation> playerUnlockedTheories = playerTheories.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
            for (ResourceLocation dependency : theory.dependencies()) {
                if (!playerUnlockedTheories.contains(dependency)) {
                    return false;
                }
            }
        }
        forceApplyTheoryToPlayer(player, theoryId);
        return true;
    }
    public void forceApplyTheoryToPlayer(Player player, ResourceLocation theoryId) {
        // 1. 尝试添加理论并检查是否有实际变更
        Set<ResourceLocation> playerUnlockedTheories = playerTheories.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        boolean changed = playerUnlockedTheories.add(theoryId);
        if (changed) {
            if (this.savedData != null) {
                this.savedData.setDirty(); // 标记数据已修改，通知 Minecraft 保存
            }
            NeoForge.EVENT_BUS.post(new TheoryUnlockedEvent(player, theoryId));
        }

        // 3. 同步客户端
        syncPlayerTheories(player);
    }
    public boolean revokeTheoryFromPlayer(Player player, ResourceLocation theoryId) {
        Set<ResourceLocation> playerUnlockedTheories = playerTheories.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        boolean changed = playerUnlockedTheories.remove(theoryId);
        if (changed) {
            if (this.savedData != null) {
                this.savedData.setDirty(); // 标记数据已修改，通知 Minecraft 保存
            }
        }
        syncPlayerTheories(player);
        return changed;
    }

    public Either<Theory,TheoryCategory> getTheory(ResourceLocation location){
        TheoryCategory category = categories.get(location);
        if (category != null) {
            return Either.right(category);
        }
        if (processed) {
            Theory theory = theories.values().stream()
                    .flatMap(Set::stream)
                    .filter(t -> t.id().equals(location))
                    .findFirst()
                    .orElse(null);
            if (theory != null) {
                return Either.left(theory);
            }
        } else {
            Theory theory = theoryCache.values().stream()
                    .filter(t -> t.id().equals(location))
                    .findFirst()
                    .orElse(null);
            if (theory != null) {
                return Either.left(theory);
            }
        }

        return null;
    }
    public void postProcessing(){
        if (processed) return;
        theories.clear();
        theoryDependencies.clear();

        Map<ResourceLocation, Set<Theory>> groupedTheories = theoryCache.values().stream()
                .filter(theory -> categories.containsKey(theory.categoryId()))
                .collect(Collectors.groupingBy(
                        Theory::categoryId,
                        Collectors.toSet()
                ));
        this.theories.putAll(groupedTheories);

        theoryDependencies = Maps.newHashMap();
        for (Map.Entry<Theory, List<ResourceLocation>> entry : dependenciesCache.entrySet()) {
            Theory sourceTheory = entry.getKey();
            List<Theory> actualDependencies = entry.getValue().stream()
                    .map(theoryCache::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!actualDependencies.isEmpty()) {
                theoryDependencies.put(sourceTheory, actualDependencies);
            }
        }
        theoryCache.clear();
        dependenciesCache.clear();
        //syncTheories(); 不应该在这里调用...
        processed = true;
    }
    private Theory getTheoryFromCache(ResourceLocation location) {
        return theories.values().stream()
                .flatMap(Set::stream)
                .filter(t -> t.id().equals(location))
                .findFirst()
                .orElse(null);
    }
    public Map<ResourceLocation, Set<Theory>> getTheories() {
        return theories;
    }
    public Map<ResourceLocation, TheoryCategory> getCategories() {
        return categories;
    }
    public Map<Theory, List<Theory>> getTheoryDependencies() {
        return theoryDependencies;
    }
    public void syncTheories() {
        PacketDistributor.sendToAllPlayers(new SyncTheoriesPayload(
                categories.values().stream().toList(),
                theories.values().stream().flatMap(Set::stream).collect(Collectors.toList())
        ));
    }
    public void syncPlayerTheories(Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncPlayerTheoriesPayload(
                player.getUUID(),
                playerTheories.getOrDefault(player.getUUID(), new HashSet<>())
        ));
    }
    public List<ResourceLocation> getAllTheoryIds() {
        return theories.values().stream()
                .flatMap(Set::stream)
                .map(Theory::id)
                .collect(Collectors.toList());
    }
}