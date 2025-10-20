package pers.notyourd3.theoria.client;

import com.google.common.collect.Maps;
import pers.notyourd3.theoria.theory.Theory;
import pers.notyourd3.theoria.theory.TheoryCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientTheoryManager {
    private static final ClientTheoryManager INSTANCE = new ClientTheoryManager();
    private Map<ResourceLocation, TheoryCategory> categoriesById = Maps.newHashMap();
    private Map<ResourceLocation, Theory> theoriesById = Maps.newHashMap();
    private Map<TheoryCategory, List<Theory>> theoriesByCategory = Maps.newHashMap();
    private Map<Theory, List<Theory>> theoryDependencies = Maps.newHashMap();

    private Set<ResourceLocation> unlockedTheories = Set.of();

    private ClientTheoryManager() {}

    public static ClientTheoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * 由网络数据包处理器调用，用从服务器接收的数据填充管理器。
     * @param categories 收到的分类列表
     * @param theories 收到的理论列表
     */
    public void sync(List<TheoryCategory> categories, List<Theory> theories) {
        // 1. 清理旧数据
        this.categoriesById.clear();
        this.theoriesById.clear();
        this.theoriesByCategory.clear();
        this.theoryDependencies.clear();

        // 2. 填充ID到对象的映射
        this.categoriesById = categories.stream()
                .collect(Collectors.toMap(TheoryCategory::id, Function.identity()));
        this.theoriesById = theories.stream()
                .collect(Collectors.toMap(Theory::id, Function.identity()));

        // 3. 构建分类到其下属理论列表的映射
        this.theoriesByCategory = theories.stream()
                .filter(theory -> this.categoriesById.containsKey(theory.categoryId()))
                .collect(Collectors.groupingBy(
                        theory -> this.categoriesById.get(theory.categoryId())
                ));

        // 4. 构建理论的依赖关系图
        for (Theory theory : theories) {
            if (theory.dependencies() != null && !theory.dependencies().isEmpty()) {
                List<Theory> dependencies = theory.dependencies().stream()
                        .map(this.theoriesById::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!dependencies.isEmpty()) {
                    this.theoryDependencies.put(theory, dependencies);
                }
            }
        }
        System.out.println("Theoria data synced to client. " + categories.size() + " categories, " + theories.size() + " theories.");
    }

    // 在这里添加各种 getter 方法，以便 UI 或其他客户端逻辑可以访问这些数据
    public Map<TheoryCategory, List<Theory>> getTheoriesByCategory() {
        return theoriesByCategory;
    }

    public Map<ResourceLocation, TheoryCategory> getCategoriesById() {
        return categoriesById;
    }

    public List<Theory> getDependencies(Theory theory) {
        return theoryDependencies.getOrDefault(theory, List.of());
    }
    public void setUnlockedTheories(Set<ResourceLocation> unlockedTheories) {
        this.unlockedTheories = unlockedTheories;
    }

    public Set<ResourceLocation> getUnlockedTheories() {
        return unlockedTheories;
    }

    public boolean isTheoryUnlocked(ResourceLocation theoryId) {
        return unlockedTheories.contains(theoryId);
    }
    public boolean canTheoryGrant(Theory theory) {
        return unlockedTheories.containsAll(theory.dependencies());
    }
}