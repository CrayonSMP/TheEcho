package de.tamashiimon.theecho.features.items.scythe;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ScytheBehavior extends ItemBehavior implements Listener {
    public static final ItemBehaviorFactory<ScytheBehavior> FACTORY = new Factory();
    private final Key key;
    private final double dropMultiplier;
    static final Map<Key, ScytheBehavior> REGISTERED = new ConcurrentHashMap<>();

    public ScytheBehavior(Key key, double dropMultiplier) {
        this.key = key;
        this.dropMultiplier = dropMultiplier;
    }

    void handleLeftClick(PlayerInteractEvent event, ItemStack item) {
        Block block = event.getClickedBlock();
        if (block == null || !isCrop(block)) return;

        event.setCancelled(true);
        dropLoot(block, item, event.getPlayer(), dropMultiplier);
        resetCrop(block);
    }

    private static final Key CROPS_TAG = Key.of("minecraft:crops");

    private boolean isCrop(Block block) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state != null) {
            return state.settings().tags().contains(CROPS_TAG);
        }

        return Tag.CROPS.isTagged(block.getType());
    }

    private void resetCrop(Block block) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state != null) {
            ImmutableBlockState first = state.owner().value().defaultState();
            CraftEngineBlocks.place(block.getLocation(), first, UpdateFlags.UPDATE_ALL, false);
            return;
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable);
        }
    }

    private void dropLoot(Block block, ItemStack tool, Player player, double multiplier) {
        Collection<ItemStack> drops = getDrops(block, tool, player);
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) continue;

            int amount = multiply(drop.getAmount(), multiplier);
            int maxStack = drop.getMaxStackSize();

            while (amount > 0) {
                int size = Math.min(amount, maxStack);
                ItemStack part = drop.clone();
                part.setAmount(size);
                block.getWorld().dropItemNaturally(loc, part);
                amount -= size;
            }
        }
    }

    private int multiply(int amount, double multiplier) {
        double result = amount * multiplier;
        int whole = (int) result;
        if (ThreadLocalRandom.current().nextDouble() < (result - whole)) whole++;
        return whole;
    }

    private Collection<ItemStack> getDrops(Block block, ItemStack tool, Player player) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state != null) {
            return getCraftEngineDrops(block, state, player);
        }
        return block.getDrops(tool, player);
    }

    private Collection<ItemStack> getCraftEngineDrops(Block block, ImmutableBlockState state, Player player) {
        return List.of();
    }

    private boolean matches(ItemStack item) {
        Key customId = CraftEngineItems.getCustomItemId(item);
        if (customId != null) {
            return customId.equals(this.key);
        }

        return Key.of(item.getType().getKey().toString()).equals(this.key);
    }

    private static class Factory implements ItemBehaviorFactory<ScytheBehavior> {
        @Override
        public ScytheBehavior create(Pack pack, Path path, Key itemKey, ConfigSection section) {
            ScytheBehavior behavior = new ScytheBehavior(itemKey, section.getDouble("dropMultiplier", 1.0));
            REGISTERED.put(itemKey, behavior);
            return behavior;
        }
    }
}