package jefry.plugin.nodupeplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

public class DiscordNotifier {
    private final FileConfiguration config;

    public DiscordNotifier(FileConfiguration config) {
        this.config = config;
    }

    public void sendAlert(Player player, String actionType) {
        String webhookUrl = config.getString("discord-webhook-url");

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            Bukkit.getLogger().severe("Webhook URL is missing or invalid!");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String titleTemplate = config.getString("alert-settings.title", "Alert - {player}");
                    String descriptionTemplate = config.getString("alert-settings.description", "{player} triggered: **{actionType}**.");
                    int embedColor = config.getInt("alert-settings.color", 16711680); // Default to red color
                    String actionMessage = config.getString("action-types." + actionType.toLowerCase(), actionType);

                    String playerName = player.getName();
                    String title = titleTemplate.replace("{player}", playerName);
                    String description = descriptionTemplate.replace("{player}", playerName).replace("{actionType}", actionMessage);

                    String avatarUrl = "https://minotar.net/helm/" + playerName + "/64.png";
                    String location = player.getLocation().getWorld().getName() + " (" +
                            player.getLocation().getBlockX() + ", " +
                            player.getLocation().getBlockY() + ", " +
                            player.getLocation().getBlockZ() + ")";
                    String time = LocalDateTime.now().toString();
                    String serverName = player.getServer().getName();
                    String footerText = config.getString("footer-text", "Server: {server}").replace("{server}", serverName);

                    String jsonPayload = "{"
                            + "\"username\": \"NoDupePlugin Alert\","
                            + "\"embeds\": [{"
                            + "\"title\": \"" + title + "\","
                            + "\"description\": \"" + description + "\","
                            + "\"color\": " + embedColor + ","
                            + "\"fields\": ["
                            + "{"
                            + "\"name\": \"Player Name\","
                            + "\"value\": \"" + playerName + "\","
                            + "\"inline\": true"
                            + "},"
                            + "{"
                            + "\"name\": \"Action\","
                            + "\"value\": \"" + actionMessage + "\","
                            + "\"inline\": true"
                            + "},"
                            + "{"
                            + "\"name\": \"Time\","
                            + "\"value\": \"" + time + "\","
                            + "\"inline\": true"
                            + "},"
                            + "{"
                            + "\"name\": \"Location\","
                            + "\"value\": \"" + location + "\","
                            + "\"inline\": false"
                            + "}"
                            + "],"
                            + "\"thumbnail\": {"
                            + "\"url\": \"" + avatarUrl + "\""
                            + "},"
                            + "\"footer\": {"
                            + "\"text\": \"" + footerText + "\","
                            + "\"icon_url\": \"" + avatarUrl + "\""
                            + "}"
                            + "}]"
                            + "}";

                    URL url = new URL(webhookUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(jsonPayload.getBytes());
                        outputStream.flush();
                    }

                    connection.getInputStream();
                    connection.disconnect();

                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to send Discord alert: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("NoDupePlugin"));
    }
}
