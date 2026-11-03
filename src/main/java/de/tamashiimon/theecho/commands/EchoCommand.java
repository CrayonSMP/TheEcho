package de.tamashiimon.theecho.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Root command ({@code /theecho}) that forwards to registered {@link EchoSubCommand}s:
 * {@code /theecho <sub command> <args...>}.
 */
public class EchoCommand implements CommandExecutor, TabCompleter {
    /** Primary names and aliases -> sub command. */
    private final Map<String, EchoSubCommand> byName = new LinkedHashMap<>();
    /** Every sub command once, in registration order (for help and tab completion). */
    private final List<EchoSubCommand> subCommands = new ArrayList<>();

    public EchoCommand register(@NotNull EchoSubCommand... commands) {
        for (EchoSubCommand sub : commands) {
            List<String> keys = new ArrayList<>();
            keys.add(sub.name());
            keys.addAll(sub.aliases());
            for (String key : keys) {
                if (byName.containsKey(key)) {
                    throw new IllegalArgumentException("Sub command name/alias '" + key + "' is already registered");
                }
            }
            for (String key : keys) {
                byName.put(key, sub);
            }
            subCommands.add(sub);
        }
        return this;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        EchoSubCommand sub = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.sendMessage(Component.text("Unknown sub command '" + args[0] + "'. ", NamedTextColor.RED)
                    .append(Component.text("Use /" + label + " for a list.", NamedTextColor.GRAY)));
            return true;
        }
        if (!sub.canUse(sender)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        EchoCommandContext ctx = new EchoCommandContext(sender, label, args[0],
                Arrays.copyOfRange(args, 1, args.length));
        try {
            sub.execute(ctx);
        } catch (Exception e) {
            JavaPlugin.getProvidingPlugin(EchoCommand.class).getLogger().log(Level.SEVERE,
                    "Error while executing /" + label + " " + sub.name(), e);
            sender.sendMessage(Component.text("An error occurred while executing this command (see console).",
                    NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return List.of();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subCommands.stream()
                    .filter(sub -> sub.canUse(sender))
                    .map(EchoSubCommand::name)
                    .filter(name -> name.startsWith(prefix))
                    .toList();
        }

        EchoSubCommand sub = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null || !sub.canUse(sender)) return List.of();

        EchoCommandContext ctx = new EchoCommandContext(sender, label, args[0],
                Arrays.copyOfRange(args, 1, args.length));
        List<String> result = sub.tabComplete(ctx);
        return result == null ? List.of() : result;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("TheEcho commands:", NamedTextColor.GOLD));
        boolean any = false;
        for (EchoSubCommand sub : subCommands) {
            if (!sub.canUse(sender)) continue;
            any = true;
            Component line = Component.text("/" + label + " " + sub.name(), NamedTextColor.YELLOW);
            if (!sub.usage().isEmpty()) {
                line = line.append(Component.text(" " + sub.usage(), NamedTextColor.GRAY));
            }
            if (!sub.description().isEmpty()) {
                line = line.append(Component.text(" - " + sub.description(), NamedTextColor.WHITE));
            }
            sender.sendMessage(line);
        }
        if (!any) {
            sender.sendMessage(Component.text("There is nothing you may use.", NamedTextColor.GRAY));
        }
    }
}