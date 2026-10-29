package com.vorinity.jokerweapons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JokerWeapons implements ModInitializer {
	public static final String MOD_ID = "jokerweapons";

	@Override
	public void onInitialize() {
		UseItemCallback.EVENT.register(JokerWeapons::onUseItem);
		ServerTickEvents.END_SERVER_TICK.register(OrbitalStrike::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> OrbitalStrike.clear());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				WeaponsCommand.register(dispatcher));

		// Both weapons show up at the end of the Combat creative tab.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(Weapons.stormMace());
			entries.accept(Weapons.orbitalSpear(entries.getContext().holders()));
		});
	}

	/**
	 * Runs on both the client and the server. The client side only decides whether to
	 * send the right-click to the server; the actual abilities run server-side.
	 */
	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		String weapon = Weapons.idOf(stack);

		if (weapon.equals(Weapons.STORM_MACE)) {
			if (player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.FAIL;
			}
			if (level instanceof ServerLevel serverLevel) {
				StormMace.fireWindCharge(serverLevel, player, stack);
			}
			return InteractionResult.SUCCESS;
		}

		if (weapon.equals(Weapons.ORBITAL_SPEAR) && player.isShiftKeyDown()) {
			// Sneak + right-click = orbital strike. Plain right-click keeps the normal spear charge.
			if (level instanceof ServerLevel serverLevel) {
				OrbitalStrike.tryStart(serverLevel, player);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}
}
