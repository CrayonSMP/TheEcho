package de.tamashiimon.theecho.features.blocks.compounding_furnace.models;

import org.bukkit.NamespacedKey;

// The strings are namespace keys
// ProcessTime in ticks
public record CompoundingFurnaceRecipe(NamespacedKey key,
                                       String fuel,
                                       int fuelAmount,
                                       String processingTool,
                                       int processingToolAmount,
                                       int processingTime,
                                       String input1,
                                       int input1Amount,
                                       String input2,
                                       int input2Amount,
                                       String output,
                                       int outputAmount
) {}