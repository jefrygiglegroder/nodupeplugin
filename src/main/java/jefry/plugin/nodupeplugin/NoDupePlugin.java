package jefry.plugin.nodupeplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class NoDupePlugin extends JavaPlugin {

    private DiscordNotifier discordNotifier;
    private PlayerActivityTracker activityTracker;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        loadConfiguration();

        // Initialize classes with the config
        discordNotifier = new DiscordNotifier(getConfig());
        activityTracker = new PlayerActivityTracker(this);

        // Register events
        getServer().getPluginManager().registerEvents(new DupeEventListener(this, activityTracker, discordNotifier), this);

        getLogger().info("NoDupePlugin has been enabled and is actively monitoring for dupes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NoDupePlugin has been disabled.");
    }

    private void loadConfiguration() {
        reloadConfig();
    }
}
