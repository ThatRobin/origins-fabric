package io.github.apace100.origins.origin;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.MultiplePower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerReference;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.util.Validatable;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.data.OriginsDataTypes;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.*;

public class Origin implements Validatable {

    public static final Origin EMPTY = OriginRegistry.register(Origin.special(Origins.identifier("empty"), ItemStack.EMPTY, Impact.NONE, Integer.MAX_VALUE));
    public static final SerializableDataType<Origin> DATA_TYPE = SerializableDataType.compound(
        new SerializableData()
            .add("id", SerializableDataTypes.IDENTIFIER)
            .add("icon", SerializableDataTypes.UNCOUNTED_ITEM_STACK, ItemStack.EMPTY)
            .addSupplied("powers", ApoliDataTypes.POWER_REFERENCE.listOf(), ArrayList::new)
            .addSupplied("upgrades", OriginsDataTypes.UPGRADES, ArrayList::new)
            .add("impact", OriginsDataTypes.IMPACT, Impact.NONE)
            .add("name", ApoliDataTypes.DEFAULT_TRANSLATABLE_TEXT, null)
            .add("description", ApoliDataTypes.DEFAULT_TRANSLATABLE_TEXT, null)
            .add("unchoosable", SerializableDataTypes.BOOLEAN, false)
            .add("order", SerializableDataTypes.INT, Integer.MAX_VALUE)
            .postProcessor(data -> {

                Identifier id = data.get("id");
                String baseKey = Util.createTranslationKey("origin", id);

                if (!data.isPresent("name")) {
                    data.set("name", Text.translatable(baseKey + ".name"));
                }

                if (!data.isPresent("description")) {
                    data.set("description", Text.translatable(baseKey + ".description"));
                }

            }),
        data -> {
            ItemStack iconStack = data.get("icon");
            return new Origin(
                data.get("id"),
                iconStack.copy(),
                data.get("powers"),
                data.get("upgrades"),
                data.get("impact"),
                data.get("name"),
                data.get("description"),
                data.get("unchoosable"),
                data.get("order")
            );
        },
        (origin, data) -> data
            .set("id", origin.getId())
            .set("icon", origin.getDisplayItem())
            .set("powers", origin.getPowers()
                .stream()
                .map(Power::getId)
                .map(PowerReference::new)
                .toList())
            .set("upgrades", origin.upgrades)
            .set("impact", origin.getImpact())
            .set("name", origin.getName())
            .set("description", origin.getDescription())
            .set("unchoosable", !origin.isChoosable())
            .set("special", origin.isSpecial())
            .set("order", origin.getOrder())
    );

    private final Identifier id;
    private final ItemStack displayItem;

    private final List<Power> powers;
    private final List<OriginUpgrade> upgrades;

    private final Impact impact;

    private final Text name;
    private final Text description;

    private final boolean choosable;
    private final boolean special;

    private final int order;

    protected Origin(Identifier id, ItemStack icon, List<Power> powers, List<OriginUpgrade> upgrades, Impact impact, Text name, Text description, boolean unchoosable, boolean special, int order) {
        this.id = id;
        this.displayItem = icon;
        this.powers = new LinkedList<>(powers);
        this.upgrades = upgrades;
        this.impact = impact;
        this.name = name;
        this.description = description;
        this.choosable = !unchoosable;
        this.special = special;
        this.order = order;
    }

    public Origin(Identifier id, ItemStack icon, List<Power> powers, List<OriginUpgrade> upgrades, Impact impact, Text name, Text description, boolean unchoosable, int order) {
        this(id, icon, powers, upgrades, impact, name, description, unchoosable, false, order);
    }

    public static Origin special(Identifier id, ItemStack icon, Impact impact, int order) {
        String baseKey = Util.createTranslationKey("origin", id);
        return new Origin(
            id, icon,
            new LinkedList<>(),
            new LinkedList<>(),
            impact,
            Text.translatable(baseKey + ".name"),
            Text.translatable(baseKey + ".description"),
            true,
            true,
            order
        );
    }

    public Identifier getId() {
        return id;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public ImmutableList<Power> getPowers() {
        return ImmutableList.copyOf(powers);
    }

    @Deprecated(forRemoval = true)
    public Optional<OriginUpgrade> getUpgrade(AdvancementEntry advancement) {
        return upgrades.stream()
            .filter(ou -> ou.advancementCondition().equals(advancement.id()))
            .findFirst();
    }

    @Deprecated(forRemoval = true)
    public boolean hasUpgrade() {
        return !this.upgrades.isEmpty();
    }

    public Impact getImpact() {
        return impact;
    }

    public MutableText getName() {
        return name.copy();
    }

    public MutableText getDescription() {
        return description.copy();
    }

    public boolean isChoosable() {
        return this.choosable;
    }

    public boolean isSpecial() {
        return this.special;
    }

    public int getOrder() {
        return this.order;
    }

    @Override
    public void validate() {

        List<Power> validatedPowers = new LinkedList<>();
        for (Power power : powers) {

            Identifier powerId = power.getId();

            if (power instanceof PowerReference reference) {
                power = reference.getReference();
            }

            if (power == null) {
                Origins.LOGGER.error("Origin \"{}\" contained unregistered power \"{}\"!", id, powerId);
            }

            else if (!Origins.config.isPowerDisabled(id, powerId)) {
                validatedPowers.add(power);
            }

        }

        this.powers.clear();
        this.powers.addAll(validatedPowers);

    }

    public boolean hasPower(Power targetPower) {
        return powers.contains(targetPower) || powers
            .stream()
            .filter(MultiplePower.class::isInstance)
            .map(MultiplePower.class::cast)
            .map(MultiplePower::getSubPowerIds)
            .flatMap(Collection::stream)
            .anyMatch(targetPower.getId()::equals);
    }


    public static Origin receive(RegistryByteBuf buf) {
        return DATA_TYPE.receive(buf);
    }

    public void send(RegistryByteBuf buf) {
        DATA_TYPE.send(buf, this);
    }

    public static Origin fromJson(Identifier id, JsonObject jsonObject) {
        jsonObject.addProperty("id", id.toString());
        return DATA_TYPE.strictParse(Calio.wrapRegistryOps(JsonOps.INSTANCE), jsonObject);
    }

    public JsonObject toJson() {
        return DATA_TYPE.strictEncodeStart(JsonOps.INSTANCE, this).getAsJsonObject();
    }

    @Override
    public String toString() {

        StringBuilder str = new StringBuilder("Origin[id = " + id.toString() + ", powers = {");
        String separator = "";

        for (Power power : powers) {
            str.append(separator).append(power.getId());
            separator = ", ";
        }

        str.append("}]");
        return str.toString();

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Origin other && this.id.equals(other.id));
    }

    public static void init() {

    }

    public static Map<OriginLayer, Origin> get(Entity entity) {
        if(entity instanceof PlayerEntity) {
            return get((PlayerEntity)entity);
        }
        return new HashMap<>();
    }

    public static Map<OriginLayer, Origin> get(PlayerEntity player) {
        return ModComponents.ORIGIN.get(player).getOrigins();
    }

}
