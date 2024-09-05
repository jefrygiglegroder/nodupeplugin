package jefry.plugin.nodupeplugin;

import org.bukkit.entity.Player;

import java.util.*;

public class PlayerActivityTracker {
    private final Map<UUID, PlayerActivity> playerActivityMap = new HashMap<>();
    private final Set<UUID> playersQuitting = new HashSet<>();

    public PlayerActivityTracker(NoDupePlugin plugin) {}

    public void trackActivity(Player player, String action) {
        UUID playerId = player.getUniqueId();
        PlayerActivity activity = playerActivityMap.getOrDefault(playerId, new PlayerActivity());
        activity.recordAction(action);
        playerActivityMap.put(playerId, activity);
    }

    public boolean isPlayerQuitting(UUID playerId) {
        return playersQuitting.contains(playerId);
    }

    public void setPlayerQuitting(UUID playerId, boolean quitting) {
        if (quitting) {
            playersQuitting.add(playerId);
        } else {
            playersQuitting.remove(playerId);
        }
    }

    // Inner class for player activity tracking
    private static class PlayerActivity {
        private final List<Long> actionTimestamps = new ArrayList<>();

        public void recordAction(String action) {
            long currentTime = System.currentTimeMillis();
            actionTimestamps.add(currentTime);
            actionTimestamps.removeIf(timestamp -> currentTime - timestamp > 5000); // Remove old entries
        }
    }
}
