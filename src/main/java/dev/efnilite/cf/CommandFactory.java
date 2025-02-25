package dev.efnilite.cf;

import dev.efnilite.cf.command.CommandProcessor;
import dev.efnilite.cf.legacy.LegacyCommandsReader;
import dev.efnilite.cf.util.config.Configuration;
import dev.efnilite.cf.util.config.Option;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.Version;
import dev.efnilite.vilib.util.elevator.GitElevator;
import dev.efnilite.vilib.util.elevator.VersionComparator;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Main class
 *
 * @author Efnilite
 * (c) MMXXI - MMXXIII
 */
public final class CommandFactory extends ViPlugin {

    private static GitElevator elevator;
    private static CommandFactory instance;
    private static CommandProcessor processor;
    private static Configuration configuration;
    public static final String NAME = "<gradient:#7F00FF:#007FFF>CommandFactory</gradient>";
    public static final String MESSAGE_PREFIX = NAME + " <#7B7B7B>» <gray>";
    public static final String REQUIRED_VILIB_VERSION = "1.1.0";

    @Override
    public void enable() {
        Time.timerStart("enableCF");

        Plugin vilib = getServer().getPluginManager().getPlugin("vilib");
        if (vilib == null || !vilib.isEnabled()) {
            getLogger().severe("##");
            getLogger().severe("## Infinite Parkour requires vilib to work!");
            getLogger().severe("##");
            getLogger().severe("## Please download it here:");
            getLogger().severe("## https://github.com/Efnilite/vilib/releases/latest");
            getLogger().severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!VersionComparator.FROM_SEMANTIC.isLatest(REQUIRED_VILIB_VERSION, vilib.getDescription().getVersion())) {
            getLogger().severe("##");
            getLogger().severe("## Infinite Parkour requires *a newer version* of vilib to work!");
            getLogger().severe("##");
            getLogger().severe("## Please download it here:");
            getLogger().severe("## https://github.com/Efnilite/vilib/releases/latest");
            getLogger().severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        logging.info("Registered under version " + Version.getPrettyVersion());
        instance = this;
        configuration = new Configuration(this);
        Option.init();

        processor = new CommandProcessor();

        registerCommand("commandfactory", new MainCommand());

        Metrics metrics = new Metrics(this, 14168);
        metrics.addCustomChart(new SingleLineChart("total_alias_count", () -> processor.getMappedSize()));

        LegacyCommandsReader.check();

        elevator = new GitElevator("Efnilite/CommandFactory", this, VersionComparator.FROM_SEMANTIC, Option.AUTO_UPDATER);

        logging.info("Loaded CommandFactory in " + Time.timerEnd("enableCF") + "ms");
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public @Nullable GitElevator getElevator() {
        return null;
    }

    /**
     * Returns the {@link Logging} belonging to this plugin.
     *
     * @return this plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return getPlugin().logging;
    }

    /**
     * Returns this plugin instance.
     *
     * @return the plugin instance.
     */
    public static CommandFactory getPlugin() {
        return instance;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static CommandProcessor getProcessor() {
        return processor;
    }
}
