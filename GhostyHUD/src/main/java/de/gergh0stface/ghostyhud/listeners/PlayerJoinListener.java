package de.gergh0stface.ghostyhud.listeners;

import de.gergh0stface.ghostyhud.GhostyHUD;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final GhostyHUD plugin;

    public PlayerJoinListener(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean firstJoin = !player.hasPlayedBefore();

        // Database
        plugin.getDatabaseManager().loadPlayer(player);

        // Scoreboard
        plugin.getScoreboardManager().setupPlayer(player);

        // BossBar
        plugin.getBossBarManager().addPlayer(player);

        // TabList
        plugin.getTabListManager().updatePlayer(player);

        // ActionBar
        plugin.getActionBarManager().sendTo(player);

        // Vanish: apply existing vanish states (re-hide staff from this player / re-hide this player if they were vanished)
        plugin.getVanishManager().applyOnJoin(player);

        // Join message
        String msgKey = firstJoin ? "join.first-join" : "join.welcome";
        player.sendMessage(plugin.getLangManager().getMessage(msgKey, player));
    }
}
