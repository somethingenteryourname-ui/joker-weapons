package me.jokerweapons;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Both weapons are normal vanilla items (mace / netherite spear) with a hidden tag,
 * so players don't need a resource pack or a client mod.
 */
public final class Weapons {
	public static final String STORM_MACE = "storm_mace";
	public static final String ORBITAL_SPEAR = "orbital_spear";

	// Vanilla mace stats. Adding any attribute modifier replaces the item's defaults,
	// so these get re-added to keep the mace's normal damage and attack speed.
	private static final double MACE_ATTACK_DAMAGE_BONUS = 5.0;  // 1 base + 5 = 6
	private static final double MACE_ATTACK_SPEED_BONUS = -3.4;  // 4 base - 3.4 = 0.6

	private final JokerWeaponsPlugin plugin;
	private final NamespacedKey weaponKey;
	private final NamespacedKey reachKey;

	public Weapons(JokerWeaponsPlugin plugin) {
		this.plugin = plugin;
		this.weaponKey = new NamespacedKey(plugin, "weapon");
		this.reachKey = new NamespacedKey(plugin, "storm_mace_reach");
	}

	public ItemStack stormMace() {
		ItemStack stack = new ItemStack(Material.MACE);
		ItemMeta meta = stack.getItemMeta();

		meta.itemName(Component.text("Storm Mace", NamedTextColor.AQUA, TextDecoration.BOLD));
		meta.lore(List.of(
				line("Right-click: Wind Charge", NamedTextColor.GRAY),
				line("Max entity reach while held", NamedTextColor.GRAY)));

		double reach = plugin.getConfig().getDouble("storm-mace.extra-entity-reach", 61.0);
		meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
				NamespacedKey.minecraft("base_attack_damage"), MACE_ATTACK_DAMAGE_BONUS,
				AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
				NamespacedKey.minecraft("base_attack_speed"), MACE_ATTACK_SPEED_BONUS,
				AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, new AttributeModifier(
				reachKey, reach, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

		meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, STORM_MACE);
		stack.setItemMeta(meta);
		return stack;
	}

	public ItemStack orbitalSpear() {
		ItemStack stack = new ItemStack(Material.NETHERITE_SPEAR);
		ItemMeta meta = stack.getItemMeta();

		meta.itemName(Component.text("Orbital Spear", NamedTextColor.RED, TextDecoration.BOLD));
		meta.lore(List.of(
				line("Sneak + right-click: Orbital Strike", NamedTextColor.GRAY),
				line("Aim at the ground", NamedTextColor.DARK_GRAY)));

		int sharpness = plugin.getConfig().getInt("orbital-spear.sharpness-level", 6);
		meta.addEnchant(Enchantment.SHARPNESS, sharpness, true);

		meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, ORBITAL_SPEAR);
		stack.setItemMeta(meta);
		return stack;
	}

	/** Returns the weapon id on the item, or "" if it isn't one of ours. */
	public String idOf(ItemStack stack) {
		if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
			return "";
		}
		String id = stack.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
		return id == null ? "" : id;
	}

	private static Component line(String text, NamedTextColor color) {
		return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
	}
}
