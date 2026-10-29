package me.jokerweapons;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /jokerweapons give <mace|spear> [player]   and   /jokerweapons reload */
public final class WeaponsCommand implements CommandExecutor, TabCompleter {
	private final JokerWeaponsPlugin plugin;

	public WeaponsCommand(JokerWeaponsPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("jokerweapons.reload")) {
				sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
				return true;
			}
			plugin.reloadConfig();
			sender.sendMessage(Component.text("Joker Weapons config reloaded.", NamedTextColor.GREEN));
			return true;
		}

		if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
			sender.sendMessage(Component.text("Usage: /" + label + " give <mace|spear> [player]  or  /" + label + " reload",
					NamedTextColor.YELLOW));
			return true;
		}
		if (!sender.hasPermission("jokerweapons.give")) {
			sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
			return true;
		}

		ItemStack item;
		switch (args[1].toLowerCase()) {
			case "mace" -> item = plugin.weapons().stormMace();
			case "spear" -> item = plugin.weapons().orbitalSpear();
			default -> {
				sender.sendMessage(Component.text("Pick mace or spear.", NamedTextColor.RED));
				return true;
			}
		}

		Player target;
		if (args.length >= 3) {
			target = Bukkit.getPlayerExact(args[2]);
			if (target == null) {
				sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
				return true;
			}
		} else if (sender instanceof Player p) {
			target = p;
		} else {
			sender.sendMessage(Component.text("Say which player to give it to.", NamedTextColor.RED));
			return true;
		}

		target.getInventory().addItem(item).values()
				.forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
		sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
				.append(item.getItemMeta().itemName())
				.append(Component.text(" to " + target.getName(), NamedTextColor.GREEN)));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> options = new ArrayList<>();
		if (args.length == 1) {
			options.add("give");
			options.add("reload");
		} else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
			options.add("mace");
			options.add("spear");
		} else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
			Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
		} else {
			return options;
		}
		String typed = args[args.length - 1].toLowerCase();
		options.removeIf(o -> !o.toLowerCase().startsWith(typed));
		return options;
	}
}
