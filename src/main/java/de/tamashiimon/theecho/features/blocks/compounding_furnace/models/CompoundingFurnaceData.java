package de.tamashiimon.theecho.features.blocks.compounding_furnace.models;

import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public record CompoundingFurnaceData(
        @Nullable ItemStack input1,
        @Nullable ItemStack input2,
        @Nullable ItemStack output,
        @Nullable ItemStack fuel,
        @Nullable ItemStack processingTool,
        int currentProcessTime
) {

    private static final String SEPARATOR = ";";
    private static final String POS_SEPARATOR = ",";
    private static final int FIELD_COUNT = 6;

    @Override
    public @NotNull String toString() {
        return String.join(SEPARATOR,
                encode(input1),
                encode(input2),
                encode(output),
                encode(fuel),
                encode(processingTool),
                String.valueOf(currentProcessTime)
        );
    }

    public static @NotNull CompoundingFurnaceData fromString(@Nullable String string) {
        if (string == null) {
            throw new IllegalArgumentException("String is null");
        }

        String[] parts = string.split(SEPARATOR, -1);
        if (parts.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid format, expected " + FIELD_COUNT + " fields but found " + parts.length);
        }

        int processTime;
        try {
            processTime = Integer.parseInt(parts[5]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid current process time: '" + parts[5] + "'", e);
        }

        return new CompoundingFurnaceData(
                decode(parts[0]),
                decode(parts[1]),
                decode(parts[2]),
                decode(parts[3]),
                decode(parts[4]),
                processTime
        );
    }

    private static @NotNull String encode(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static @Nullable ItemStack decode(@NotNull String data) {
        if (data.isEmpty()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (Exception e) {
            // deserializeBytes can throw more than IllegalArgumentException (e.g. data from a newer
            // server version, unknown components) - always report the real reason
            throw new IllegalArgumentException("Invalid item data ("
                    + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }
    }
}