package de.tamashiimon.theecho.features.blocks.compounding_furnace;

import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceData;
import de.tamashiimon.theecho.features.blocks.compounding_furnace.models.CompoundingFurnaceRecipe;
import de.tamashiimon.theecho.models.ItemSpec;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

public class CompoundingFurnaceService {
    private static final String RECIPE_TYPE = "compoundingfurnace";
    private static final String DEFAULT_RECIPE_RESOURCE = "recipes/default.yml";
    private final Map<NamespacedKey, CompoundingFurnaceRecipe> recipes = new HashMap<>();

    public boolean validateRecepie(CompoundingFurnaceData data) {
        return findRecipe(data).isPresent();
    }

    /** First recipe whose fuel, tool and both inputs match (id and minimum amount). */
    public @NotNull Optional<CompoundingFurnaceRecipe> findRecipe(@NotNull CompoundingFurnaceData data) {
        for (CompoundingFurnaceRecipe recipe : recipes.values()) {
            if (matches(data.fuel(), recipe.fuel(), recipe.fuelAmount())
                    && matches(data.processingTool(), recipe.processingTool(), recipe.processingToolAmount())
                    && matches(data.input1(), recipe.input1(), recipe.input1Amount())
                    && matches(data.input2(), recipe.input2(), recipe.input2Amount())) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** Whether the recipe's output still fits into the current output stack. */
    public boolean canOutput(@Nullable ItemStack output, @NotNull CompoundingFurnaceRecipe recipe) {
        if (output == null || output.getType().isAir()) return true;
        return matches(output, recipe.output(), 1)
                && output.getAmount() + recipe.outputAmount() <= output.getMaxStackSize();
    }

    // ------------------------------------------------ slot suitability (used for shift-click)

    public boolean isFuel(@Nullable ItemStack stack) {
        return usedAs(stack, CompoundingFurnaceRecipe::fuel);
    }

    public boolean isTool(@Nullable ItemStack stack) {
        return usedAs(stack, CompoundingFurnaceRecipe::processingTool);
    }

    public boolean isInput1(@Nullable ItemStack stack) {
        return usedAs(stack, CompoundingFurnaceRecipe::input1);
    }

    public boolean isInput2(@Nullable ItemStack stack) {
        return usedAs(stack, CompoundingFurnaceRecipe::input2);
    }

    /** Whether any recipe uses this item in the field selected by {@code field}. */
    private boolean usedAs(@Nullable ItemStack stack, Function<CompoundingFurnaceRecipe, String> field) {
        if (ItemStackUtils.isEmpty(stack)) return false;
        Key id = BukkitItemManager.instance().wrap(stack).id();
        for (CompoundingFurnaceRecipe recipe : recipes.values()) {
            if (id.equals(Key.of(field.apply(recipe)))) return true;
        }
        return false;
    }

    private static boolean matches(@Nullable ItemStack stack, String key, int amount) {
        if (ItemStackUtils.isEmpty(stack)) return false;
        if (stack.getAmount() < amount) return false;
        Item wrapped = BukkitItemManager.instance().wrap(stack);
        return wrapped.id().equals(Key.of(key));
    }

    /**
     * Builds an item from a namespaced id. Returns null if the id is unknown.
     * Vanilla ids resolve via Material, everything else via CraftEngine.
     */
    public static @Nullable ItemStack buildItem(@NotNull String id, int amount) {
        ItemStack stack;
        if (!id.contains(":") || id.startsWith("minecraft:")) {
            Material material = Material.matchMaterial(id);
            if (material == null || material.isAir()) return null;
            stack = new ItemStack(material);
        } else {
            BukkitItemDefinition definition = CraftEngineItems.byId(Key.of(id));
            if (definition == null) return null;
            stack = definition.buildBukkitItem();
        }
        stack.setAmount(amount);
        return stack;
    }

    public @NotNull Collection<CompoundingFurnaceRecipe> getRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public void loadRecipes(@NotNull Plugin plugin) {
        recipes.clear();

        Path folder = plugin.getDataFolder().toPath().resolve("recipes");

        if (Files.notExists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create recipes folder", e);
                return;
            }

            try {
                plugin.saveResource(DEFAULT_RECIPE_RESOURCE, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Default recipe file '" + DEFAULT_RECIPE_RESOURCE + "' not found in the plugin jar");
            }
        }

        List<Path> files;
        try (Stream<Path> stream = Files.walk(folder)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read recipes folder", e);
            return;
        }

        for (Path file : files) {
            loadFile(plugin, file);
        }

        plugin.getLogger().info("Loaded " + recipes.size() + " compounding furnace recipes.");
    }

    private void loadFile(Plugin plugin, Path file) {
        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(file.toFile());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not parse " + file.getFileName(), e);
            return;
        }

        ConfigurationSection section = yaml.getConfigurationSection("recipes");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(id);
            if (recipeSection == null) continue;

            if (!RECIPE_TYPE.equals(recipeSection.getString("type"))) continue;

            NamespacedKey key = NamespacedKey.fromString(id);
            if (key == null) {
                plugin.getLogger().warning(file.getFileName() + ": invalid recipe id '" + id + "'");
                continue;
            }
            if (recipes.containsKey(key)) {
                plugin.getLogger().warning(file.getFileName() + ": duplicate recipe id '" + id + "', skipping");
                continue;
            }

            try {
                recipes.put(key, fromConfig(key, recipeSection));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(file.getFileName() + ": recipe '" + id + "' is invalid: " + e.getMessage());
            }
        }
    }

    public static @NotNull CompoundingFurnaceRecipe fromConfig(@NotNull NamespacedKey key,
                                                               @NotNull ConfigurationSection section) {
        ItemSpec fuel = ItemSpec.parse(requireString(section, "fuel"));
        ItemSpec tool = ItemSpec.parse(requireString(section, "processing_tool"));
        ItemSpec in1 = ItemSpec.parse(requireString(section, "input1"));
        ItemSpec in2 = ItemSpec.parse(requireString(section, "input2"));
        ItemSpec out = ItemSpec.parse(requireString(section, "output"));

        return new CompoundingFurnaceRecipe(
                key,
                fuel.id(), fuel.amount(),
                tool.id(), tool.amount(),
                section.getInt("processing_time"),
                in1.id(), in1.amount(),
                in2.id(), in2.amount(),
                out.id(), out.amount()
        );
    }

    private static @NotNull String requireString(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing field '" + path + "'");
        }
        return value;
    }
}