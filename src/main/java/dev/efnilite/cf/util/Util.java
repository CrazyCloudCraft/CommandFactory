package dev.efnilite.cf.util;

import dev.efnilite.vilib.util.Strings;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Util {

    private static final char[] RANDOM_DIGITS = "1234567890".toCharArray();

    /**
     * Random digits
     *
     * @return a string with an amount of random digits
     */
    public static String randomDigits(int amount) {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            random.append(RANDOM_DIGITS[ThreadLocalRandom.current().nextInt(RANDOM_DIGITS.length - 1)]);
        }
        return random.toString();
    }

    /**
     * Gets the size of a ConfigurationSection
     *
     * @param file The file
     * @param path The path
     */
    public static @Nullable List<String> getNode(FileConfiguration file, String path) {
        ConfigurationSection section = file.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        return new ArrayList<>(section.getKeys(false));
    }

    public static void send(CommandSender sender, String msg) {
        sender.sendMessage(Strings.colour(msg));
    }

    public static String formatDuration(long millis) {
        return Duration.ofMillis(millis)
                .toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}