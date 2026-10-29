package me.jokerweapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class WeaponListener implements Listener {
	private final JokerWeaponsPlugin plugin;
	/** Server tick when each player's mace wind charge is ready again. */
	private final Map<UUID, Integer> maceReadyAt = new HashMap<>();

	public WeaponListener(JokerWeaponsPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		String weapon = plugin.weapons().idOf(item);
		if (weapon.isEmpty() || !player.hasPermission("jokerweapons.use")) {
			return;
		}

		if (weapon.equals(Weapons.STORM_MACE)) {
			event.setUseItemInHand(Event.Result.DENY);
			fireWindCharge(player);
		} else if (weapon.equals(Weapons.ORBITAL_SPEAR) && player.isSneaking()) {
			// Sneak + right-click = orbital strike. A normal right-click keeps the spear's charge attack.
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);
			plugin.orbitalStrike().tryStart(player);
		}
	}

	private void fireWindCharge(Player player) {
		int now = plugin.getServer().getCurrentTick();
		Integer readyAt = maceReadyAt.get(player.getUniqueId());
		if (readyAt != null && now < readyAt) {
			return;
		}

		int cooldown = plugin.getConfig().getInt("storm-mace.wind-charge-cooldown-ticks", 10);
		double speed = plugin.getConfig().getDouble("storm-mace.wind-charge-speed", 1.5);

		WindCharge charge = player.launchProjectile(WindCharge.class);
		charge.setVelocity(player.getEyeLocation().getDirection().multiply(speed));

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW,
				SoundCategory.PLAYERS, 0.5F, 0.4F / (float) (Math.random() * 0.4 + 0.8));

		maceReadyAt.put(player.getUniqueId(), now + cooldown);
		// Shows the white cooldown overlay on maces in the hotbar.
		player.setCooldown(player.getInventory().getItemInMainHand(), cooldown);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		maceReadyAt.remove(event.getPlayer().getUniqueId());
	}
}
