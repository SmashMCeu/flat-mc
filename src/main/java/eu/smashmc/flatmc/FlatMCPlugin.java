package eu.smashmc.flatmc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FlatMCPlugin extends JavaPlugin {

	@Override
	public void onEnable() {
		Bukkit.broadcastMessage("Hello world");
		super.onEnable();
	}
}
