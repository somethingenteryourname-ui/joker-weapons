package me.jokerweapons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Sneak + right-click with the Orbital Spear: a red beam comes down where you're looking,
 * then TNT gets flung out from the center in rings.
 */
public final class OrbitalStrike implements Listener {
	private static final Particle.DustOptions BEAM_RED = new Particle.DustOptions(Color.fromRGB(0xFF1A1A), 2.5F);
	private static final Particle.DustOptions RING_RED = new Particle.DustOptions(Color.fromRGB(0xAA0000), 1.5F);

	private final JokerWeaponsPlugin plugin;
	private final NamespacedKey noBlockDamageKey;
	private final Map<UUID, Long> readyAt = new HashMap<>();
	private final Set<BukkitTask> active = new HashSet<>();

	public OrbitalStrike(JokerWeaponsPlugin plugin) {
		this.plugin = plugin;
		this.noBlockDamageKey = new NamespacedKey(plugin, "orbital_no_block_damage");
	}

	public void tryStart(Player player) {
		long now = System.currentTimeMillis();
		Long ready = readyAt.get(player.getUniqueId());
		if (ready != null && now < ready) {
			long seconds = (ready - now + 999) / 1000;
			player.sendActionBar(Component.text("Orbital Strike recharging: " + seconds + "s", NamedTextColor.RED));
			return;
		}

		double range = plugin.getConfig().getDouble("orbital-spear.target-range", 160);
		RayTraceResult hit = player.getWorld().rayTraceBlocks(
				player.getEyeLocation(), player.getEyeLocation().getDirection(), range,
				FluidCollisionMode.NEVER, true);
		if (hit == null || hit.getHitBlock() == null) {
			player.sendActionBar(Component.text("No target in range - aim at the ground", NamedTextColor.GRAY));
			return;
		}

		Location target = hit.getHitPosition().toLocation(player.getWorld());
		int cooldownSeconds = plugin.getConfig().getInt("orbital-spear.cooldown-seconds", 30);
		readyAt.put(player.getUniqueId(), now + cooldownSeconds * 1000L);

		player.sendActionBar(Component.text("ORBITAL STRIKE INBOUND", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		target.getWorld().playSound(target, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 4.0F, 0.5F);

		startStrike(player, target);
	}

	private void startStrike(Player shooter, Location target) {
		int beamTicks = plugin.getConfig().getInt("orbital-spear.beam-ticks", 40);
		int beamHeight = plugin.getConfig().getInt("orbital-spear.beam-height", 100);
		List<Integer> radii = plugin.getConfig().getIntegerList("orbital-spear.ring-radii");
		List<Integer> counts = plugin.getConfig().getIntegerList("orbital-spear.ring-tnt");
		int fuse = plugin.getConfig().getInt("orbital-spear.tnt-fuse-ticks", 50);
		boolean breakBlocks = plugin.getConfig().getBoolean("orbital-spear.break-blocks", true);
		int outerRadius = radii.isEmpty() ? 12 : radii.get(radii.size() - 1);

		int[] age = {0};
		Consumer<BukkitTask> step = task -> {
			World world = target.getWorld();
			if (age[0] < beamTicks) {
				if (age[0] % 2 == 0) {
					drawBeam(world, target, beamHeight);
				}
				if (age[0] % 4 == 0) {
					drawRing(world, target, outerRadius);
				}
				if (age[0] == beamTicks - 20) {
					world.playSound(target, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 4.0F, 0.8F);
				}
				age[0]++;
				return;
			}

			launchTnt(world, target, shooter, radii, counts, fuse, breakBlocks);
			active.remove(task);
			task.cancel();
		};
		plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
			active.add(task);
			step.accept(task);
		}, 0L, 1L);
	}

	private void drawBeam(World world, Location base, int height) {
		for (int dy = 0; dy < height; dy++) {
			world.spawnParticle(Particle.DUST, base.getX(), base.getY() + dy, base.getZ(),
					2, 0.12, 0.5, 0.12, 0.0, BEAM_RED, true);
		}
	}

	private void drawRing(World world, Location center, double radius) {
		int points = 48;
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			world.spawnParticle(Particle.DUST,
					center.getX() + Math.cos(a) * radius, center.getY() + 0.2, center.getZ() + Math.sin(a) * radius,
					1, 0.0, 0.0, 0.0, 0.0, RING_RED, true);
		}
	}

	private void launchTnt(World world, Location center, Player shooter, List<Integer> radii,
			List<Integer> counts, int fuse, boolean breakBlocks) {
		Location spawn = center.clone().add(0, 1.0, 0);
		int rings = Math.min(radii.size(), counts.size());

		for (int ring = 0; ring < rings; ring++) {
			int count = counts.get(ring);
			double radius = radii.get(ring);
			// Upward speed 0.5 keeps it in the air ~20 ticks, so radius/20 lands it on the ring.
			double outward = radius / 20.0;
			double offset = ring * 0.35;

			for (int i = 0; i < count; i++) {
				double a = offset + Math.PI * 2 * i / count;
				Vector velocity = new Vector(Math.cos(a) * outward, 0.5, Math.sin(a) * outward);
				int thisFuse = fuse + (int) (Math.random() * 10);
				world.spawn(spawn, TNTPrimed.class, tnt -> {
					tnt.setSource(shooter);
					tnt.setFuseTicks(thisFuse);
					tnt.setVelocity(velocity);
					if (!breakBlocks) {
						tnt.getPersistentDataContainer().set(noBlockDamageKey, PersistentDataType.BYTE, (byte) 1);
					}
				});
			}
		}

		world.spawnParticle(Particle.EXPLOSION_EMITTER, spawn, 1, 0, 0, 0, 0, null, true);
		world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 4.0F, 0.6F);
	}

	@EventHandler
	public void onExplode(EntityExplodeEvent event) {
		if (event.getEntity().getPersistentDataContainer().has(noBlockDamageKey, PersistentDataType.BYTE)) {
			event.blockList().clear();
		}
	}

	public void cancelAll() {
		for (BukkitTask task : active) {
			task.cancel();
		}
		active.clear();
		readyAt.clear();
	}
}
