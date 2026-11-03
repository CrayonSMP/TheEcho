package de.tamashiimon.theecho.features.items.scythe;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.UseOnContext;

import java.nio.file.Path;

public class ScytheBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<ScytheBehavior> FACTORY = new Factory();

    public ScytheBehavior() {

    }

    private static class Factory implements ItemBehaviorFactory<ScytheBehavior> {
        @Override
        public ScytheBehavior create(Pack pack, Path path, Key itemKey, ConfigSection section) {

            return new ScytheBehavior();
        }
    }
}