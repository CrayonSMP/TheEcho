package de.tamashiimon.theecho.features.items.scythe;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ScytheListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        Key id = CraftEngineItems.getCustomItemId(item);
        if (id == null) id = Key.of(item.getType().getKey().toString());

        ScytheBehavior behavior = ScytheBehavior.REGISTERED.get(id);
        if (behavior != null) behavior.handleLeftClick(event, item);
    }
}