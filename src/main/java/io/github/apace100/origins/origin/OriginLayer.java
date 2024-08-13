package io.github.apace100.origins.origin;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.apace100.apoli.condition.factory.ConditionTypeFactory;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.data.CompoundSerializableDataType;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.data.OriginsDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class OriginLayer implements Comparable<OriginLayer> {

    public static final CompoundSerializableDataType<OriginLayer> DATA_TYPE = SerializableDataType.compound(
        new SerializableData()
            .add("id", SerializableDataTypes.IDENTIFIER)
            .addSupplied("order", SerializableDataTypes.INT, OriginLayerManager::size)
            .add("origins", OriginsDataTypes.ORIGINS_OR_CONDITIONED_ORIGINS)
            .add("replace", SerializableDataTypes.BOOLEAN, false)
            .add("enabled", SerializableDataTypes.BOOLEAN, true)
            .add("name", ApoliDataTypes.DEFAULT_TRANSLATABLE_TEXT, null)
            .add("gui_title", OriginsDataTypes.GUI_TITLE, null)
            .add("missing_name", ApoliDataTypes.DEFAULT_TRANSLATABLE_TEXT, null)
            .add("missing_description", ApoliDataTypes.DEFAULT_TRANSLATABLE_TEXT, null)
            .add("allow_random", SerializableDataTypes.BOOLEAN, false)
            .add("allow_random_unchoosable", SerializableDataTypes.BOOLEAN, false)
            .add("exclude_random", SerializableDataTypes.IDENTIFIERS, new LinkedList<>())
            .add("replace_exclude_random", SerializableDataTypes.BOOLEAN, false)
            .add("default_origin", SerializableDataTypes.IDENTIFIER, null)
            .add("auto_choose", SerializableDataTypes.BOOLEAN, false)
            .add("hidden", SerializableDataTypes.BOOLEAN, false)
            .postProcessor(data -> {

                Identifier id = data.get("id");
                String baseKey = Util.createTranslationKey("layer", id);

                if (!data.isPresent("name")) {
                    data.set("name", Text.translatable(baseKey + ".name"));
                }

                if (!data.isPresent("gui_title")) {

                    Text name = data.get("name");

                    Text viewOriginText = Text.translatable(Origins.MODID + ".gui.view_origin.title", name);
                    Text chooseOriginText = Text.translatable(Origins.MODID + ".gui.choose_origin.title", name);

                    data.set("gui_title", new GuiTitle(viewOriginText, chooseOriginText));

                }

                else {

                    GuiTitle guiTitle = data.get("gui_title");

                    Text viewOriginText = Optional.ofNullable(guiTitle.viewOrigin()).orElseGet(() -> Text.translatable(baseKey + ".view_origin.name"));
                    Text chooseOriginText = Optional.ofNullable(guiTitle.chooseOrigin()).orElseGet(() -> Text.translatable(baseKey + ".choose_origin.name"));

                    data.set("gui_title", new GuiTitle(viewOriginText, chooseOriginText));

                }

            }),
        data -> new OriginLayer(
            data.get("id"),
            data.get("order"),
            data.get("origins"),
            data.get("replace"),
            data.get("enabled"),
            data.get("name"),
            data.get("gui_title"),
            data.get("missing_name"),
            data.get("missing_description"),
            data.get("allow_random"),
            data.get("allow_random_unchoosable"),
            data.get("exclude_random"),
            data.get("replace_exclude_random"),
            data.get("default_origin"),
            data.get("auto_choose"),
            data.get("hidden")
        ),
        (layer, data) -> data
            .set("id", layer.getId())
            .set("order", layer.getOrder())
            .set("origins", layer.getConditionedOrigins())
            .set("replace", layer.shouldReplace())
            .set("enabled", layer.isEnabled())
            .set("name", layer.getName())
            .set("gui_title", layer.guiTitle)
            .set("missing_name", layer.getMissingName())
            .set("missing_description", layer.getMissingDescription())
            .set("allow_random", layer.isRandomAllowed())
            .set("allow_random_unchoosable", layer.unchoosableRandomAllowed)
            .set("exclude_random", layer.originsExcludedFromRandom)
            .set("replace_exclude_random", layer.replaceOriginsExcludedFromRandom)
            .set("default_origin", layer.getDefaultOrigin())
            .set("auto_choose", layer.shouldAutoChoose())
            .set("hidden", layer.isHidden())
    );

    private final Identifier id;
    private final int order;

    private final List<ConditionedOrigin> origins;
    private final boolean replaceOrigins;

    private final boolean enabled;

    private final Text name;
    private final GuiTitle guiTitle;

    @Nullable
    private final Text missingName;
    @Nullable
    private final Text missingDescription;

    private final boolean randomAllowed;
    private final boolean unchoosableRandomAllowed;

    private final List<Identifier> originsExcludedFromRandom;
    private final boolean replaceOriginsExcludedFromRandom;

    @Nullable
    private final Identifier defaultOrigin;
    private final boolean autoChoose;

    private final boolean hidden;

    protected OriginLayer(Identifier id, int order, List<ConditionedOrigin> origins, boolean replaceOrigins, boolean enabled, Text name, GuiTitle guiTitle, @Nullable Text missingName, @Nullable Text missingDescription, boolean randomAllowed, boolean unchoosableRandomAllowed, List<Identifier> originsExcludedFromRandom, boolean replaceOriginsExcludedFromRandom, @Nullable Identifier defaultOrigin, boolean autoChoose, boolean hidden) {
        this.id = id;
        this.order = order;
        this.origins = origins;
        this.replaceOrigins = replaceOrigins;
        this.enabled = enabled;
        this.name = name;
        this.guiTitle = guiTitle;
        this.missingName = missingName;
        this.missingDescription = missingDescription;
        this.randomAllowed = randomAllowed;
        this.unchoosableRandomAllowed = unchoosableRandomAllowed;
        this.originsExcludedFromRandom = originsExcludedFromRandom;
        this.replaceOriginsExcludedFromRandom = replaceOriginsExcludedFromRandom;
        this.defaultOrigin = defaultOrigin;
        this.autoChoose = autoChoose;
        this.hidden = hidden;
    }

    public int getOrder() {
        return order;
    }

    public List<ConditionedOrigin> getConditionedOrigins() {
        return origins;
    }

    public Text getName() {
        return name;
    }

    @Nullable
    public Text getMissingName() {
        return missingName;
    }

    @Nullable
    public Text getMissingDescription() {
        return missingDescription;
    }

    public Text getViewOriginTitle() {
        return guiTitle.viewOrigin();
    }

    public Text getChooseOriginTitle() {
        return guiTitle.chooseOrigin();
    }

    public boolean shouldReplace() {
        return replaceOrigins;
    }

    public boolean shouldReplaceExcludedOriginsFromRandom() {
        return replaceOriginsExcludedFromRandom;
    }

    public Identifier getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasDefaultOrigin() {
        return this.getDefaultOrigin() != null;
    }

    @Nullable
    public Identifier getDefaultOrigin() {
        return defaultOrigin;
    }

    public boolean shouldAutoChoose() {
        return autoChoose;
    }

    public List<Identifier> getOrigins() {
        return getOrigins(null);
    }

    public List<Identifier> getOrigins(@Nullable PlayerEntity playerEntity) {
        return origins
            .stream()
            .filter(co -> playerEntity == null || co.isConditionFulfilled(playerEntity))
            .flatMap(co -> co.origins.stream())
            .filter(OriginRegistry::contains)
            .collect(Collectors.toList());
    }

    public int getOriginOptionCount(PlayerEntity playerEntity) {

        int choosableOrigins = (int) getOrigins(playerEntity)
            .stream()
            .map(OriginRegistry::get)
            .filter(Origin::isChoosable)
            .count();

        if (choosableOrigins > 1 && (randomAllowed && !getRandomOrigins(playerEntity).isEmpty())) {
            choosableOrigins++;
        }

        return choosableOrigins;

    }

    public boolean contains(Identifier originId) {
        return origins
            .stream()
            .flatMap(co -> co.origins().stream())
            .anyMatch(originId::equals);
    }

    public boolean contains(Origin origin) {
        return contains(origin.getId());
    }

    public boolean contains(Identifier originId, PlayerEntity playerEntity) {
        return origins
            .stream()
            .filter(co -> co.isConditionFulfilled(playerEntity))
            .flatMap(co -> co.origins().stream())
            .anyMatch(originId::equals);
    }

    public boolean contains(Origin origin, PlayerEntity playerEntity) {
        return contains(origin.getId(), playerEntity);
    }

    public boolean isRandomAllowed() {
        return randomAllowed;
    }

    public boolean isHidden() {
        return hidden;
    }

    public List<Identifier> getRandomOrigins(PlayerEntity playerEntity) {
        return origins
            .stream()
            .filter(co -> co.isConditionFulfilled(playerEntity))
            .flatMap(co -> co.origins.stream())
            .filter(OriginRegistry::contains)
            .filter(oId -> !originsExcludedFromRandom.contains(oId))
            .filter(oid -> unchoosableRandomAllowed || OriginRegistry.get(oid).isChoosable())
            .collect(Collectors.toList());
    }

    public OriginLayer merge(OriginLayer otherLayer) {

        Set<ConditionedOrigin> origins = new LinkedHashSet<>(this.getConditionedOrigins());
        Set<Identifier> originsExcludedFromRandom = new LinkedHashSet<>(this.originsExcludedFromRandom);

        if (otherLayer.shouldReplace()) {
            origins.clear();
        }

        if (otherLayer.shouldReplaceExcludedOriginsFromRandom()) {
            originsExcludedFromRandom.clear();
        }

        origins.addAll(otherLayer.getConditionedOrigins());
        originsExcludedFromRandom.addAll(otherLayer.originsExcludedFromRandom);

        return new OriginLayer(
            this.getId(),
            otherLayer.getOrder(),
            new LinkedList<>(origins),
            otherLayer.shouldReplace(),
            otherLayer.isEnabled(),
            otherLayer.getName(),
            otherLayer.guiTitle,
            otherLayer.getMissingName(),
            otherLayer.getMissingDescription(),
            otherLayer.isRandomAllowed(),
            otherLayer.unchoosableRandomAllowed,
            new LinkedList<>(originsExcludedFromRandom),
            otherLayer.shouldReplaceExcludedOriginsFromRandom(),
            otherLayer.getDefaultOrigin(),
            otherLayer.shouldAutoChoose(),
            otherLayer.isHidden()
        );

    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof OriginLayer that && Objects.equals(this.getId(), that.getId()));
    }

    @Override
    public int compareTo(OriginLayer that) {
        return Integer.compare(this.order, that.order);
    }

    @Override
    public String toString() {
        return "OriginLayer{" +
            "id=" + id +
            ", order=" + order +
            ", origins=" + getOrigins() +
            ", replaceOrigins=" + replaceOrigins +
            ", enabled=" + enabled +
            ", name=" + name +
            ", guiTitle=" + guiTitle +
            ", missingName=" + missingName +
            ", missingDescription=" + missingDescription +
            ", randomAllowed=" + randomAllowed +
            ", unchoosableRandomAllowed=" + unchoosableRandomAllowed +
            ", originsExcludedFromRandom=" + originsExcludedFromRandom +
            ", replaceOriginsExcludedFromRandom=" + replaceOriginsExcludedFromRandom +
            ", defaultOrigin=" + defaultOrigin +
            ", autoChoose=" + autoChoose +
            ", hidden=" + hidden +
            '}';
    }

    public static OriginLayer fromJson(Identifier id, JsonObject jsonObject) {
        jsonObject.addProperty("id", id.toString());
        return DATA_TYPE.strictParse(Calio.wrapRegistryOps(JsonOps.INSTANCE), jsonObject);
    }

    public record GuiTitle(@Nullable Text viewOrigin, @Nullable Text chooseOrigin) {

        public static final CompoundSerializableDataType<GuiTitle> DATA_TYPE = SerializableDataType.compound(
            new SerializableData()
                .add("view_origin", SerializableDataTypes.TEXT, null)
                .add("choose_origin", SerializableDataTypes.TEXT, null),
            data -> new GuiTitle(
                data.get("view_origin"),
                data.get("choose_origin")
            ),
            (guiTitle, data) -> data
                .set("view_origin", guiTitle.viewOrigin())
                .set("choose_origin", guiTitle.viewOrigin)
        );

    }

    public record ConditionedOrigin(@Nullable ConditionTypeFactory<Entity>.Instance condition, List<Identifier> origins) {

        public static final CompoundSerializableDataType<ConditionedOrigin> DATA_TYPE = SerializableDataType.compound(
            new SerializableData()
                .add("condition", ApoliDataTypes.ENTITY_CONDITION, null)
                .add("origins", SerializableDataTypes.IDENTIFIERS),
            data -> new ConditionedOrigin(
                data.get("condition"),
                data.get("origins")
            ),
            (conditionedOrigin, data) -> data
                .set("condition", conditionedOrigin.condition())
                .set("origins", conditionedOrigin.origins())
        );

        public boolean isConditionFulfilled(PlayerEntity playerEntity) {
            return condition == null || condition.test(playerEntity);
        }

    }

}
