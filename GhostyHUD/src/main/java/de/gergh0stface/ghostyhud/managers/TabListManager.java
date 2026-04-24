package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.utils.ColorUtils;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Manages the player list (Tab) header and footer.
 * Uses the Adventure API – sendPlayerListHeaderAndFooter(Component, Component).
 */
public class TabListManager {

    private final GhostyHUD plugin;
    private BukkitTask task;

    public TabListManager(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) return;
        int interval = plugin.getConfig().getInt("tablist.update-interval", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayer), 0L, interval);
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) { task.cancel(); task = null; }
    }

    public void updatePlayer(Player viewer) {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) return;

        Component header = buildMultiline(
                plugin.getConfig().getStringList("tablist.header"), viewer);
        Component footer = buildMultiline(
                plugin.getConfig().getStringList("tablist.footer"), viewer);

        // Adventure API – not deprecated in Paper 1.21
        viewer.sendPlayerListHeaderAndFooter(header, footer);
    }

    public void clearPlayer(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Component buildMultiline(List<String> lines, Player viewer) {
        if (lines.isEmpty()) return Component.empty();

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            String processed = PlaceholderUtils.replace(lines.get(i), viewer);
            result = result.append(ColorUtils.toComponent(processed));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }
}
