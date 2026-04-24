package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages the level-based vanish system.
 *
 * ──────────────────────────────────────────────────────
 * HOW LEVELS WORK
 * ──────────────────────────────────────────────────────
 * Every staff rank is assigned a vanish level via permissions:
 *
 *   ghostyhud.vanish.level.1   → e.g. Supporter
 *   ghostyhud.vanish.level.2   → e.g. Moderator
 *   ghostyhud.vanish.level.3   → e.g. Admin
 *   ghostyhud.vanish.level.10  → e.g. Owner  (max supported)
 *
 * Visibility rule:
 *   viewer.vanishLevel  >=  target.vanishLevel  → can see the target
 *
 * ghostyhud.vanish          → required to use /vanish at all
 * ghostyhud.vanish.others   → can vanish / reveal other players
 * ghostyhud.vanish.seeall   → bypasses level checks; sees all vanished players
 * ──────────────────────────────────────────────────────
 */
public class VanishManager {

    public static final int MAX_LEVEL = 10;

    private final GhostyHUD plugin;

    /** UUID → vanish level the player is currently invisible at */
    private final Map<UUID, Integer> vanished = new HashMap<>();

    public VanishManager(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean toggle(Player player) {
        if (isVanished(player)) { unvanish(player); return false; }
        vanish(player); return true;
    }

    public void vanish(Player player) {
        int level = getVanishLevel(player);
        vanished.put(player.getUniqueId(), level);

        // Hide from players who lack clearance
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!canSee(other, level)) other.hidePlayer(plugin, player);
        }

        // Hide already-vanished players from the newly-vanished player (if they lack clearance)
        for (Map.Entry<UUID, Integer> entry : vanished.entrySet()) {
            if (entry.getKey().equals(player.getUniqueId())) continue;
            Player other = Bukkit.getPlayer(entry.getKey());
            if (other != null && !canSee(player, entry.getValue())) {
                player.hidePlayer(plugin, other);
            }
        }
    }

    public void unvanish(Player player) {
        vanished.remove(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.showPlayer(plugin, player);
        }
    }

    /**
     * Called on PlayerJoin: applies existing vanish visibility rules for the
     * joining player and any already-vanished players on the server.
     */
    public void applyOnJoin(Player joiningPlayer) {
        for (Map.Entry<UUID, Integer> entry : vanished.entrySet()) {
            Player vanishedPlayer = Bukkit.getPlayer(entry.getKey());
            if (vanishedPlayer == null) continue;
            if (!canSee(joiningPlayer, entry.getValue())) {
                joiningPlayer.hidePlayer(plugin, vanishedPlayer);
            }
        }

        // If the joining player themselves was already marked as vanished (re-login),
        // hide them from those who lack the required clearance level.
        if (isVanished(joiningPlayer)) {
            int level = vanished.get(joiningPlayer.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(joiningPlayer)) continue;
                if (!canSee(other, level)) other.hidePlayer(plugin, joiningPlayer);
            }
        }
    }

    /** Called on PlayerQuit – vanish state persists across reconnects by design. */
    public void onQuit(Player player) {
        // Intentionally NOT removing from map → vanish persists after reconnect.
        // Uncomment the next line if you prefer vanish to reset on logout:
        // vanished.remove(player.getUniqueId());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isVanished(Player player) {
        return vanished.containsKey(player.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return vanished.containsKey(uuid);
    }

    /** Returns the highest vanish level the player holds (0 = no permission). */
    public int getVanishLevel(Player player) {
        for (int i = MAX_LEVEL; i >= 1; i--) {
            if (player.hasPermission("ghostyhud.vanish.level." + i)) return i;
        }
        return 0;
    }

    /** Can {@code viewer} see a player who is vanished at {@code targetLevel}? */
    public boolean canSee(Player viewer, int targetLevel) {
        if (viewer.hasPermission("ghostyhud.vanish.seeall")) return true;
        return getVanishLevel(viewer) >= targetLevel;
    }

    /** Can viewer see the target (considering vanish)? */
    public boolean canSee(Player viewer, Player target) {
        if (!isVanished(target)) return true;
        int targetLevel = vanished.getOrDefault(target.getUniqueId(), 0);
        return canSee(viewer, targetLevel);
    }

    public Map<UUID, Integer> getVanishedMap() { return Collections.unmodifiableMap(vanished); }
    public int getVanishedCount() { return vanished.size(); }
}
