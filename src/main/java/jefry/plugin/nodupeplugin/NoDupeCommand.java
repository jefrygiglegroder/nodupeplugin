package jefry.plugin.nodupeplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NoDupeCommand implements CommandExecutor {

    private final NoDupePlugin plugin;

    public NoDupeCommand(NoDupePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nodupe.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            // Reload the configuration
            plugin.reloadConfig();
            plugin.loadConfiguration();

            sender.sendMessage("§aNoDupePlugin configuration reloaded successfully.");
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
            return true;
        }

        // Default message for /nodupe command
        sender.sendMessage("§eUsage: /" + label + " reload");
        return true;
    }
}
