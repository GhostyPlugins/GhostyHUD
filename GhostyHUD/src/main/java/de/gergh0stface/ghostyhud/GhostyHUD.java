package de.gergh0stface.ghostyhud;

import de.gergh0stface.ghostyhud.commands.ReloadCommand;
import de.gergh0stface.ghostyhud.commands.VanishCommand;
import de.gergh0stface.ghostyhud.listeners.PlayerJoinListener;
import de.gergh0stface.ghostyhud.listeners.PlayerQuitListener;
import de.gergh0stface.ghostyhud.managers.*;
import de.gergh0stface.ghostyhud.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * GhostyHUD – Main plugin class
 * Author: Ger_Gh0stface
 */
public final class GhostyHUD extends JavaPlugin {

    private static GhostyHUD instance;

    private LangManager       langManager;
    private DatabaseManager   databaseManager;
    private TabListManager    tabListManager;
    private ScoreboardManager scoreboardManager;
    private BossBarManager    bossBarManager;
    private ActionBarManager  actionBarManager;
    private VanishManager     vanishManager;

    // ── Plugin name / version (resolved once at startup) ─────────────────────
    private String pluginName;
    private String pluginVersion;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;

        // Resolve name & version without repeated deprecated getDescription() calls
        pluginName    = getName();          // JavaPlugin#getName() – not deprecated
        pluginVersion = getPluginMeta().getVersion(); // Paper 1.20.5+ PluginMeta API

        saveDefaultConfig();

        // Vanish must be first – other managers query it
        vanishManager     = new VanishManager(this);
        langManager       = new LangManager(this);
        databaseManager   = new DatabaseManager(this);
        databaseManager.initialize();

        tabListManager    = new TabListManager(this);
        scoreboardManager = new ScoreboardManager(this);
        bossBarManager    = new BossBarManager(this);
        actionBarManager  = new ActionBarManager(this);

        tabListManager   .startTask();
        scoreboardManager.startTask();
        bossBarManager   .startTask();
        actionBarManager .startTask();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        registerCmd("ghostyhud", new ReloadCommand(this));
        registerCmd("vanish",    new VanishCommand(this));

        // Init already-online players (e.g. after /reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            scoreboardManager.setupPlayer(p);
            bossBarManager.addPlayer(p);
            tabListManager.updatePlayer(p);
        }

        printBanner(true);
    }

    @Override
    public void onDisable() {
        if (tabListManager    != null) tabListManager   .stopTask();
        if (scoreboardManager != null) scoreboardManager.stopTask();
        if (bossBarManager    != null) { bossBarManager.stopTask(); bossBarManager.destroy(); }
        if (actionBarManager  != null) actionBarManager .stopTask();
        if (databaseManager   != null) databaseManager  .close();

        printBanner(false);
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    public void reload() {
        tabListManager   .stopTask();
        scoreboardManager.stopTask();
        bossBarManager   .stopTask();
        bossBarManager   .destroy();
        actionBarManager .stopTask();

        reloadConfig();
        PlaceholderUtils.reset();

        langManager       = new LangManager(this);
        tabListManager    = new TabListManager(this);
        scoreboardManager = new ScoreboardManager(this);
        bossBarManager    = new BossBarManager(this);
        actionBarManager  = new ActionBarManager(this);
        // VanishManager intentionally NOT recreated – preserves active vanish states

        tabListManager   .startTask();
        scoreboardManager.startTask();
        bossBarManager   .startTask();
        actionBarManager .startTask();

        for (Player p : Bukkit.getOnlinePlayers()) {
            scoreboardManager.setupPlayer(p);
            bossBarManager.addPlayer(p);
            tabListManager.updatePlayer(p);
            actionBarManager.sendTo(p);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void registerCmd(String name, Object handler) {
        PluginCommand cmd = Objects.requireNonNull(getCommand(name),
                "Command '" + name + "' missing from plugin.yml!");
        if (handler instanceof CommandExecutor ex) cmd.setExecutor(ex);
        if (handler instanceof TabCompleter    tc) cmd.setTabCompleter(tc);
    }

    private void printBanner(boolean enabling) {
        String state = enabling ? "enabled" : "disabled";
        String line  = "==============================================";
        Bukkit.getConsoleSender().sendMessage(line);
        Bukkit.getConsoleSender().sendMessage(
                "                         " + pluginName + " v" + pluginVersion + " " + state);
        Bukkit.getConsoleSender().sendMessage(
                "                         Author >> Ger_Gh0stface");
        Bukkit.getConsoleSender().sendMessage(line);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static GhostyHUD getInstance()           { return instance; }
    public LangManager       getLangManager()         { return langManager; }
    public DatabaseManager   getDatabaseManager()     { return databaseManager; }
    public TabListManager    getTabListManager()      { return tabListManager; }
    public ScoreboardManager getScoreboardManager()   { return scoreboardManager; }
    public BossBarManager    getBossBarManager()      { return bossBarManager; }
    public ActionBarManager  getActionBarManager()    { return actionBarManager; }
    public VanishManager     getVanishManager()       { return vanishManager; }
}
