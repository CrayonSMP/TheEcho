package de.tamashiimon.theecho.features.items.scythe;

import lombok.Getter;

@Getter
public class ScytheSettings {
    private final String itemID;
    private final double multiplier;

    public ScytheSettings(String itemID, double multiplier) {
        this.itemID = itemID;
        this.multiplier = multiplier;
    }
}
