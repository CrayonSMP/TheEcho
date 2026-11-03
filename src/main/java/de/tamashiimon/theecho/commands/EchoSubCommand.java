package de.tamashiimon.theecho.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Base class for a sub command of {@code /theecho}.
 * <pre>{@code
 * public class ReloadSubCommand extends EchoSubCommand {
 *     public ReloadSubCommand() {
 *         super("reload", "theecho.reload");
 *         description("Reloads the recipes");
 *     }
 *
 *     public void execute(EchoCommandContext ctx) {
 *         ctx.reply("<green>Reloaded!");
 *     }
 * }
 * }</pre>
 */
public abstract class EchoSubCommand {
    private final String name;
    private final @Nullable String permission;
    private List<String> aliases = List.of();
    private String description = "";
    private String usage = "";

    /** @param name the key used in the command, e.g. "reload" for {@code /theecho reload} */
    protected EchoSubCommand(@NotNull String name) {
        this(name, null);
    }

    /** @param permission required permission, or null if everybody may use it */
    protected EchoSubCommand(@NotNull String name, @Nullable String permission) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.permission = permission;
    }

    /** Runs the sub command. Everything after the sub command name is in {@code ctx.args()}. */
    public abstract void execute(@NotNull EchoCommandContext ctx);

    /** Tab completion for the arguments after the sub command name. Default: no suggestions. */
    public @Nullable List<String> tabComplete(@NotNull EchoCommandContext ctx) {
        return List.of();
    }

    // ---------------------------------------------------------------- optional settings (call in the constructor)

    protected final void aliases(@NotNull String... aliases) {
        this.aliases = java.util.Arrays.stream(aliases).map(a -> a.toLowerCase(Locale.ROOT)).toList();
    }

    protected final void description(@NotNull String description) {
        this.description = description;
    }

    /** Argument hint shown in the help, e.g. {@code "<recipe>"}. */
    protected final void usage(@NotNull String usage) {
        this.usage = usage;
    }

    // ---------------------------------------------------------------- getters

    public final @NotNull String name() {
        return name;
    }

    public final @NotNull List<String> aliases() {
        return aliases;
    }

    public final @Nullable String permission() {
        return permission;
    }

    public final @NotNull String description() {
        return description;
    }

    public final @NotNull String usage() {
        return usage;
    }

    public final boolean canUse(@NotNull CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }
}