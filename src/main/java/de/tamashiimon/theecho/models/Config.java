package de.tamashiimon.theecho.models;

import java.util.List;

public record Config(boolean debugMode, CompoundingFurnace compoundingFurnace) {

    public record CompoundingFurnace(String inventoryTitle, Slots slots, Items items) {}

    public record Slots(int input1Slot, int input2Slot, int fuelSlot,
                        int outputSlot, int processToolSlot, int processSlot) {}

    public record Items(List<String> processingItems) {

        public Items {
            if (processingItems == null) {
                throw new IllegalArgumentException("Processing-Items are null" );
            }
            processingItems = List.copyOf(processingItems);
        }
    }
}