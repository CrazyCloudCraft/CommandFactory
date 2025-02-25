package dev.efnilite.cf;

import dev.efnilite.cf.command.Executor;
import dev.efnilite.cf.command.RegisterNotification;
import dev.efnilite.cf.command.wrapper.AliasedCommand;
import dev.efnilite.cf.util.ChatAnswer;
import dev.efnilite.cf.util.Util;
import dev.efnilite.vilib.inventory.Menu;
import dev.efnilite.vilib.inventory.PagedMenu;
import dev.efnilite.vilib.inventory.animation.*;
import dev.efnilite.vilib.inventory.item.Item;
import dev.efnilite.vilib.inventory.item.MenuItem;
import dev.efnilite.vilib.inventory.item.SliderItem;
import dev.efnilite.vilib.inventory.item.TimedItem;
import dev.efnilite.vilib.util.Time;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Menu handling
 *
 * @author Efnilite
 */
@ApiStatus.Internal
public class FactoryMenu {

    public static void openMain(Player player) {
        PagedMenu mainMenu = new PagedMenu(4, "<white>Commands");

        List<MenuItem> commands = new ArrayList<>();
        for (String alias : CommandFactory.getProcessor().getAliases()) {
            commands.add(new Item(Material.WRITABLE_BOOK, "<#1F85DE><bold>" + alias)
                    .lore("<gray>Click to edit this alias.")
                    .click((event) -> openEditor(player, alias)));
        }

        mainMenu.
                displayRows(0, 1)
                .addToDisplay(commands)

                .nextPage(35, new Item(Material.LIME_DYE, "<#0DCB07><bold>»") // next page
                        .click((event) -> mainMenu.page(1)))
                .prevPage(27, new Item(Material.RED_DYE, "<#DE1F1F><bold>«") // previous page
                        .click((event) -> mainMenu.page(-1)))

                .item(30, new Item(Material.PAPER, "<#2055B8><bold>New command")
                        .lore("<gray>Create a new command.")
                        .click((event) -> initNew(player)))

                .item(31, new Item(Material.NOTE_BLOCK, "<#5F9DAD><bold>Settings")
                        .lore("<gray>Open the settings menu.")
                        .click((event) -> openSettings(player)))

                .item(32, new Item(Material.ARROW, "<red><bold>Close").click((event) -> player.closeInventory()))

                .fillBackground(Material.GRAY_STAINED_GLASS_PANE)
                .animation(new SnakeSingleAnimation())
                .open(player);
    }

    public static void initNew(Player player) {
        new ChatAnswer(player, "cancel")
                .pre((pl) -> {
                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Please enter the alias. " +
                            "This is the other (usually shorter) version of the main command. Example: /gmc (with main command /gamemode creative). " +
                            "Type 'cancel' to cancel.");
                    pl.closeInventory();
                })
                .post((pl, alias) -> new ChatAnswer(player, "cancel")
                        .pre((pl1) -> {
                            Util.send(pl1, CommandFactory.MESSAGE_PREFIX + "Please enter the main command. " +
                                    "This is the command that actually gets executed when you enter the alias. Example: /gamemode creative (with alias /gmc). " +
                                    "Type 'cancel' to cancel.");
                            pl1.closeInventory();
                        })
                        .post((pl1, main) -> {
                            RegisterNotification notification = CommandFactory.getProcessor().register(alias, main);

                            if (notification != null) {
                                switch (notification) {
                                    case ARGUMENT_NULL:
                                        Util.send(pl, CommandFactory.MESSAGE_PREFIX + "<red>You entered a value which is null!");
                                        return;
                                    case ALIAS_ALREADY_EXISTS:
                                        Util.send(pl, CommandFactory.MESSAGE_PREFIX + "<red>That alias already exists!");
                                        return;
                                }
                            }
                            openEditor(pl1, alias);
                        })
                        .cancel((pl1) -> Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Cancelled registering your command.")))
                .cancel((pl) -> Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Cancelled registering your command."));
    }

    public static void openEditor(Player player, String alias) {
        AliasedCommand command = CommandFactory.getProcessor().get(alias);

        if (command == null) {
            return;
        }

        Menu editor = new Menu(4, "<white>Editing " + alias);

        RegisterNotification notification = command.getNotification();
        if (notification != null) {
            if (notification == RegisterNotification.OVERRIDING_EXISTING) {
                editor
                        .item(31, new Item(Material.REDSTONE_TORCH, "<#B61616><bold>Warning!")
                                .lore("<gray>This command overrides another command.",
                                        "<gray>This may cause issues with the server,",
                                        "<gray>or the plugin that owns this command.", "",
                                        "<gray>Please <#CB7575><bold>don't report problems or errors<gray>",
                                        "<gray>with this command if you see this warning."));
            }
        }

        editor
                .distributeRowEvenly(1)
                .item(9, new Item(Material.COMMAND_BLOCK, "<#91AEE2><bold>Main command")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(command.getMainCommand()),
                                "<gray>This is the other (usually shorter) version of the main command.",
                                "<gray>Example: /gmc (with main command /gamemode creative)",
                                "<gray>Set the main command by typing it.")
                        .click((event) -> new ChatAnswer(player, "cancel")
                                .pre((pl) -> {
                                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "<gray>Please enter a command. Type 'cancel' to cancel.");
                                    pl.closeInventory();
                                })
                                .post((pl, msg) -> {
                                    CommandFactory.getProcessor().editMainCommand(alias, msg);
                                    openEditor(pl, alias);
                                })
                                .cancel((pl) -> openEditor(pl, alias))))

                .item(10, new Item(Material.NAME_TAG, "<#91AEE2><bold>Permission")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(command.getPermission()),
                                "<gray>Set the permission to execute this command by typing it.")
                        .click((event) -> new ChatAnswer(player, "cancel")
                                .pre((pl) -> {
                                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "<gray>Please enter a permission. Type 'cancel' to cancel.");
                                    pl.closeInventory();
                                })
                                .post((pl, msg) -> {
                                    CommandFactory.getProcessor().editPermission(alias, msg);
                                    openEditor(pl, alias);
                                })
                                .cancel((pl) -> openEditor(pl, alias))))

                .item(11, new Item(Material.IRON_HORSE_ARMOR, "<#91AEE2><bold>Permission message")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(command.getPermissionMessage()),
                                "<gray>Set the permission message by typing it.",
                                "<gray>This is the message players get when",
                                "<gray>they don't have enough permissions.")
                        .click((event) -> new ChatAnswer(player, "cancel")
                                .pre((pl) -> {
                                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Please enter a permission message. " +
                                            "Use '<name>' for colours. Hex colours are supported ('<#abcde>'). Type 'cancel' to cancel.");
                                    pl.closeInventory();
                                })
                                .post((pl, msg) -> {
                                    CommandFactory.getProcessor().editPermissionMessage(alias, msg);
                                    openEditor(pl, alias);
                                })
                                .cancel((pl) -> openEditor(pl, alias))))

                .item(12, new Item(Material.CLOCK, "<#91AEE2><bold>Cooldown")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(Util.formatDuration(command.getCooldownMs())), "<gray>Set the cooldown.")
                        .click((event) -> openCooldown(player, alias)))

                .item(13, new Item(Material.GOLDEN_HORSE_ARMOR, "<#91AEE2><bold>Cooldown message")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(command.getCooldownMessage()), "<gray>Set the cooldown message by typing it.",
                                "<gray>This is the message players get when", "<gray>they still have a cooldown.")
                        .click((event) -> new ChatAnswer(player, "cancel")
                                .pre((pl) -> {
                                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Please enter a cooldown message. " +
                                            "Use '<name>' for colours. Hex colours are supported ('<#abcde>'). Use '%time%' for the remaining time." +
                                            "Use '%cooldown%' for the total cooldown time. Type 'cancel' to cancel.");
                                    pl.closeInventory();
                                })
                                .post((pl, msg) -> {
                                    CommandFactory.getProcessor().editCooldownMessage(alias, msg);
                                    openEditor(pl, alias);
                                })
                                .cancel((pl) -> openEditor(pl, alias))))

                .item(14, new Item(Material.PLAYER_HEAD, "<#91AEE2><bold>Executor")
                        .lore("<#7285A9>Currently<gray>: " + command.getExecutableBy().name().toLowerCase(), "<gray>Set who can execute this command.")
                        .click((event) -> openExecutor(player, alias)))

                .item(15, new Item(Material.DIAMOND_HORSE_ARMOR, "<#91AEE2><bold>Executor message")
                        .lore("<#7285A9>Currently<gray>: " + orNothing(command.getExecutableByMessage()), "<gray>Set the executor message by typing it.",
                                "<gray>This is the message players/console get when", "<gray>they can't execute this command.")
                        .click((event) -> new ChatAnswer(player, "cancel")
                                .pre((pl) -> {
                                    Util.send(pl, CommandFactory.MESSAGE_PREFIX + "Please enter an executor message. " +
                                            "Use '<name>'. Hex colours are supported ('<#abcde>'). Type 'cancel' to cancel.");
                                    pl.closeInventory();
                                })
                                .post((pl, msg) -> {
                                    CommandFactory.getProcessor().editExecutableByMessage(alias, msg);
                                    openEditor(pl, alias);
                                })
                                .cancel((pl) -> openEditor(pl, alias))))

                .item(30, new Item(Material.BUCKET, "<#C43131><bold>Delete")
                        .lore("<gray>Deletes this command forever.")
                        .click((event) -> {
                            Menu menu = event.getMenu();
                            menu.item(event.getSlot(), new TimedItem(new Item(Material.BARRIER, "<#980F0F><bold>Are you sure?")
                                    .lore("<gray>If you click this item again,", "<gray>this command will be <underline>permanently deleted</underline><gray>!")
                                    .click((event1) -> {
                                        Util.send(player, CommandFactory.MESSAGE_PREFIX + "Deleted command '" + alias + "'!");
                                        CommandFactory.getProcessor().unregister(alias);
                                        openMain(player);
                                    }), event).stay(5 * 20));
                            menu.updateItem(event.getSlot());
                        }))

                .item(32, new Item(Material.ARROW, "<red><bold>Go back")
                        .lore("<gray>Go back to the main menu")
                        .click((event) -> openMain(player)))
                .animation(new WaveEastAnimation())
                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .open(player);
    }

    private static void openExecutor(Player player, String alias) {
        AliasedCommand command = CommandFactory.getProcessor().get(alias);

        if (command == null) {
            return;
        }

        Executor executor = command.getExecutableBy();
        AtomicBoolean players = new AtomicBoolean(executor == Executor.PLAYER || executor == Executor.BOTH);
        AtomicBoolean console = new AtomicBoolean(executor == Executor.CONSOLE || executor == Executor.BOTH);

        new Menu(3, "<white>Executor of " + alias)
                .distributeRowEvenly(1)

                .item(9, new SliderItem()
                        .initial(players.get() ? 0 : 1)
                        .add(0, new Item(Material.LIME_STAINED_GLASS_PANE, "<green><bold>Players")
                                .lore("<gray>Players can execute this command"), (event) -> {
                            players.set(true);
                            return true;
                        })
                        .add(1, new Item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Players")
                                .lore("<gray>Players can't execute this command"), (event) -> {
                            players.set(false);
                            return true;
                        }))

                .item(10, new SliderItem()
                        .initial(console.get() ? 0 : 1)
                        .add(0, new Item(Material.LIME_STAINED_GLASS_PANE, "<green><bold>Console")
                                .lore("<gray>Console can execute this command"), (event) -> {
                            console.set(true);
                            return true;
                        })
                        .add(1, new Item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Console")
                                .lore("<gray>Console can't execute this command"), (event) -> {
                            console.set(false);
                            return true;
                        }))

                .item(26, new Item(Material.WRITABLE_BOOK, "<#2FBE6A><bold>Save changes")
                        .lore("<gray>Click to confirm.")
                        .click((event) -> {
                            if (players.get() && console.get()) {
                                CommandFactory.getProcessor().editExecutableBy(alias, Executor.BOTH);
                                openEditor(player, alias);
                            } else if (players.get()) {
                                CommandFactory.getProcessor().editExecutableBy(alias, Executor.PLAYER);
                                openEditor(player, alias);
                            } else if (console.get()) {
                                CommandFactory.getProcessor().editExecutableBy(alias, Executor.CONSOLE);
                                openEditor(player, alias);
                            } else {
                                Menu menu = event.getMenu();
                                menu.item(26, new TimedItem(new Item(Material.BARRIER, "<dark_red><bold>Invalid arguments!")
                                        .lore("<gray>You need to have someone be able to execute this command!")
                                        .click((event1) -> {

                                        }), event).stay(5 * 20));
                                menu.updateItem(26);
                            }
                        }))

                .animation(new SplitMiddleInAnimation())
                .fillBackground(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .open(player);
    }

    private static void openCooldown(Player player, String alias) {
        AliasedCommand command = CommandFactory.getProcessor().get(alias);

        if (command == null) {
            return;
        }

        long cooldown = command.getCooldownMs();
        Item save = new Item(Material.WRITABLE_BOOK, "<#2FBE6A><bold>Save changes")
                .lore("<green>Current total<gray>: " + Util.formatDuration(cooldown), "<gray>Click to confirm.");

        // Init days for items
        Duration total = Duration.ofMillis(cooldown); // get the total cooldown in ms
        AtomicInteger days = new AtomicInteger((int) total.toDays());
        total = total.minusDays(days.get()); // remove days
        AtomicInteger hours = new AtomicInteger((int) total.toHours());
        total = total.minusHours(hours.get()); // remove hours
        AtomicInteger mins = new AtomicInteger((int) total.toMinutes());
        total = total.minusMinutes(mins.get()); // remove mins
        AtomicInteger secs = new AtomicInteger((int) total.getSeconds());
        total = total.minusSeconds(secs.get()); // remove secs
        AtomicInteger ms = new AtomicInteger((int) total.toMillis());

        // Init all items
        SliderItem daysItem = new SliderItem().initial(days.get() > -1 && days.get() < 31 ? days.get() : 0);
        SliderItem hoursItem = new SliderItem().initial(hours.get() > -1 && hours.get() < 24 ? hours.get() : 0);
        SliderItem minsItem = new SliderItem().initial(mins.get() > -1 && mins.get() < 60 ? mins.get() : 0);
        SliderItem secsItem = new SliderItem().initial(secs.get() > -1 && secs.get() < 60 ? secs.get() : 0);
        SliderItem msItem = new SliderItem().initial(ms.get() > -1 && ms.get() < 1000 ? (ms.get() / 50) : 0);

        for (int i = 0; i < 31; i++) {
            int finalIndex = i;
            daysItem.add(i, new Item(Material.RED_STAINED_GLASS_PANE, "<dark_red><bold>" + i + " <dark_red>day(s)")
                            .lore("<gray>Use <dark_red>left<gray> or <dark_red>right click<gray> to add or remove days"),
                    (event) -> {
                        days.set(finalIndex);
                        save.lore("<green>Current total<gray>: " + Util.formatDuration(getDuration(days, hours, mins, secs, ms)), "<gray>Click to confirm.");
                        event.getMenu().item(26, save);
                        event.getMenu().updateItem(26);
                        return true;
                    });
        }

        for (int i = 0; i < 24; i++) {
            int finalIndex = i;
            hoursItem.add(i, new Item(Material.ORANGE_STAINED_GLASS_PANE, "<gold><bold>" + i + " <gold>hour(s)")
                            .lore("<gray>Use <gold>left<gray> or <gold>right click<gray> to add or remove hours"),
                    (event) -> {
                        hours.set(finalIndex);
                        save.lore("<green>Current total<gray>: " + Util.formatDuration(getDuration(days, hours, mins, secs, ms)), "<gray>Click to confirm.");
                        event.getMenu().item(26, save);
                        event.getMenu().updateItem(26);
                        return true;
                    });
        }

        for (int i = 0; i < 60; i++) {
            int finalIndex = i;
            minsItem.add(i, new Item(Material.YELLOW_STAINED_GLASS_PANE, "<yellow><bold>" + i + " <yellow>minute(s)")
                            .lore("<gray>Use <yellow>left<gray> or <yellow>right click<gray> to add or remove minutes"),
                    (event) -> {
                        mins.set(finalIndex);
                        save.lore("<green>Current total<gray>: " + Util.formatDuration(getDuration(days, hours, mins, secs, ms)), "<gray>Click to confirm.");
                        event.getMenu().item(26, save);
                        event.getMenu().updateItem(26);
                        return true;
                    });
        }

        for (int i = 0; i < 60; i++) {
            int finalIndex = i;
            secsItem.add(i, new Item(Material.GREEN_STAINED_GLASS_PANE, "<dark_green><bold>" + i + " <dark_green>second(s)")
                    .lore("<gray>Use <dark_green>left<gray> or <dark_green>right click<gray> to add or remove seconds"),
                    (event) -> {
                        secs.set(finalIndex);
                        save.lore("<green>Current total<gray>: " + Util.formatDuration(getDuration(days, hours, mins, secs, ms)), "<gray>Click to confirm.");
                        event.getMenu().item(26, save);
                        event.getMenu().updateItem(26);
                        return true;
                    });
        }

        for (int i = 0; i < 20; i++) {
            int finalIndex = i * 50;
            msItem.add(i, new Item(Material.LIME_STAINED_GLASS_PANE, "<green><bold>" + finalIndex + " <green>millisecond(s)")
                    .lore("<gray>Use <green>left<gray> or <green>right click<gray> to add or remove milliseconds"),
                    (event) -> {
                        ms.set(finalIndex);
                        save.lore("<green>Current total<gray>: " + Util.formatDuration(getDuration(days, hours, mins, secs, ms)), "<gray>Click to confirm.");
                        event.getMenu().item(26, save);
                        event.getMenu().updateItem(26);
                        return true;
                    });
        }

        new Menu(3, "<white>Cooldown of " + alias)
                .item(9, daysItem)
                .item(10, hoursItem)
                .item(11, minsItem)
                .item(12, secsItem)
                .item(13, msItem)
                .item(26, save
                        .click((event) -> {
                            CommandFactory.getProcessor().editCooldown(alias, getDuration(days, hours, mins, secs, ms));
                            openEditor(player, alias);
                        }))
                .distributeRowEvenly(1)
                .fillBackground(Material.CYAN_STAINED_GLASS_PANE)
                .animation(new SplitMiddleOutAnimation())
                .open(player);
    }

    public static void openSettings(Player player) {
        Menu settingsMenu = new Menu(3, "<white>Settings");
        settingsMenu
                .distributeRowEvenly(1)
                .item(10, new Item(Material.CLOCK, "<blue><bold>Reset cooldowns")
                        .lore("<gray>This will reset all active cooldowns.")
                        .click((event) -> {
                            Menu menu = event.getMenu();
                            menu.item(event.getSlot(), new TimedItem(new Item(Material.BARRIER, "<red><bold>Are you sure?")
                                    .lore("<gray>If you click this item again,", "<gray><u> cooldowns</u><gray> will be reset!")
                                    .click((event1) -> {
                                        CommandFactory.getProcessor().resetCooldowns();
                                        Util.send(player, CommandFactory.MESSAGE_PREFIX + "Reset all cooldowns!");
                                    }), event).stay(20 * 5));
                            menu.updateItem(event.getSlot());
                        }))

                .item(11, new Item(Material.COMPARATOR, "<blue><bold>Reload files")
                        .lore("<gray>This will reload all files.")
                        .click((event) -> {
                            Menu menu = event.getMenu();
                            menu.item(event.getSlot(), new TimedItem(new Item(Material.BARRIER, "<red><bold>Are you sure?")
                                    .lore("<gray>If you click this item again,", "<gray>all files will be reloaded!")
                                    .click((event1) -> {
                                        player.closeInventory();
                                        player.performCommand("commandfactory reload");
                                    }), event).stay(20 * 5));
                            menu.updateItem(event.getSlot());
                        }))

                .item(26, new Item(Material.ARROW, "<red><bold>Go back")
                        .lore("<gray>Go back to the main menu")
                        .click((event) -> openMain(player)))
                .fillBackground(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .animation(new RandomAnimation())
                .open(player);
    }

    private static String orNothing(@Nullable String string) {
        if (string == null) {
            return "nothing";
        }
        return string;
    }

    private static long getDuration(AtomicInteger days, AtomicInteger hrs, AtomicInteger mins, AtomicInteger secs, AtomicInteger ms) {
        return days.longValue() * Time.toMillis(Time.SECONDS_PER_DAY) +
                hrs.longValue() * Time.toMillis(Time.SECONDS_PER_HOUR) +
                mins.longValue() * Time.toMillis(Time.SECONDS_PER_MINUTE) +
                Time.toMillis(secs.intValue()) + ms.longValue();
    }
}
