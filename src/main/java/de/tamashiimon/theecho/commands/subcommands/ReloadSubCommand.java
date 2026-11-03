package de.tamashiimon.theecho.commands.subcommands;

import de.tamashiimon.theecho.TheEcho;
import de.tamashiimon.theecho.commands.EchoCommandContext;
import de.tamashiimon.theecho.commands.EchoSubCommand;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ReloadSubCommand extends EchoSubCommand {
    private final TheEcho plugin;

    public ReloadSubCommand(@NotNull TheEcho plugin) {
        super("reload", "theecho.reload");
        description("Reloads the compounding furnace recipes");
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull EchoCommandContext ctx) {
        try {
            TheEcho.getInstance().reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ctx.reply("<green>reloaded.");
    }
}