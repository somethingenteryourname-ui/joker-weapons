package com.vorinity.jokerweapons;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** /jokerweapons give <mace|spear> [player]  (ops only) */
public final class WeaponsCommand {
	private WeaponsCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jokerweapons")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("give")
						.then(Commands.literal("mace")
								.executes(ctx -> give(ctx.getSource(), ctx.getSource().getPlayerOrException(), true))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))))
						.then(Commands.literal("spear")
								.executes(ctx -> give(ctx.getSource(), ctx.getSource().getPlayerOrException(), false))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))))));
	}

	private static int give(CommandSourceStack source, ServerPlayer target, boolean mace) {
		ItemStack stack = mace ? Weapons.stormMace() : Weapons.orbitalSpear(source.registryAccess());
		String itemName = stack.getHoverName().getString();

		if (!target.getInventory().add(stack)) {
			target.drop(stack, false);
		}

		String playerName = target.getName().getString();
		source.sendSuccess(() -> Component.literal("Gave " + itemName + " to " + playerName), true);
		return 1;
	}
}
