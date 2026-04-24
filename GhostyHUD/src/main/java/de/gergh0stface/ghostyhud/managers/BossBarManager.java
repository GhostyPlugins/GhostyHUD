package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.utils.ColorUtils;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the custom BossBar.
 * A single shared BossBar is used; players are added / removed on join / quit.
 *
 * Note: Bukkit.createBossBar(String, BarColor, BarStyle) uses a legacy String
 * title, which is the standard cross-platform approach for Spigot + Paper.
 */
public class BossBarManager {

    private final GhostyHUD plugin;
    private BukkitTask task;
    private BossBar bossBar;

    public BossBarManager(GhostyHUD plugin) {
        this.plugin = plugin;
        createBar();
    }

    private void createBar() {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) return;

        BarColor color = parseColor(plugin.getConfig().getString("bossbar.color", "PURPLE"));
        BarStyle style = parseStyle(plugin.getConfig().getString("bossbar.style", "SOLID"));
        double progress = Math.max(0.0, Math.min(1.0,
                plugin.getConfig().getDouble("bossbar.progress", 1.0)));

        bossBar = Bukkit.createBossBar("GhostyHUD", color, style);
        bossBar.setProgress(progress);
        bossBar.setVisible(true);
    }

    public void startTask() {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true) || bossBar == null) return;
        int interval = plugin.getConfig().getInt("bossbar.update-interval", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateTitle, 0L, interval);
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) { task.cancel(); task = null; }
    }

    public void addPlayer(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        if (bossBar != null) bossBar.removePlayer(player);
    }

    private void updateTitle() {
        if (bossBar == null) return;
        Player sample = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        String rawTitle = plugin.getConfig().getString("bossbar.title", "&5GhostyHUD");
        // Bukkit BossBar.setTitle(String) accepts a §-coded string.
        bossBar.setTitle(ColorUtils.colorize(PlaceholderUtils.replace(rawTitle, sample)));
    }

    public void destroy() {
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
    }

    private BarColor parseColor(String raw) {
        try { return BarColor.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bossbar color '" + raw + "', using PURPLE.");
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseStyle(String raw) {
        try { return BarStyle.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bossbar style '" + raw + "', using SOLID.");
            return BarStyle.SOLID;
        }
    }
}
