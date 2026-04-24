package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.utils.ColorUtils;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Sends a continuously refreshed Action Bar message to every online player.
 * Uses the Adventure API (Paper 1.21) – no deprecated Spigot/BungeeCord calls.
 */
public class ActionBarManager {

    private final GhostyHUD plugin;
    private BukkitTask task;

    public ActionBarManager(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        if (!plugin.getConfig().getBoolean("actionbar.enabled", true)) return;
        int interval = plugin.getConfig().getInt("actionbar.update-interval", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::sendTo), 0L, interval);
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) { task.cancel(); task = null; }
    }

    public void sendTo(Player player) {
        if (!plugin.getConfig().getBoolean("actionbar.enabled", true)) return;
        String raw = plugin.getConfig().getString("actionbar.message", "&5GhostyHUD");
        // Adventure API: player.sendActionBar(Component) – not deprecated
        player.sendActionBar(ColorUtils.toComponent(PlaceholderUtils.replace(raw, player)));
    }
}
