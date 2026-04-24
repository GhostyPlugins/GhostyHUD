package de.gergh0stface.ghostyhud.commands;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.managers.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /vanish [player]
 *
 * Permissions:
 *   ghostyhud.vanish          → vanish yourself
 *   ghostyhud.vanish.others   → vanish / reveal other players
 *   ghostyhud.vanish.level.<n>→ your vanish level
 *   ghostyhud.vanish.seeall   → see all vanished players regardless of level
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    private final GhostyHUD plugin;

    public VanishCommand(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {

        VanishManager vm = plugin.getVanishManager();

        // ── vanish <other player> ─────────────────────────────────────────────
        if (args.length >= 1) {
            if (!sender.hasPermission("ghostyhud.vanish.others")) {
                sender.sendMessage(plugin.getLangManager().getMessage("no-permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getLangManager().getMessage("vanish.player-not-found"));
                return true;
            }
            toggleVanish(sender, target, vm);
            return true;
        }

        // ── vanish self ───────────────────────────────────────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangManager().getMessage("player-only"));
            return true;
        }
        if (!player.hasPermission("ghostyhud.vanish")) {
            sender.sendMessage(plugin.getLangManager().getMessage("no-permission"));
            return true;
        }

        toggleVanish(sender, player, vm);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void toggleVanish(CommandSender executor, Player target, VanishManager vm) {
        boolean isSelf = (executor instanceof Player p) && p.equals(target);
        boolean nowVanished = vm.toggle(target);

        if (nowVanished) {
            int level = vm.getVanishLevel(target);
            String levelName = getLevelName(level);

            // Notify the target
            target.sendMessage(plugin.getLangManager()
                    .getMessage("vanish.vanished", target)
                    .replace("{level}", String.valueOf(level))
                    .replace("{level_name}", levelName));

            // Notify executor if it's someone else
            if (!isSelf) {
                executor.sendMessage(plugin.getLangManager()
                        .getMessage("vanish.vanished-other")
                        .replace("{target}", target.getName())
                        .replace("{level}", String.valueOf(level))
                        .replace("{level_name}", levelName));
            }
        } else {
            // Notify target
            target.sendMessage(plugin.getLangManager().getMessage("vanish.unvanished", target));

            // Notify executor if it's someone else
            if (!isSelf) {
                executor.sendMessage(plugin.getLangManager()
                        .getMessage("vanish.unvanished-other")
                        .replace("{target}", target.getName()));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable level name from config, e.g. "Supporter".
     * Falls back to "Level &lt;n&gt;" if not configured.
     */
    private String getLevelName(int level) {
        String key = "vanish.level-names.level-" + level;
        String name = plugin.getConfig().getString(key, null);
        return name != null ? name : "Level " + level;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command cmd,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("ghostyhud.vanish.others")) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
