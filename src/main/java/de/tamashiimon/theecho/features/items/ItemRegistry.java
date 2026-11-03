package de.tamashiimon.theecho.features.items;

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
        //nothing rn -_-
    }

    private void registerListeners() {
        //nothing rn -_-
    }

    public void reloadItems() {

    }
}
