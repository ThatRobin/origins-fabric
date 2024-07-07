package io.github.apace100.origins.badge;

import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.registry.DataObject;
import io.github.apace100.calio.registry.DataObjectFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;

import java.util.List;

public interface Badge extends DataObject<Badge> {
    
    Identifier spriteId();
    
    boolean hasTooltip();
    
    @Environment(EnvType.CLIENT)
    List<TooltipComponent> getTooltipComponents(PowerType<?> powerType, int widthLimit, float time, TextRenderer textRenderer);

    SerializableData.Instance toData(SerializableData.Instance instance);

    BadgeFactory getBadgeFactory();

    @Override
    default DataObjectFactory<Badge> getFactory() {
        return this.getBadgeFactory();
    }

    static Badge receive(RegistryByteBuf buf) {
        return BadgeManager.REGISTRY.receiveDataObject(buf);
    }

    default void send(RegistryByteBuf buf) {

        DataObjectFactory<Badge> factory = this.getFactory();
        buf.writeIdentifier(this.getBadgeFactory().id());

        factory.getData().write(buf, factory.toData(this));

    }
    
}
