package pers.notyourd3.theoria.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import pers.notyourd3.theoria.theory.TheoryManager;
import java.util.Collection;

public class TheoryCommand {
    private static final SuggestionProvider<CommandSourceStack> THEORY_ID_SUGGESTION = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TheoryManager.getInstance().getAllTheoryIds().stream(),
                    builder
            );
    // 注册指令的主方法
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // 2. 创建一个可重用的 ResourceLocation 参数构建器
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> theoryIdArgument =
                Commands.argument("theory_id", ResourceLocationArgument.id())
                        // 3. 将 SuggestionProvider 绑定到参数上
                        .suggests(THEORY_ID_SUGGESTION);

        dispatcher.register(
                Commands.literal("theory")
                        .requires(source -> source.hasPermission(2))

                        // 子指令: /theory grant <targets> <theory_id>
                        .then(Commands.literal("grant")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(theoryIdArgument.executes(context -> grantTheory(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "theory_id")
                                        )))
                                )
                        )

                        // 子指令: /theory revoke <targets> <theory_id>
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(theoryIdArgument.executes(context -> revokeTheory( // 重用 theoryIdArgument
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "theory_id")
                                        )))
                                )
                        )
        );
    }
    // 授予理论的逻辑
    private static int grantTheory(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation theoryId) throws CommandSyntaxException {
        TheoryManager manager = TheoryManager.getInstance();
        int successCount = 0;

        // 验证理论是否存在
        if (manager.getTheory(theoryId) == null) {
            source.sendFailure(Component.translatable("commands.theoria.theory.invalid", theoryId.toString()));
            return 0;
        }

        for (ServerPlayer player : targets) {
            // 使用 forceApplyTheoryToPlayer 来绕过依赖检查，直接授予理论
            manager.forceApplyTheoryToPlayer(player, theoryId);
            successCount++;
        }

        if (successCount == 1) {
            source.sendSuccess(() -> Component.translatable("commands.theoria.theory.grant.success.single",
                    theoryId.toString(), targets.iterator().next().getDisplayName()), true);
        } else if (successCount > 1) {
            int finalSuccessCount = successCount;
            source.sendSuccess(() -> Component.translatable("commands.theoria.theory.grant.success.multiple",
                    theoryId.toString(), finalSuccessCount), true);
        } else {
            source.sendFailure(Component.translatable("commands.theoria.theory.grant.failure", theoryId.toString()));
        }

        return successCount;
    }

    // 移除理论的逻辑
    private static int revokeTheory(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation theoryId) throws CommandSyntaxException {
        TheoryManager manager = TheoryManager.getInstance();
        int successCount = 0;

        for (ServerPlayer player : targets) {
            // TheoryManager 中需要一个新的 revoke 方法
            boolean revoked = manager.revokeTheoryFromPlayer(player, theoryId);
            if (revoked) {
                successCount++;
            }
        }

        if (successCount == 1) {
            source.sendSuccess(() -> Component.translatable("commands.theoria.theory.revoke.success.single",
                    theoryId.toString(), targets.iterator().next().getDisplayName()), true);
        } else if (successCount > 1) {
            int finalSuccessCount = successCount;
            source.sendSuccess(() -> Component.translatable("commands.theoria.theory.revoke.success.multiple",
                    theoryId.toString(), finalSuccessCount), true);
        } else {
            source.sendFailure(Component.translatable("commands.theoria.theory.revoke.failure", theoryId.toString()));
        }

        return successCount;
    }
}