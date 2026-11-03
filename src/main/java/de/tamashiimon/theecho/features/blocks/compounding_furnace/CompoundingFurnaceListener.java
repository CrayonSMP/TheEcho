package de.tamashiimon.theecho.features.blocks.compounding_furnace;

import de.tamashiimon.theecho.features.BlockRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class CompoundingFurnaceListener implements Listener {
    private final Plugin plugin;

    public CompoundingFurnaceListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CompoundingFurnaceInventory furnace)) return;

        boolean clickedTop = event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());

        if (clickedTop) {
            int slot = event.getSlot();
            if (furnace.isLockedSlot(slot)) {
                event.setCancelled(true);
                return;
            }
            // Output slot: taking items is allowed, inserting is not
            if (furnace.isOutputSlot(slot) && !isTakeAction(event.getAction())) {
                event.setCancelled(true);
                return;
            }
        } else {
            // Double-click "collect to cursor" could pull items out of locked/output slots
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                return;
            }

            // Shift-click from the player inventory: route the items into the matching furnace slots ourselves
            if (event.isShiftClick() && event.getClickedInventory() != null) {
                event.setCancelled(true);

                ItemStack current = event.getCurrentItem();
                if (current == null || current.getType().isAir()) return;

                ItemStack rest = furnace.insertShift(current, BlockRegistry.getCompoundingFurnaceService());
                event.getClickedInventory().setItem(event.getSlot(), rest);
                furnace.syncData();
                return;
            }
        }

        syncNextTick(furnace);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CompoundingFurnaceInventory furnace)) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize && (furnace.isLockedSlot(rawSlot) || furnace.isOutputSlot(rawSlot))) {
                event.setCancelled(true);
                return;
            }
        }

        syncNextTick(furnace);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CompoundingFurnaceInventory furnace) {
            furnace.syncData();
            // The closing player is still counted as viewer during this event
            if (event.getInventory().getViewers().size() <= 1) {
                furnace.unregister();
            }
        }
    }

    private static boolean isTakeAction(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, MOVE_TO_OTHER_INVENTORY -> true;
            default -> false;
        };
    }

    // The inventory only reflects the change after the event has finished, so sync on the next tick
    private void syncNextTick(CompoundingFurnaceInventory furnace) {
        Bukkit.getScheduler().runTask(plugin, furnace::syncData);
    }
}