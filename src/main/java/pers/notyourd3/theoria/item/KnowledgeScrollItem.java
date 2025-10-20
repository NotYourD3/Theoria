package pers.notyourd3.theoria.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import pers.notyourd3.theoria.theory.Theory;
import pers.notyourd3.theoria.theory.TheoryManager;

import java.util.Optional;
import java.util.function.Consumer;

public class KnowledgeScrollItem extends Item {
    public KnowledgeScrollItem(Properties properties) {
        super(properties);
    }
    @Override
    public boolean isFoil(ItemStack itemStack) {
        return !getTheory(itemStack).getPath().equals("null");
    }
    public static ResourceLocation getTheory(ItemStack itemStack) {
        return itemStack.getOrDefault(ModDataComponents.SAVED_THEORY.get(), new ModDataComponents.savedTheory(ResourceLocation.withDefaultNamespace("null"))).location();
    }
    @Override
    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (!pLevel.isClientSide()) {
            if(TheoryManager.getInstance().applyTheoryToPlayer(pPlayer,getTheory(itemstack))){
            itemstack.consume(1,pPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
            ResourceLocation theoryId = getTheory(stack);
            if (!theoryId.getPath().equals("null")) {
                TheoryManager.getInstance().getTheory(theoryId).ifLeft(theory -> {
                    Component theoryTitle = theory.title();
                    tooltipAdder.accept(theoryTitle);
                });
            }
        }

}
