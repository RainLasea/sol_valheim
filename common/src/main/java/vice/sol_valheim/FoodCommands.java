package vice.sol_valheim;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

final class FoodCommands {
    private FoodCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> slots() {
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
