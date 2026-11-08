package vice.sol_valheim;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/** Register during the loader's command event; the root command requires permission level 2. */
public final class FoodCommands {
    private FoodCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sol_valheim")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    if (!PlayerLifecycle.reloadConfig(context.getSource().getServer())) {
                        context.getSource().sendFailure(Component.translatable("commands.sol_valheim.reload.failed"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.translatable("commands.sol_valheim.reload.success"), true);
                    return 1;
                }))
                .then(slots()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> slots() {
        return Commands.literal("slots")
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("slots", IntegerArgumentType.integer(2, 5))
                                        .executes(context -> {
                                            var players = EntityArgument.getPlayers(context, "players");
                                            int slots = IntegerArgumentType.getInteger(context, "slots");
                                            for (var player : players) FoodApi.setMaxSlots(player, slots);
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.sol_valheim.slots.set", players.size(), slots), true);
                                            return players.size();
                                        }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players()).executes(context -> {
                            var players = EntityArgument.getPlayers(context, "players");
                            for (var player : players) FoodApi.resetMaxSlots(player);
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    "commands.sol_valheim.slots.reset", players.size()), true);
                            return players.size();
                        })))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                            var player = EntityArgument.getPlayer(context, "player");
                            int slots = FoodApi.getMaxSlots(player);
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    "commands.sol_valheim.slots.get", player.getDisplayName(), slots), false);
                            return slots;
                        })));
    }
}
