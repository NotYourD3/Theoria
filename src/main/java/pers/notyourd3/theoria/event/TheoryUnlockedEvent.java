package pers.notyourd3.theoria.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event; // 导入基类 Event

// 注意：这个事件应该在服务器端（逻辑端）触发。

public class TheoryUnlockedEvent extends Event {

    private final Player player;
    private final ResourceLocation theoryId;

    public TheoryUnlockedEvent(Player player, ResourceLocation theoryId) {
        this.player = player;
        this.theoryId = theoryId;
    }

    /**
     * 获取解锁理论的玩家。
     * @return 玩家对象
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 获取被解锁理论的 ResourceLocation ID。
     * @return 理论ID
     */
    public ResourceLocation getTheoryId() {
        return theoryId;
    }

    // 如果事件是可取消的，需要重写 isCancelable() 并返回 true。
    // 在这个场景中，解锁理论通常不应该被取消，所以保持默认（不可取消）即可。
}