package de.gergh0stface.ghostyhud.listeners;

import de.gergh0stface.ghostyhud.GhostyHUD;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final GhostyHUD plugin;

    public PlayerQuitListener(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getScoreboardManager().removePlayer(player);
        plugin.getBossBarManager().removePlayer(player);
        plugin.getTabListManager().clearPlayer(player);
        plugin.getVanishManager().onQuit(player);
    }
}
