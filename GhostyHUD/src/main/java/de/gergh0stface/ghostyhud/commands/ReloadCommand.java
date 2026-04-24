package de.gergh0stface.ghostyhud.commands;

import de.gergh0stface.ghostyhud.GhostyHUD;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final GhostyHUD plugin;

    public ReloadCommand(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("ghostyhud.admin")) {
                    sender.sendMessage(plugin.getLangManager().getMessage("no-permission"));
                    return true;
                }
                try {
                    plugin.reload();
                    sender.sendMessage(plugin.getLangManager().getMessage("reload-success"));
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error during reload: " + ex.getMessage());
                    ex.printStackTrace();
                    sender.sendMessage(plugin.getLangManager().getMessage("reload-failed"));
                }
            }
            case "help" -> sendHelp(sender);
            default      -> sender.sendMessage(plugin.getLangManager().getMessage("unknown-command"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        var lang = plugin.getLangManager();
        sender.sendMessage(lang.getMessage("help.header"));
        sender.sendMessage(lang.getMessage("help.title"));
        sender.sendMessage(lang.getMessage("help.spacer"));
        sender.sendMessage(lang.getMessage("help.cmd-reload"));
        sender.sendMessage(lang.getMessage("help.cmd-help"));
        sender.sendMessage(lang.getMessage("help.cmd-vanish"));
        sender.sendMessage(lang.getMessage("help.footer"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return Arrays.asList("reload", "help");
        return List.of();
    }
}
