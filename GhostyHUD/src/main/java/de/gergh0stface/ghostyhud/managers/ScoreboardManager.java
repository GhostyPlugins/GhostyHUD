package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.utils.ColorUtils;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Per-player sidebar scoreboard.
 *
 * Uses the Adventure Component API for Team prefix/suffix (Paper 1.21).
 * Each player gets their own Scoreboard instance so placeholders are individual.
 * Supports up to 16 lines (Minecraft sidebar maximum).
 */
public class ScoreboardManager {

    private final GhostyHUD plugin;
    private BukkitTask task;

    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    /**
     * Invisible §-coded entries – one per scoreboard line slot.
     * Players never see these strings; only the Team prefix is shown.
     */
    private static final String[] ENTRIES = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };

    public ScoreboardManager(GhostyHUD plugin) {
        this.plugin = plugin;
    }

    // ── Task lifecycle ────────────────────────────────────────────────────────

    public void startTask() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        int interval = plugin.getConfig().getInt("scoreboard.update-interval", 40);
        task = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayer), 0L, interval);
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) { task.cancel(); task = null; }
    }

    // ── Setup / removal ───────────────────────────────────────────────────────

    public void setupPlayer(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        org.bukkit.scoreboard.ScoreboardManager bm =
                Objects.requireNonNull(Bukkit.getScoreboardManager());
        Scoreboard board = bm.getNewScoreboard();

        // Title (Component via Adventure)
        String rawTitle = plugin.getConfig().getString("scoreboard.title", "&5GhostyHUD");
        Component title = ColorUtils.toComponent(PlaceholderUtils.replace(rawTitle, player));

        @SuppressWarnings("deprecation") // String-based criterion still required on Spigot
        Objective obj = board.registerNewObjective("ghostyhud", "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int count = Math.min(lines.size(), ENTRIES.length);

        for (int i = 0; i < count; i++) {
            Team team = board.registerNewTeam("line_" + i);
            team.addEntry(ENTRIES[i]);
            Score score = obj.getScore(ENTRIES[i]);
            score.setScore(count - i);
            // Hide the number on the right side of each scoreboard line
            score.numberFormat(NumberFormat.blank());
        }

        player.setScoreboard(board);
        playerBoards.put(player.getUniqueId(), board);
        applyLines(player, board, lines);
    }

    public void removePlayer(Player player) {
        playerBoards.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.setScoreboard(
                    Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) { setupPlayer(player); return; }

        Objective obj = board.getObjective("ghostyhud");
        if (obj != null) {
            String rawTitle = plugin.getConfig().getString("scoreboard.title", "&5GhostyHUD");
            obj.displayName(ColorUtils.toComponent(PlaceholderUtils.replace(rawTitle, player)));
        }

        applyLines(player, board, plugin.getConfig().getStringList("scoreboard.lines"));
    }

    // ── Line rendering ────────────────────────────────────────────────────────

    /**
     * Sets each Team's prefix (and optionally suffix) using Adventure Components.
     *
     * In 1.21 the Component-based Team API has no hard character limit beyond
     * normal chat component size, so we no longer need to split at 64 chars.
     * We still split very long lines into prefix + suffix for safety.
     */
    private void applyLines(Player player, Scoreboard board, List<String> lines) {
        int count = Math.min(lines.size(), ENTRIES.length);
        for (int i = 0; i < count; i++) {
            Team team = board.getTeam("line_" + i);
            if (team == null) continue;

            String raw   = PlaceholderUtils.replace(lines.get(i), player);
            String full  = ColorUtils.colorize(raw);          // §-coded string

            // Split at 128 §-coded chars so the legacy sidebar doesn't clip.
            // With Adventure component API, prefix/suffix accept components, not raw §-strings.
            if (full.length() <= 128) {
                team.prefix(ColorUtils.toComponent(raw));
                team.suffix(Component.empty());
            } else {
                // Cut the raw string (before colorize) proportionally
                int cutRaw = (int) (raw.length() * (128.0 / full.length()));
                String prefixRaw = raw.substring(0, Math.min(cutRaw, raw.length()));
                String suffixRaw = raw.substring(Math.min(cutRaw, raw.length()));
                team.prefix(ColorUtils.toComponent(prefixRaw));
                team.suffix(ColorUtils.toComponent(suffixRaw));
            }
        }
    }
}
