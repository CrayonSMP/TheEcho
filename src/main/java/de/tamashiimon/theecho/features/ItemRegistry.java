package de.tamashiimon.theecho.features;

import de.tamashiimon.theecho.features.blocks.compounding_furnace.CompoundingFurnaceBehavior;
import de.tamashiimon.theecho.features.items.scythe.ScytheBehavior;
import de.tamashiimon.theecho.features.items.scythe.ScytheListener;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.plugin.Plugin;

public class ItemRegistry {
    private static Key behaviorKey(String behavior) {
        return Key.from("theecho:" + behavior);
    }

    private final Plugin plugin;

    public ItemRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        registerBehaviors();
        registerListeners();
    }


    private void registerBehaviors() {
        ItemBehaviors.register(behaviorKey("compoundingfurnace"), ScytheBehavior.FACTORY);
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new  ScytheListener(), plugin);
    }

    public void reloadItems() {

    }
}
