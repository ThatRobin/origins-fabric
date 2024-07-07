package io.github.apace100.origins.registry;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.component.PlayerOriginComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public class ModComponents implements EntityComponentInitializer {

    public static final ComponentKey<OriginComponent> ORIGIN = ComponentRegistry.getOrCreate(Origins.identifier("origin"), OriginComponent.class);

    public static void register() {

    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, ORIGIN)
            .after(PowerHolderComponent.KEY)
            .respawnStrategy(RespawnCopyStrategy.CHARACTER)
            .end(PlayerOriginComponent::new);
    }

}
