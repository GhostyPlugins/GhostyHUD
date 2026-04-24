package de.gergh0stface.ghostyhud.utils;

import de.gergh0stface.ghostyhud.GhostyHUD;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Replaces all GhostyHUD placeholder tokens in a string.
 *
 * ─────────────────────────────────────────────────────────────
 *  PLAYER PLACEHOLDERS
 * ─────────────────────────────────────────────────────────────
 *  {player}        Player name
 *  {displayname}   Display name
 *  {uuid}          Player UUID
 *  {prefix}        LuckPerms prefix
 *  {suffix}        LuckPerms suffix
 *  {rank}          Primary group (capitalised)
 *  {group}         Primary group (raw)
 *  {ping}          Ping in ms
 *  {world}         World name
 *  {health}        Health (1 decimal)
 *  {max_health}    Max health (1 decimal)
 *  {food}          Food level (0–20)
 *  {armor}         Armor value (0–20)
 *  {level}         XP level
 *  {exp}           XP progress (0.00–1.00)
 *  {gamemode}      SURVIVAL / CREATIVE / ADVENTURE / SPECTATOR
 *  {gamemode_short}S / C / A / SP
 *  {x} {y} {z}    Block coordinates
 *  {biome}         Biome name
 *  {vanished}      true / false
 *  {vanish_level}  Numeric vanish level (0 = none)
 *
 * ─────────────────────────────────────────────────────────────
 *  SERVER PLACEHOLDERS
 * ─────────────────────────────────────────────────────────────
 *  {online}        Visible player count (excl. vanished)
 *  {online_real}   True online count (incl. vanished)
 *  {max}           Max player slots
 *  {tps}           Server TPS (1 decimal, max 20)
 *  {server}        Server name
 *  {version}       Server version string (e.g. 1.21.1)
 *  {ram_used}      Used JVM memory in MB
 *  {ram_max}       Max JVM memory in MB
 *  {vanished_count}Number of currently vanished players
 *
 * ─────────────────────────────────────────────────────────────
 *  DATE / TIME PLACEHOLDERS
 * ─────────────────────────────────────────────────────────────
 *  {date}          dd.MM.yyyy
 *  {date_short}    dd.MM.yy
 *  {time}          HH:mm
 *  {time_full}     HH:mm:ss
 *  {day}           Day of week (e.g. Monday)
 */
public final class PlaceholderUtils {

    private static LuckPerms luckPerms   = null;
    private static boolean   lpChecked   = false;
    private static boolean   lpAvailable = false;

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private static final DateTimeFormatter DATE_FMT       = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_SHORT_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DateTimeFormatter TIME_FMT       = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FULL_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DAY_FMT        = DateTimeFormatter.ofPattern("EEEE");

    private PlaceholderUtils() {}

    // ── LuckPerms lazy init ───────────────────────────────────────────────────

    private static LuckPerms luckPerms() {
        if (!lpChecked) {
            lpChecked = true;
            try {
                RegisteredServiceProvider<LuckPerms> rsp =
                        Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (rsp != null) {
                    luckPerms   = rsp.getProvider();
                    lpAvailable = true;
                }
            } catch (Throwable t) {
                // Catches both Exception AND NoClassDefFoundError / NoSuchMethodError
                // which occur when LuckPerms is not installed on the server.
                GhostyHUD.getInstance().getLogger()
                        .warning("[GhostyHUD] LuckPerms not found – rank placeholders disabled.");
            }
        }
        return luckPerms;
    }

    public static void reset() {
        luckPerms   = null;
        lpChecked   = false;
        lpAvailable = false;
    }

    // ── Main replacement ──────────────────────────────────────────────────────

    public static String replace(String text, Player player) {
        if (text == null) return "";

        if (player != null) {

            // Identity
            text = text.replace("{player}",      player.getName());
            // displayName() is the Adventure API equivalent of the deprecated getDisplayName()
            text = text.replace("{displayname}", LEGACY.serialize(player.displayName()));
            text = text.replace("{uuid}",        player.getUniqueId().toString());

            // Connection
            text = text.replace("{ping}", String.valueOf(player.getPing()));

            // World / position
            text = text.replace("{world}", player.getWorld().getName());
            text = text.replace("{x}",     String.valueOf(player.getLocation().getBlockX()));
            text = text.replace("{y}",     String.valueOf(player.getLocation().getBlockY()));
            text = text.replace("{z}",     String.valueOf(player.getLocation().getBlockZ()));
            text = text.replace("{biome}", biomeName(player));

            // Health & vitals
            text = text.replace("{health}", String.format("%.1f", player.getHealth()));
            text = text.replace("{max_health}", String.format("%.1f", attrValue(player, Attribute.GENERIC_MAX_HEALTH, 20.0)));
            text = text.replace("{food}",   String.valueOf(player.getFoodLevel()));
            text = text.replace("{armor}",  String.valueOf((int) attrValue(player, Attribute.GENERIC_ARMOR, 0.0)));
            text = text.replace("{level}",  String.valueOf(player.getLevel()));
            text = text.replace("{exp}",    String.format("%.2f", player.getExp()));

            // Gamemode
            String gmShort = switch (player.getGameMode()) {
                case SURVIVAL  -> "S";
                case CREATIVE  -> "C";
                case ADVENTURE -> "A";
                case SPECTATOR -> "SP";
            };
            text = text.replace("{gamemode}",       player.getGameMode().name());
            text = text.replace("{gamemode_short}",  gmShort);

            // Vanish
            boolean isVanished = GhostyHUD.getInstance().getVanishManager().isVanished(player);
            int     vLevel     = GhostyHUD.getInstance().getVanishManager().getVanishLevel(player);
            text = text.replace("{vanished}",     String.valueOf(isVanished));
            text = text.replace("{vanish_level}", String.valueOf(vLevel));

            // LuckPerms
            text = replaceLuckPerms(text, player);
        }

        // ── Server-wide ───────────────────────────────────────────────────────
        long vanishedCount = GhostyHUD.getInstance().getVanishManager().getVanishedCount();
        long realOnline    = Bukkit.getOnlinePlayers().size();
        long visibleOnline = Math.max(0, realOnline - vanishedCount);

        text = text.replace("{online}",         String.valueOf(visibleOnline));
        text = text.replace("{online_real}",    String.valueOf(realOnline));
        text = text.replace("{max}",            String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("{tps}",            String.format("%.1f", tps()));
        text = text.replace("{server}",         Bukkit.getServer().getName());
        text = text.replace("{version}",        serverVersion());
        text = text.replace("{ram_used}",       String.valueOf(ramUsed()));
        text = text.replace("{ram_max}",        String.valueOf(ramMax()));
        text = text.replace("{vanished_count}", String.valueOf(vanishedCount));

        // ── Date / Time ───────────────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        text = text.replace("{date}",       now.format(DATE_FMT));
        text = text.replace("{date_short}", now.format(DATE_SHORT_FMT));
        text = text.replace("{time}",       now.format(TIME_FMT));
        text = text.replace("{time_full}",  now.format(TIME_FULL_FMT));
        text = text.replace("{day}",        now.format(DAY_FMT));

        return text;
    }

    // ── LuckPerms helper ──────────────────────────────────────────────────────

    private static String replaceLuckPerms(String text, Player player) {
        LuckPerms lp = luckPerms();
        if (!lpAvailable || lp == null) return defaultLp(text);

        try {
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return defaultLp(text);

            QueryOptions opts;
            try {
                opts = lp.getContextManager().getQueryOptions(player);
            } catch (Throwable e) {
                opts = QueryOptions.nonContextual();
            }

            String prefix = user.getCachedData().getMetaData(opts).getPrefix();
            String suffix = user.getCachedData().getMetaData(opts).getSuffix();
            String rank   = user.getPrimaryGroup();

            text = text.replace("{prefix}", prefix != null ? prefix : "");
            text = text.replace("{suffix}", suffix != null ? suffix : "");
            text = text.replace("{rank}",   rank   != null ? capitalise(rank) : "Default");
            text = text.replace("{group}",  rank   != null ? rank             : "default");
        } catch (Throwable e) {
            return defaultLp(text);
        }
        return text;
    }

    // ── Attribute helper ──────────────────────────────────────────────────────

    /**
     * Safely reads a numeric attribute value.
     * Falls back to {@code def} if the attribute is not present.
     */
    private static double attrValue(Player player, Attribute attribute, double def) {
        try {
            AttributeInstance inst = player.getAttribute(attribute);
            return inst != null ? inst.getValue() : def;
        } catch (Exception e) {
            return def;
        }
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private static String defaultLp(String text) {
        return text
                .replace("{prefix}", "")
                .replace("{suffix}", "")
                .replace("{rank}",   "Default")
                .replace("{group}",  "default");
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static double tps() {
        try {
            return Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        } catch (Exception e) {
            return 20.0;
        }
    }

    private static String serverVersion() {
        String ver = Bukkit.getVersion();
        int start = ver.indexOf("MC: ");
        if (start != -1) {
            int end = ver.indexOf(')', start);
            if (end != -1) return ver.substring(start + 4, end).trim();
        }
        return ver;
    }

    private static long ramUsed() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    private static long ramMax() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    private static String biomeName(Player player) {
        try {
            String raw = player.getLocation().getBlock().getBiome().name();
            String[] parts = raw.split("_");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0)));
                sb.append(p.substring(1).toLowerCase());
            }
            return sb.toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
