package de.gergh0stface.ghostyhud.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for colorizing strings.
 * Supports:
 *   - Legacy & color codes  (&a, &c, &l …)
 *   - Hex color codes        (&#RRGGBB and #RRGGBB)
 *
 * Use {@link #colorize(String)} to get a §-coded String.
 * Use {@link #toComponent(String)} to get an Adventure Component (for modern Paper API).
 */
public final class ColorUtils {

    private static final Pattern HEX_PATTERN =
            Pattern.compile("&#([A-Fa-f0-9]{6})|(?<![&§])#([A-Fa-f0-9]{6})");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private ColorUtils() {}

    // ── String (§-codes) ──────────────────────────────────────────────────────

    /**
     * Translates & and hex colour codes → §-coded String.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Hex codes first
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            try {
                String code = ChatColor.of("#" + hex).toString();
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(code));
            } catch (IllegalArgumentException ignored) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(buffer);

        // 2. Legacy &x codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Strips all colour / formatting codes.
     */
    public static String strip(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    // ── Adventure Component ───────────────────────────────────────────────────

    /**
     * Converts a raw string (with & and/or hex codes) directly into an
     * Adventure {@link Component}. Use this for all modern Paper API calls
     * (sendActionBar, sendPlayerListHeader, team prefix/suffix, …).
     */
    public static Component toComponent(String text) {
        return LEGACY.deserialize(colorize(text));
    }

    /**
     * Serialises an Adventure Component back to a §-coded String.
     */
    public static String fromComponent(Component component) {
        return LEGACY.serialize(component);
    }
}
