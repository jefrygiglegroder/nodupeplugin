package jefry.plugin.nodupeplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BedrockCheatProtector implements Listener {

    private final NoDupePlugin plugin;
    private final DiscordNotifier discordNotifier;

    public BedrockCheatProtector(NoDupePlugin plugin, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.discordNotifier = discordNotifier;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player is a Bedrock player
        if (ClientVersionChecker.isBedrockPlayer(player)) {
            // Detect extreme speed or fly hacking for Bedrock players
            double distance = event.getFrom().distance(event.getTo());
            double maxAllowedDistance = 10.0; // Max allowed distance per tick; adjust as needed

            if (distance > maxAllowedDistance) {
                event.setCancelled(true);
                plugin.getLogger().warning("Suspicious movement detected for Bedrock player: " + player.getName());
                discordNotifier.sendAlert(player, "suspicious-movement");
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Check if the player is a Bedrock player
        if (ClientVersionChecker.isBedrockPlayer(player)) {
            // Detect suspicious teleportation distance for Bedrock players
            double maxAllowedTeleportDistance = 50.0; // Adjust as needed
            double distance = event.getFrom().distance(event.getTo());

            if (distance > maxAllowedTeleportDistance) {
                event.setCancelled(true);
                plugin.getLogger().warning("Suspicious teleport detected for Bedrock player: " + player.getName());
                discordNotifier.sendAlert(player, "suspicious-teleport");
            }
        }
    }
}
