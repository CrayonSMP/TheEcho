package de.tamashiimon.theecho.features;

import de.tamashiimon.theecho.features.blocks.compounding_furnace.CompoundingFurnaceBehavior;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.CompoundingFurnaceListener;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.CompoundingFurnaceService;
import de.tamashiimon.theecho.TheEcho;
import de.tamashiimon.theecho.models.Config;
import lombok.Getter;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.plugin.Plugin;

public class BlockRegistry {
    private static Key behaviorKey(String behavior) {
        return Key.from("theecho:" + behavior);
    }
    @Getter
    private static CompoundingFurnaceService compoundingFurnaceService;

    private final Plugin plugin;

    public BlockRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerBlocks() {
        registerBehaviors();
        registerListeners();
    }


    private void registerBehaviors() {
        compoundingFurnaceService = new CompoundingFurnaceService();
        compoundingFurnaceService.loadRecipes(plugin);
        BlockBehaviors.register(behaviorKey("compoundingfurnace"), CompoundingFurnaceBehavior.FACTORY);
    }

    private void registerListeners() {
         plugin.getServer().getPluginManager().registerEvents(new CompoundingFurnaceListener(plugin), plugin);
    }

    public void reloadBlocks() {
        compoundingFurnaceService.loadRecipes(plugin);
    }
}
