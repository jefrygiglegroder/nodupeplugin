package jefry.plugin.nodupeplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class DupeEventListener implements Listener {

    private final NoDupePlugin plugin;
    private final PlayerActivityTracker activityTracker;
    private final DiscordNotifier discordNotifier;

    public DupeEventListener(NoDupePlugin plugin, PlayerActivityTracker activityTracker, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.activityTracker = activityTracker;
        this.discordNotifier = discordNotifier;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (activityTracker.isPlayerQuitting(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getLogger().warning("Cancelled item drop as player was logging out: " + player.getName());
            discordNotifier.sendAlert(player, "item-drop");
        } else {
            activityTracker.trackActivity(player, "item-drop");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activityTracker.setPlayerQuitting(player.getUniqueId(), true);

        player.saveData();
        plugin.getLogger().info("Player data saved on logout for " + player.getName());
        discordNotifier.sendAlert(player, "logout");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> activityTracker.setPlayerQuitting(player.getUniqueId(), false), 20L);
    }
}
