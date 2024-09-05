package jefry.plugin.nodupeplugin;

import org.bukkit.entity.Player;

public class ClientVersionChecker {
    public static boolean isBedrockPlayer(Player player) {
        // Assuming GeyserMC provides a way to detect Bedrock clients
        return player.getEffectivePermissions().stream()
                .anyMatch(perm -> perm.getPermission().equalsIgnoreCase("geyser.platform.bedrock"));
    }
}
