package com.vorinity.jokerweapons;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The two weapons are vanilla items (mace / netherite spear) marked with custom data,
 * so no textures or new item registrations are needed.
 */
public final class Weapons {
	public static final String TAG_KEY = "jokerweapons_weapon";
	public static final String STORM_MACE = "storm_mace";
	public static final String ORBITAL_SPEAR = "orbital_spear";

	/** Base player entity reach is 3; the game caps the attribute at 64, so +61 is the max. */
	public static final double MACE_EXTRA_REACH = 61.0;
	public static final int SPEAR_SHARPNESS = 6;

	private Weapons() {
	}

	public static ItemStack stormMace() {
		ItemStack stack = new ItemStack(Items.MACE);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Storm Mace")
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				line("Right-click: Wind Charge", ChatFormatting.GRAY),
				line("Max entity reach while held", ChatFormatting.GRAY))));

		ItemAttributeModifiers modifiers = stack
				.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
				.withModifierAdded(
						Attributes.ENTITY_INTERACTION_RANGE,
						new AttributeModifier(
								Identifier.fromNamespaceAndPath(JokerWeapons.MOD_ID, "storm_mace_reach"),
								MACE_EXTRA_REACH,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND);
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);

		mark(stack, STORM_MACE);
		return stack;
	}

	public static ItemStack orbitalSpear(HolderLookup.Provider registries) {
		ItemStack stack = new ItemStack(Items.NETHERITE_SPEAR);
		stack.set(DataComponents.ITEM_NAME, Component.literal("Orbital Spear")
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				line("Sneak + right-click: Orbital Strike", ChatFormatting.GRAY),
				line("Aim at the ground", ChatFormatting.DARK_GRAY))));
		stack.enchant(
				registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
				SPEAR_SHARPNESS);

		mark(stack, ORBITAL_SPEAR);
		return stack;
	}

	/** Returns the weapon id stored on the stack, or "" if it's not one of ours. */
	public static String idOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return "";
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return "";
		}
		return data.copyTag().getStringOr(TAG_KEY, "");
	}

	private static void mark(ItemStack stack, String id) {
		CompoundTag tag = new CompoundTag();
		tag.putString(TAG_KEY, id);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	private static Component line(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(style -> style.withItalic(false).withColor(color));
	}
}
