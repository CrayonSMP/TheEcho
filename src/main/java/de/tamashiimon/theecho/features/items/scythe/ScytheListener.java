package de.tamashiimon.theecho.features.items.scythe;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class ScytheListener implements Listener {
    public static List<ScytheSettings> scytheSettings = new ArrayList<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            if ()
        }
    }
}
