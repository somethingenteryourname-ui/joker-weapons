package me.jokerweapons;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class JokerWeaponsPlugin extends JavaPlugin {
	private Weapons weapons;
	private OrbitalStrike orbitalStrike;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		weapons = new Weapons(this);
		orbitalStrike = new OrbitalStrike(this);

		getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
		getServer().getPluginManager().registerEvents(orbitalStrike, this);

		WeaponsCommand command = new WeaponsCommand(this);
		PluginCommand cmd = getCommand("jokerweapons");
		if (cmd != null) {
			cmd.setExecutor(command);
			cmd.setTabCompleter(command);
		}
	}

	@Override
	public void onDisable() {
		if (orbitalStrike != null) {
			orbitalStrike.cancelAll();
		}
	}

	public Weapons weapons() {
		return weapons;
	}

	public OrbitalStrike orbitalStrike() {
		return orbitalStrike;
	}
}
