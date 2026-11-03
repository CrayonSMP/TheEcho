package de.tamashiimon.theecho;

import de.tamashiimon.theecho.commands.EchoCommand;
import de.tamashiimon.theecho.commands.subcommands.ReloadSubCommand;
import de.tamashiimon.theecho.features.blocks.BlockRegistry;
import de.tamashiimon.theecho.features.items.ItemRegistry;
import de.tamashiimon.theecho.models.Config;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class TheEcho extends JavaPlugin {

    @Getter
    private static Config pluginConfig;

    private static TheEcho plugin;

    @Getter
    private static BlockRegistry blockRegistry;
    @Getter
    private static ItemRegistry itemRegistry;

    @Override
    public void onEnable() {
        plugin = this;
        try {
            pluginConfig = ConfigLoader.load(this);
        } catch (IOException e) {
            getLogger().severe("Invalid config.yml: " + e.getMessage());
        }

        EchoCommand command = new EchoCommand();
        getServer().getPluginCommand("theecho").setExecutor(command);
        command.register(new ReloadSubCommand(this));

        blockRegistry = new BlockRegistry(this);
        itemRegistry = new ItemRegistry(this);
        blockRegistry.registerBlocks();
        itemRegistry.registerItems();
    }

    public void reload() throws IOException {
        pluginConfig = ConfigLoader.load(this);

        blockRegistry.reloadBlocks();
        itemRegistry.reloadItems();
    }

    public static TheEcho getInstance() {
        return plugin;
    }
}