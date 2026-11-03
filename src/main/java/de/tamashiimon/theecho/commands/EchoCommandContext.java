package de.tamashiimon.theecho.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.List;

/**
 * Everything a sub command gets: who ran it, the label of the root command and the arguments
 * <b>after</b> the sub command name.
 * <p>
 * Example: {@code /theecho reload recipes} -> {@code label = "theecho"}, {@code subLabel = "reload"},
 * {@code args = ["recipes"]}. For tab completion the last element of {@code args} is the word being typed.
 */
public record EchoCommandContext(@NotNull CommandSender sender,
                                 @NotNull String label,
                                 @NotNull String subLabel,
                                 @NotNull String[] args) {

    /** Number of arguments after the sub command name. */
    public int length() {
        return args.length;
    }

    public boolean has(int index) {
        return index >= 0 && index < args.length;
    }

    public @Nullable String arg(int index) {
        return has(index) ? args[index] : null;
    }

    public @NotNull String arg(int index, @NotNull String fallback) {
        return has(index) ? args[index] : fallback;
    }

    /** The word currently being typed (tab completion); empty string if there is none. */
    public @NotNull String lastArg() {
        return args.length == 0 ? "" : args[args.length - 1];
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public @Nullable Player player() {
        return sender instanceof Player p ? p : null;
    }

    public boolean hasPermission(@NotNull String permission) {
        return sender.hasPermission(permission);
    }

    public void reply(@NotNull Component message) {
        sender.sendMessage(message);
    }

    /** Sends a MiniMessage string. Do not put raw player input in here (it would be parsed as tags). */
    public void reply(@NotNull String miniMessage) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    /** Tab completion helper: the options that start with the word being typed (case-insensitive), sorted. */
    public @NotNull List<String> complete(@NotNull Collection<String> options) {
        String prefix = lastArg().toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}