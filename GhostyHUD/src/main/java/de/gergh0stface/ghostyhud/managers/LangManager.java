package de.gergh0stface.ghostyhud.managers;

import de.gergh0stface.ghostyhud.GhostyHUD;
import de.gergh0stface.ghostyhud.utils.ColorUtils;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages language / localisation files.
 * Falls back gracefully: configured lang → en_US → hard-coded fallback.
 */
public class LangManager {

    private final GhostyHUD plugin;
    private FileConfiguration lang;
    private String currentLanguage;

    public LangManager(GhostyHUD plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        currentLanguage = plugin.getConfig().getString("language", "en_US");

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        saveDefaultLang("en_US");
        saveDefaultLang("de_DE");

        File langFile = new File(langDir, currentLanguage + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + currentLanguage
                    + ".yml' not found – falling back to en_US.");
            langFile = new File(langDir, "en_US.yml");
            currentLanguage = "en_US";
        }

        lang = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource("lang/" + currentLanguage + ".yml");
        if (defaultStream == null) defaultStream = plugin.getResource("lang/en_US.yml");
        if (defaultStream != null) {
            lang.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
        }
    }

    private void saveDefaultLang(String locale) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("lang/" + locale + ".yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save lang/" + locale + ".yml: " + e.getMessage());
            }
        }
    }

    public void reload() {
        load();
    }

    public String getMessage(String path, Player player) {
        String raw = lang.getString("messages." + path,
                "&cMessage key not found: messages." + path);
        String prefix = lang.getString("prefix", "&5GhostyHUD &8» &r");
        raw = raw.replace("{prefix}", prefix);
        if (player != null) raw = PlaceholderUtils.replace(raw, player);
        return ColorUtils.colorize(raw);
    }

    public String getMessage(String path) {
        return getMessage(path, null);
    }

    public String getRawPrefix() {
        return lang.getString("prefix", "&5GhostyHUD &8» &r");
    }

    public String getCurrentLanguage() { return currentLanguage; }
    public FileConfiguration getConfig() { return lang; }
}
