package jefry.plugin.nodupeplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class NoDupePlugin extends JavaPlugin implements Listener {

    // Plugin configuration variables
    private String discordWebhookUrl;
    private int blockPlaceThreshold;
    private int itemDropThreshold;
    private int redstoneCooldownMs;
    private List<String> whitelistedPlayers;
    private Set<Material> sensitiveMaterials;

    // Tracks redstone activations to prevent excessive flags
    private final Map<UUID, PlayerActivity> playerActivityMap = new HashMap<>();
    private final Map<Block, Long> redstoneActivationTimestamps = new HashMap<>();

    @Override
    public void onEnable() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);

        // Load the config
        loadConfiguration();

        getLogger().info("NoDupePlugin has been enabled and is actively monitoring for dupes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NoDupePlugin has been disabled.");
    }

    // Load the configuration from config.yml
    private void loadConfiguration() {
        // Create config.yml if it doesn't exist
        saveDefaultConfig();

        // Load values from the config.yml file
        discordWebhookUrl = getConfig().getString("discord_webhook_url");
        blockPlaceThreshold = getConfig().getInt("thresholds.block_place");
        itemDropThreshold = getConfig().getInt("thresholds.item_drop");
        redstoneCooldownMs = getConfig().getInt("redstone_cooldown");
        whitelistedPlayers = getConfig().getStringList("whitelisted_players");

        sensitiveMaterials = new HashSet<>();
        List<String> materialList = getConfig().getStringList("sensitive_materials");
        for (String materialName : materialList) {
            Material material = Material.getMaterial(materialName);
            if (material != null) {
                sensitiveMaterials.add(material);
            } else {
                getLogger().warning("Invalid material in config: " + materialName);
            }
        }

        getLogger().info("Configuration reloaded successfully.");
    }

    // Command to reload the config
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("nodupe")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();  // Reload the config.yml
                loadConfiguration();  // Apply the new config
                sender.sendMessage("NoDupePlugin configuration reloaded.");
                return true;
            }
        }
        return false;
    }

    // Track player activity (block places, item drops, etc.)
    private void trackPlayerActivity(Player player, String action) {
        UUID playerId = player.getUniqueId();
        PlayerActivity activity = playerActivityMap.getOrDefault(playerId, new PlayerActivity(player));

        activity.recordAction(action);
        playerActivityMap.put(playerId, activity);

        // Check if player exceeds any action threshold within the tracking window
        if (activity.getBlockPlaceCount() > blockPlaceThreshold || activity.getItemDropCount() > itemDropThreshold) {
            logSuspiciousActivity(player, "Exceeded action threshold: " + action);
            player.sendMessage("Warning: You are performing too many actions in a short time.");
        }
    }

    // Prevent inventory-related duplication (e.g., duping through shulker boxes or books)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && sensitiveMaterials.contains(clickedItem.getType())) {
                Player player = (Player) event.getWhoClicked();

                trackPlayerActivity(player, "inventory-click");
            }
        }
    }

    // Prevent duping via item drops
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (sensitiveMaterials.contains(droppedItem.getType())) {
            trackPlayerActivity(player, "item-drop");

            // Cancel drop if threshold is exceeded
            if (playerActivityMap.get(player.getUniqueId()).getItemDropCount() > itemDropThreshold) {
                logSuspiciousActivity(player, "Attempted to drop too many items.");
                event.setCancelled(true);
            }
        }
    }

    // Prevent block-based duping via excessive block placements
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (sensitiveMaterials.contains(block.getType())) {
            trackPlayerActivity(player, "block-place");

            if (playerActivityMap.get(player.getUniqueId()).getBlockPlaceCount() > blockPlaceThreshold) {
                logSuspiciousActivity(player, "Attempted to place too many sensitive blocks.");
                event.setCancelled(true); // Cancel excessive placements
            }
        }
    }

    // Rate-limited redstone activity monitoring
    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.PISTON || block.getType() == Material.HOPPER) {
            long currentTime = System.currentTimeMillis();
            long lastActivation = redstoneActivationTimestamps.getOrDefault(block, 0L);

            if (currentTime - lastActivation < redstoneCooldownMs) {
                logSuspiciousActivity(null, "Excessive redstone activity detected on " + block.getType());
                event.setNewCurrent(0); // Block redstone action
            } else {
                redstoneActivationTimestamps.put(block, currentTime);
            }
        }
    }

    // Sync player data on quit to prevent inventory dupes
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.saveData();
        logSuspiciousActivity(player, "Player data saved on logout.");
    }

    // Log suspicious activity and send a Discord webhook notification
    private void logSuspiciousActivity(Player player, String message) {
        String playerName = (player != null) ? player.getName() : "Unknown";
        getLogger().log(Level.WARNING, "[AntiDupe] {0}: {1}", new Object[]{playerName, message});

        // Send to Discord webhook
        sendDiscordAlert("[AntiDupe] " + playerName + ": " + message);
    }

    // Send alerts to a Discord channel via webhook
    private void sendDiscordAlert(String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(discordWebhookUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    String jsonPayload = "{\"content\": \"" + message + "\"}";

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(jsonPayload.getBytes());
                    outputStream.flush();
                    outputStream.close();

                    connection.getInputStream(); // Trigger the request
                    connection.disconnect();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to send Discord alert", e);
                }
            }
        }.runTaskAsynchronously(this);
    }

    // Player activity tracker (to prevent excessive actions in short bursts)
    private static class PlayerActivity {
        private final Player player;
        private final List<Long> blockPlaceTimestamps = new ArrayList<>();
        private final List<Long> itemDropTimestamps = new ArrayList<>();

        public PlayerActivity(Player player) {
            this.player = player;
        }

        public void recordAction(String action) {
            long currentTime = System.currentTimeMillis();

            if (action.equals("block-place")) {
                blockPlaceTimestamps.add(currentTime);
                blockPlaceTimestamps.removeIf(timestamp -> currentTime - timestamp > 5000); // Remove old entries
            } else if (action.equals("item-drop")) {
                itemDropTimestamps.add(currentTime);
                itemDropTimestamps.removeIf(timestamp -> currentTime - timestamp > 5000); // Remove old entries
            }
        }

        public int getBlockPlaceCount() {
            return blockPlaceTimestamps.size();
        }

        public int getItemDropCount() {
            return itemDropTimestamps.size();
        }
    }
}
