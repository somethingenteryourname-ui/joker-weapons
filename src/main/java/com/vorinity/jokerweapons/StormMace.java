package com.vorinity.jokerweapons;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;

public final class StormMace {
	/** Same cooldown a normal wind charge has (half a second). */
	public static final int WIND_CHARGE_COOLDOWN_TICKS = 10;

	private StormMace() {
	}

	/** Fires a wind charge exactly like throwing the vanilla item. */
	public static void fireWindCharge(ServerLevel level, Player player, ItemStack stack) {
		WindCharge charge = new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ());
		charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
		level.addFreshEntity(charge);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

		player.getCooldowns().addCooldown(stack, WIND_CHARGE_COOLDOWN_TICKS);
	}
}
