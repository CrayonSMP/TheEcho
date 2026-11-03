package de.tamashiimon.theecho.features.blocks.compounding_furnace;

import de.tamashiimon.theecho.TheEcho;
import de.tamashiimon.theecho.features.BlockRegistry;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceData;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceRecipe;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CompoundingFurnaceBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<CompoundingFurnaceBehavior> FACTORY = new Factory();

    private static final int TICKS_PER_CALL = 1;
    /** Delay of the self-scheduled tick: 1 = tick() runs every game tick. */
    private static final int TICK_DELAY = 1;
    private static final boolean CONSUME_TOOL = true;
    static final int SAVE_INTERVAL = 20;

    /**
     * Live state of furnaces that nobody has open. Without this, every tick would re-read the old state from
     * the chunk PDC (which is only written periodically) and the process time could never advance.
     */
    private static final Map<String, CompoundingFurnaceData> IDLE = new ConcurrentHashMap<>();

    public final Property<Boolean> litProperty;
    private final CompoundingFurnaceService compoundingFurnaceService;

    private CompoundingFurnaceBehavior(BlockDefinition blockDefinition, Property<Boolean> litProperty, CompoundingFurnaceService compoundingFurnaceService) {
        super(blockDefinition);
        this.litProperty = litProperty;
        this.compoundingFurnaceService = compoundingFurnaceService;
    }

    public static @NotNull NamespacedKey dataKey(@NotNull BlockPos pos) {
        return Objects.requireNonNull(NamespacedKey.fromString(
                "theecho:compoundingfurnace/" + pos.x() + "_" + pos.y() + "_" + pos.z()));
    }

    private static @NotNull String cacheKey(@NotNull World world, @NotNull BlockPos pos) {
        return world.getUID() + ":" + pos.x() + "," + pos.y() + "," + pos.z();
    }

    /** Removes and returns the cached live state of a closed furnace (null if there is none). */
    private static @Nullable CompoundingFurnaceData takeIdle(@NotNull World world, @NotNull BlockPos pos) {
        return IDLE.remove(cacheKey(world, pos));
    }

    static boolean sameItems(@NotNull CompoundingFurnaceData a, @NotNull CompoundingFurnaceData b) {
        return Objects.equals(a.input1(), b.input1())
                && Objects.equals(a.input2(), b.input2())
                && Objects.equals(a.output(), b.output())
                && Objects.equals(a.fuel(), b.fuel())
                && Objects.equals(a.processingTool(), b.processingTool());
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        Object level = args[1];
        Object blockPos = args[2];

        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        Block bukkitBlock = CraftBlockProxy.INSTANCE.at(level, blockPos);
        PersistentDataContainer pdc = bukkitBlock.getChunk().getPersistentDataContainer();

        if (!pdc.has(dataKey(pos), PersistentDataType.STRING)) {
            CompoundingFurnaceData data = new CompoundingFurnaceData(null, null, null, null, null, 0);
            pdc.set(dataKey(pos), PersistentDataType.STRING, data.toString());
        }

        // start the tick loop (tick() reschedules itself)
        scheduleNextTick(level, blockPos, thisBlock);
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
        super.affectNeighborsAfterRemoval(thisBlock, args);
        Object level = args[1];
        Object blockPos = args[2];

        BlockPos pos = LocationUtils.fromBlockPos(blockPos);
        Block bukkitBlock = CraftBlockProxy.INSTANCE.at(level, blockPos);
        World world = bukkitBlock.getWorld();


        CompoundingFurnaceInventory open = CompoundingFurnaceInventory.find(world, pos);
        if (open != null) {
            for (HumanEntity viewer : new ArrayList<>(open.getInventory().getViewers())) {
                viewer.closeInventory();
            }
            open.unregister();
        }

        PersistentDataContainer pdc = bukkitBlock.getChunk().getPersistentDataContainer();
        NamespacedKey key = dataKey(pos);

        // The cached state is newer than the PDC, so prefer it
        CompoundingFurnaceData data = takeIdle(world, pos);
        if (data == null) {
            String dataString = pdc.get(key, PersistentDataType.STRING);
            if (dataString == null) return;
            try {
                data = CompoundingFurnaceData.fromString(dataString);
            } catch (IllegalArgumentException e) {
                pdc.remove(key);
                return;
            }
        }
        Location center = bukkitBlock.getLocation().add(0.5, 0.5, 0.5);

        for (ItemStack item : new ItemStack[]{
                data.input1(), data.input2(), data.output(), data.fuel(), data.processingTool()}) {
            if (item != null && !item.getType().isAir()) {
                world.dropItemNaturally(center, item);
            }
        }

        pdc.remove(key);
    }

    /** Vanilla scheduled tick: only runs when scheduled, so it schedules itself again every time. */
    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object level = args[1];
        Object blockPos = args[2];

        Block block = CraftBlockProxy.INSTANCE.at(level, blockPos);
        boolean keepTicking;
        try {
            keepTicking = process(block);
        } catch (Exception e) {
            TheEcho.getPlugin(TheEcho.class).getLogger().log(java.util.logging.Level.WARNING,
                    "Error while ticking compounding furnace at " + LocationUtils.fromBlockPos(blockPos), e);
            return; // stop instead of spamming every tick; opening the furnace starts it again
        }
        if (keepTicking) {
            scheduleNextTick(level, blockPos, thisBlock);
        }
    }

    private static void scheduleNextTick(Object level, Object blockPos, Object thisBlock) {
        // identical scheduled ticks are de-duplicated by the game, so calling this repeatedly is safe
        LevelAccessorProxy.INSTANCE.scheduleTick$0(level, blockPos, thisBlock, TICK_DELAY);
    }

    /** @return false if this block is no longer a valid furnace and should not be ticked any more */
    private boolean process(@NotNull Block block) {
        if (CraftEngineBlocks.getCustomBlockState(block) == null) {
            IDLE.remove(cacheKey(block.getWorld(), new BlockPos(block.getX(), block.getY(), block.getZ())));
            return false;
        }

        BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        World world = block.getWorld();
        NamespacedKey key = dataKey(pos);
        String cacheKey = cacheKey(world, pos);
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();

        CompoundingFurnaceInventory open = CompoundingFurnaceInventory.find(world, pos);

        CompoundingFurnaceData data;
        if (open != null) {
            // the open inventory is the source of truth while someone is looking at it
            IDLE.remove(cacheKey);
            data = open.toData();
        } else {
            data = IDLE.get(cacheKey);
            if (data == null) {
                String raw = pdc.get(key, PersistentDataType.STRING);
                if (raw == null) return false;
                try {
                    data = CompoundingFurnaceData.fromString(raw);
                } catch (IllegalArgumentException e) {
                    TheEcho.getPlugin(TheEcho.class).getLogger()
                            .warning("Invalid compounding furnace data at " + pos + ": " + e.getMessage());
                    return false;
                }
            }
        }

        Optional<CompoundingFurnaceRecipe> match = compoundingFurnaceService.findRecipe(data);
        boolean working = match.isPresent() && compoundingFurnaceService.canOutput(data.output(), match.get());

        CompoundingFurnaceData next;
        if (working) {
            CompoundingFurnaceRecipe recipe = match.get();
            int time = data.currentProcessTime() + TICKS_PER_CALL;
            next = time >= recipe.processingTime() ? craft(data, recipe) : withTime(data, time);
        } else {
            next = data.currentProcessTime() == 0 ? data : withTime(data, 0);
        }

        if (open != null) {
            open.applyData(next, working ? match.get() : null);
        } else {
            // keep the live state in memory, the PDC is only written periodically / on item changes
            IDLE.put(cacheKey, next);
            if (next != data
                    && (!sameItems(data, next) || next.currentProcessTime() % SAVE_INTERVAL == 0)) {
                pdc.set(key, PersistentDataType.STRING, next.toString());
            }
        }

        setLit(block, working);
        return true;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        // Furnaces without a pending tick (e.g. placed before ticking existed, or stopped by an error) restart here
        scheduleNextTick(context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(context.getClickedPos()),
                BlockStateUtils.getBlockOwner(state.customBlockState().minecraftState()));
        var cePlayer = context.getPlayer();
        if (cePlayer == null) return InteractionResult.SUCCESS;

        org.bukkit.entity.Player player = Bukkit.getPlayer(cePlayer.uuid());
        if (player == null) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        World world = player.getWorld();

        CompoundingFurnaceInventory inventory = CompoundingFurnaceInventory.find(world, pos);
        if (inventory == null) {
            // the cached state is newer than the PDC, so prefer it
            CompoundingFurnaceData data = takeIdle(world, pos);
            if (data == null) data = loadData(world, pos);
            if (data == null) {
                // Stored data is unreadable: do NOT open an empty GUI, closing it would overwrite the stored items
                player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Der Ofen enthaelt nicht lesbare Daten (siehe Konsole). Die Items bleiben gespeichert."));
                return InteractionResult.SUCCESS;
            }
            inventory = new CompoundingFurnaceInventory(world, pos, data);
            compoundingFurnaceService.findRecipe(data).ifPresent(inventory::updateInventory);
        }

        player.openInventory(inventory.getInventory());
        return InteractionResult.SUCCESS;
    }

    /** Loads the stored data. Empty data if nothing is stored, null if stored data exists but cannot be read. */
    private static @Nullable CompoundingFurnaceData loadData(@NotNull World world, @NotNull BlockPos pos) {
        String raw = world.getChunkAt(pos.x() >> 4, pos.z() >> 4)
                .getPersistentDataContainer()
                .get(dataKey(pos), PersistentDataType.STRING);

        if (raw != null) {
            try {
                return CompoundingFurnaceData.fromString(raw);
            } catch (IllegalArgumentException e) {
                TheEcho.getPlugin(TheEcho.class).getLogger().log(java.util.logging.Level.WARNING,
                        "Invalid compounding furnace data at " + pos + ": " + e.getMessage(), e.getCause() != null ? e.getCause() : e);
                return null;
            }
        }
        return new CompoundingFurnaceData(null, null, null, null, null, 0);
    }

    private static @NotNull CompoundingFurnaceData withTime(@NotNull CompoundingFurnaceData d, int time) {
        return new CompoundingFurnaceData(d.input1(), d.input2(), d.output(), d.fuel(), d.processingTool(), time);
    }

    private @NotNull CompoundingFurnaceData craft(@NotNull CompoundingFurnaceData d, @NotNull CompoundingFurnaceRecipe r) {
        ItemStack output = d.output();
        if (output == null || output.getType().isAir()) {
            output = CompoundingFurnaceService.buildItem(r.output(), r.outputAmount());
            if (output == null) {
                TheEcho.getPlugin(TheEcho.class).getLogger()
                        .warning("Recipe " + r.key() + ": unknown output item '" + r.output() + "'");
                return withTime(d, 0);
            }
        } else {
            output = output.clone();
            output.setAmount(output.getAmount() + r.outputAmount());
        }

        return new CompoundingFurnaceData(
                shrink(d.input1(), r.input1Amount()),
                shrink(d.input2(), r.input2Amount()),
                output,
                shrink(d.fuel(), r.fuelAmount()),
                CONSUME_TOOL ? shrink(d.processingTool(), r.processingToolAmount()) : d.processingTool(),
                0
        );
    }

    private static @Nullable ItemStack shrink(@Nullable ItemStack stack, int amount) {
        if (stack == null) return null;
        int left = stack.getAmount() - amount;
        if (left <= 0) return null;
        ItemStack copy = stack.clone();
        copy.setAmount(left);
        return copy;
    }

    private void setLit(@NotNull Block block, boolean lit) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null || state.get(litProperty) == lit) return;
        CraftEngineBlocks.place(block.getLocation(), state.with(litProperty, lit), UpdateFlags.UPDATE_ALL, false);
    }

    private static class Factory implements BlockBehaviorFactory<CompoundingFurnaceBehavior> {
        @Override
        public CompoundingFurnaceBehavior create(BlockDefinition block, ConfigSection section) {
            return new CompoundingFurnaceBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "lit", Boolean.class),
                    BlockRegistry.getCompoundingFurnaceService()
            );
        }
    }
}