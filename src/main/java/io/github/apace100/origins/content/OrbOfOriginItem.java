package io.github.apace100.origins.content;

import io.github.apace100.origins.component.item.ItemOriginsComponent;
import io.github.apace100.origins.registry.ModDataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.util.*;

public class OrbOfOriginItem extends Item {

    public OrbOfOriginItem() {
        this(new Settings()
            .maxCount(1)
            .rarity(Rarity.RARE)
            .component(ModDataComponentTypes.ORIGIN, ItemOriginsComponent.DEFAULT));
    }

    public OrbOfOriginItem(Settings settings) {
        super(settings);
    }

}
