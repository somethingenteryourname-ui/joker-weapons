package com.vorinity.jokerweapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sneak + right-click with the Orbital Spear: a red beam comes down on the block you're
 * looking at, then TNT gets flung out from the center in rings.
 */
public final class OrbitalStrike {
	// ---- Tweak these to change the ability ----
	public static final double TARGET_RANGE = 160.0;       // how far away you can aim
	public static final int COOLDOWN_TICKS = 20 * 30;      // 30 seconds
	public static final int BEAM_TICKS = 40;               // beam shows for 2 seconds before the TNT
	public static final int BEAM_HEIGHT = 100;
	public static final int[] RING_RADII = {0, 4, 8, 12};  // blocks from the center
	public static final int[] RING_TNT = {1, 8, 14, 20};   // TNT per ring (43 total)
	public static final int TNT_FUSE = 50;

	private static final DustParticleOptions BEAM_RED = new DustParticleOptions(0xFF1A1A, 2.5F);
	private static final DustParticleOptions RING_RED = new DustParticleOptions(0xAA0000, 1.5F);

	private static final List<Strike> ACTIVE = new ArrayList<>();
	private static final Map<UUID, Long> READY_AT = new HashMap<>();

	private OrbitalStrike() {
	}

	public static void tryStart(ServerLevel level, Player player) {
		long now = level.getServer().getTickCount();
		Long readyAt = READY_AT.get(player.getUUID());
		if (readyAt != null && now < readyAt) {
			long seconds = (readyAt - now + 19) / 20;
			player.displayClientMessage(Component.literal("Orbital Strike recharging: " + seconds + "s")
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		HitResult hit = player.pick(TARGET_RANGE, 1.0F, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.displayClientMessage(Component.literal("No target in range - aim at the ground")
					.withStyle(ChatFormatting.GRAY), true);
			return;
		}

		Vec3 target = hit.getLocation();
		ACTIVE.add(new Strike(level, target, player.getUUID()));
		READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);

		player.displayClientMessage(Component.literal("ORBITAL STRIKE INBOUND")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
		level.playSound(null, target.x, target.y, target.z,
				SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 0.5F);
	}

	public static void tick(MinecraftServer server) {
		Iterator<Strike> it = ACTIVE.iterator();
		while (it.hasNext()) {
			if (it.next().tick(server)) {
				it.remove();
			}
		}
	}

	public static void clear() {
		ACTIVE.clear();
		READY_AT.clear();
	}

	private static final class Strike {
		private final ServerLevel level;
		private final Vec3 pos;
		private final UUID owner;
		private int age;

		Strike(ServerLevel level, Vec3 pos, UUID owner) {
			this.level = level;
			this.pos = pos;
			this.owner = owner;
		}

		/** @return true when finished */
		boolean tick(MinecraftServer server) {
			if (age < BEAM_TICKS) {
				if (age % 2 == 0) {
					drawBeam();
				}
				if (age % 4 == 0) {
					drawRing(RING_RADII[RING_RADII.length - 1]);
				}
				if (age == BEAM_TICKS - 20) {
					level.playSound(null, pos.x, pos.y, pos.z,
							SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 4.0F, 0.8F);
				}
				age++;
				return false;
			}

			launchTnt(server);
			return true;
		}

		private void drawBeam() {
			for (int dy = 0; dy < BEAM_HEIGHT; dy++) {
				level.sendParticles(BEAM_RED, true, true,
						pos.x, pos.y + dy, pos.z,
						2, 0.12, 0.5, 0.12, 0.0);
			}
		}

		private void drawRing(double radius) {
			int points = 48;
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				level.sendParticles(RING_RED, true, true,
						pos.x + Math.cos(a) * radius, pos.y + 0.2, pos.z + Math.sin(a) * radius,
						1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		private void launchTnt(MinecraftServer server) {
			ServerPlayer shooter = server.getPlayerList().getPlayer(owner);

			for (int ring = 0; ring < RING_RADII.length; ring++) {
				int count = RING_TNT[ring];
				double radius = RING_RADII[ring];
				// Upward speed 0.5 gives ~20 "effective" ticks of travel, so radius/20 lands it on the ring.
				double outward = radius / 20.0;
				double offset = ring * 0.35;

				for (int i = 0; i < count; i++) {
					double a = offset + Math.PI * 2 * i / count;
					PrimedTnt tnt = new PrimedTnt(level, pos.x, pos.y + 1.0, pos.z, shooter);
					tnt.setDeltaMovement(Math.cos(a) * outward, 0.5, Math.sin(a) * outward);
					tnt.setFuse(TNT_FUSE + level.getRandom().nextInt(10));
					level.addFreshEntity(tnt);
				}
			}

			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, true,
					pos.x, pos.y + 1.0, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.playSound(null, pos.x, pos.y, pos.z,
					SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0F, 0.6F);
		}
	}
}
