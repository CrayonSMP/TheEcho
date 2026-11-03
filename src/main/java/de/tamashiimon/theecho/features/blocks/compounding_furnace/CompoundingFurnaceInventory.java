package de.tamashiimon.theecho.features.blocks.compounding_furnace;

import de.tamashiimon.theecho.TheEcho;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceData;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceRecipe;
import de.tamashiimon.theecho.models.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundingFurnaceInventory implements InventoryHolder {
    private static final int INVENTORY_SIZE = 54;

    /** Currently open furnace inventories, so the ticker can work on the live data. */
    private static final Map<String, CompoundingFurnaceInventory> OPEN = new ConcurrentHashMap<>();

    private static String registryKey(@NotNull World world, @NotNull BlockPos pos) {
        return world.getUID() + ":" + pos.x() + "," + pos.y() + "," + pos.z();
    }

    public static @Nullable CompoundingFurnaceInventory find(@NotNull World world, @NotNull BlockPos pos) {
        return OPEN.get(registryKey(world, pos));
    }

    private final Inventory inventory;
    private final World world;
    private final BlockPos pos;
    private CompoundingFurnaceData data;
    private final Config config = TheEcho.getPluginConfig();

    private final Config.Slots slots = config.compoundingFurnace().slots();
    private final Config.Items items = config.compoundingFurnace().items();

    private final List<ItemStack> progressItems = new ArrayList<>();

    public CompoundingFurnaceInventory(@NotNull World world, @NotNull BlockPos pos, @NotNull CompoundingFurnaceData data) {
        this.world = world;
        this.pos = pos;
        this.data = data;

        String title = config.compoundingFurnace().inventoryTitle();
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
                title == null || title.isBlank()
                        ? Component.text("Compounding Furnace")
                        : MiniMessage.miniMessage().deserialize(title));

        for (String id : items.processingItems()) {
            progressItems.add(buildItem(id));
        }

        loadFromData();
        updateInventory(null);
        OPEN.put(registryKey(world, pos), this);
    }

    /** Removes this inventory from the open-registry. Call when the last viewer closes it. */
    public void unregister() {
        OPEN.remove(registryKey(world, pos), this);
    }

    public void updateInventory(@Nullable CompoundingFurnaceRecipe recipe) {
        if (progressItems.isEmpty()) return;

        int index = 0;
        if (recipe != null && recipe.processingTime() > 0) {
            double progress = Math.clamp((double) data.currentProcessTime() / recipe.processingTime(), 0.0, 1.0);
            index = (int) Math.floor(progress * (progressItems.size() - 1));
            // Only show the last stage once the recipe is actually complete
            if (progress >= 1.0) index = progressItems.size() - 1;
        }
        setIfChanged(slots.processSlot(), progressItems.get(index));
    }

    /**
     * Reads the current slot contents into the data object and writes it to the chunk's PDC.
     * Call this after every change to the input/fuel/tool/output slots (the listener does this).
     */
    public void syncData() {
        this.data = toData();
        writeToChunk();
    }

    /** Replaces the data (e.g. after a tick), refreshes changed slots and persists it when needed. */
    public void applyData(@NotNull CompoundingFurnaceData newData, @Nullable CompoundingFurnaceRecipe recipe) {
        boolean itemsChanged = !CompoundingFurnaceBehavior.sameItems(this.data, newData);
        this.data = newData;
        loadFromData();
        updateInventory(recipe);
        int time = newData.currentProcessTime();
        if (itemsChanged || (time > 0 && time % CompoundingFurnaceBehavior.SAVE_INTERVAL == 0)) {
            writeToChunk();
        }
    }

    private void writeToChunk() {
        world.getChunkAt(pos.x() >> 4, pos.z() >> 4)
                .getPersistentDataContainer()
                .set(CompoundingFurnaceBehavior.dataKey(pos), PersistentDataType.STRING, data.toString());
    }

    /**
     * Shift-click insert from the player inventory: fills matching stacks first, then empty slots.
     * Only slots whose role (input1/input2/fuel/tool) is used by at least one recipe for this item are considered.
     *
     * @return the remainder that did not fit (null if everything fit)
     */
    public @Nullable ItemStack insertShift(@NotNull ItemStack stack, @NotNull CompoundingFurnaceService service) {
        List<Integer> targets = new ArrayList<>();
        if (service.isInput1(stack)) targets.add(slots.input1Slot());
        if (service.isInput2(stack)) targets.add(slots.input2Slot());
        if (service.isFuel(stack)) targets.add(slots.fuelSlot());
        if (service.isTool(stack)) targets.add(slots.processToolSlot());

        ItemStack rest = stack.clone();

        // 1. merge into existing stacks
        for (int slot : targets) {
            if (rest.getAmount() <= 0) break;
            ItemStack cur = inventory.getItem(slot);
            if (cur == null || cur.getType().isAir() || !cur.isSimilar(rest)) continue;
            int move = Math.min(cur.getMaxStackSize() - cur.getAmount(), rest.getAmount());
            if (move <= 0) continue;
            cur.setAmount(cur.getAmount() + move);
            inventory.setItem(slot, cur);
            rest.setAmount(rest.getAmount() - move);
        }

        // 2. fill empty slots
        for (int slot : targets) {
            if (rest.getAmount() <= 0) break;
            ItemStack cur = inventory.getItem(slot);
            if (cur != null && !cur.getType().isAir()) continue;
            int move = Math.min(rest.getMaxStackSize(), rest.getAmount());
            ItemStack put = rest.clone();
            put.setAmount(move);
            inventory.setItem(slot, put);
            rest.setAmount(rest.getAmount() - move);
        }

        return rest.getAmount() <= 0 ? null : rest;
    }

    /**
     * Reads the current slot contents and returns a new data object
     * (the record is immutable, so a new instance is created).
     */
    public @NotNull CompoundingFurnaceData toData() {
        return new CompoundingFurnaceData(
                get(slots.input1Slot()),
                get(slots.input2Slot()),
                get(slots.outputSlot()),
                get(slots.fuelSlot()),
                get(slots.processToolSlot()),
                data.currentProcessTime()
        );
    }

    public @NotNull CompoundingFurnaceData data() {
        return data;
    }

    /** Slots the player may put items into / take items out of. */
    public boolean isInteractiveSlot(int slot) {
        return slot == slots.input1Slot()
                || slot == slots.input2Slot()
                || slot == slots.fuelSlot()
                || slot == slots.outputSlot()
                || slot == slots.processToolSlot();
    }

    /** Slots where the player may only take items, not insert. */
    public boolean isOutputSlot(int slot) {
        return slot == slots.outputSlot();
    }

    /** Slots (e.g. the progress indicator or empty ones) that must never be modified by the player. */
    public boolean isLockedSlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE && !isInteractiveSlot(slot);
    }

    public @NotNull BlockPos pos() {
        return pos;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ---------------------------------------------------------------- internals

    private void loadFromData() {
        setIfChanged(slots.input1Slot(), data.input1());
        setIfChanged(slots.input2Slot(), data.input2());
        setIfChanged(slots.fuelSlot(), data.fuel());
        setIfChanged(slots.outputSlot(), data.output());
        setIfChanged(slots.processToolSlot(), data.processingTool());
    }

    /** Only touches the slot if the item actually differs (avoids flicker and needless packets). */
    private void setIfChanged(int slot, @Nullable ItemStack item) {
        ItemStack current = inventory.getItem(slot);
        boolean currentEmpty = current == null || current.getType().isAir();
        boolean newEmpty = item == null || item.getType().isAir();

        if (currentEmpty && newEmpty) return;
        if (!currentEmpty && !newEmpty && current.equals(item)) return;

        inventory.setItem(slot, newEmpty ? null : item.clone());
    }

    /** Returns a copy of the slot content (never the live stack, so stored data cannot change behind our back). */
    private @Nullable ItemStack get(int slot) {
        ItemStack stack = inventory.getItem(slot);
        return stack == null || stack.getType().isAir() ? null : stack.clone();
    }

    /**
     * Builds an item from a config id. Vanilla ids ("minecraft:...") resolve via the Paper registry,
     * everything else via CraftEngine.
     */
    private static @NotNull ItemStack buildItem(@NotNull String id) {
        if (id.startsWith("minecraft:")) {
            NamespacedKey key = NamespacedKey.fromString(id);
            if (key == null) {
                throw new IllegalArgumentException("Invalid Item-ID: " + id);
            }

            ItemType type = Registry.ITEM.get(key);
            if (type == null) {
                throw new IllegalArgumentException("Unknown Minecraft-Item: " + id);
            }

            return type.createItemStack();
        }

        Key key = Key.of(id);
        BukkitItemDefinition customItem = CraftEngineItems.byId(key);
        if (customItem == null) {
            throw new IllegalArgumentException("Unknown CraftEngine-Item: " + id);
        }

        return customItem.buildBukkitItem();
    }
}