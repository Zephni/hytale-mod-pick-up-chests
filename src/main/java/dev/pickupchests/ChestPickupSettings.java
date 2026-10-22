package dev.pickupchests;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class ChestPickupSettings implements Component<EntityStore> {
    static final BuilderCodec<ChestPickupSettings> CODEC = BuilderCodec
            .builder(ChestPickupSettings.class, ChestPickupSettings::new)
            .append(new KeyedCodec<>("CarryInHands", Codec.BOOLEAN),
                    (settings, value) -> settings.carryInHands = value, settings -> settings.carryInHands).add()
            .append(new KeyedCodec<>("SecondaryAction", Codec.BOOLEAN),
                    (settings, value) -> settings.secondaryAction = value, settings -> settings.secondaryAction).add()
            .build();
    private static final ChestPickupSettings DEFAULT = new ChestPickupSettings();
    private static ComponentType<EntityStore, ChestPickupSettings> componentType;
    private boolean carryInHands = true;
    private boolean secondaryAction;

    ChestPickupSettings() {
    }

    ChestPickupSettings(boolean carryInHands, boolean secondaryAction) {
        this.carryInHands = carryInHands;
        this.secondaryAction = secondaryAction;
    }

    static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(ChestPickupSettings.class, "PickUpChestsSettings", CODEC);
    }

    static ComponentType<EntityStore, ChestPickupSettings> getComponentType() {
        return componentType;
    }

    static ChestPickupSettings get(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        var settings = accessor.getComponent(ref, componentType);
        return settings == null ? DEFAULT : settings;
    }

    boolean carryInHands() {
        return carryInHands;
    }

    boolean secondaryAction() {
        return secondaryAction;
    }

    InteractionType interactionType() {
        return secondaryAction ? InteractionType.Secondary : InteractionType.Use;
    }

    String bindingKey() {
        return secondaryAction ? "SecondaryItemAction" : "BlockInteractAction";
    }

    @Override
    public ChestPickupSettings clone() {
        return new ChestPickupSettings(carryInHands, secondaryAction);
    }
}
