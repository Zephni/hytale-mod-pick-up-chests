package dev.pickupchests;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.interaction.interaction.UnarmedInteractions;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class ChestPickupInput extends UnarmedInteractions {
    static final String ROOT = "PickUpChests_Secondary";

    private ChestPickupInput(UnarmedInteractions original) {
        id = original.getId();
        var bindings = new EnumMap<InteractionType, String>(InteractionType.class);
        bindings.putAll(original.getInteractions());
        bindings.putIfAbsent(InteractionType.Secondary, ROOT);
        bindings.putIfAbsent(InteractionType.Use, ROOT);
        interactions = Map.copyOf(bindings);
    }

    static boolean isCrouching(MovementStatesComponent movement, PlayerInput input) {
        if (input != null) {
            // Interaction packets can be consumed before queued movement is applied this tick.
            var updates = input.getMovementUpdateQueue();
            for (int index = updates.size() - 1; index >= 0; index--) {
                if (updates.get(index) instanceof PlayerInput.SetMovementStates update) {
                    return update.movementStates().crouching;
                }
            }
        }
        return movement.getMovementStates().crouching;
    }

    static void register() {
        var unarmed = UnarmedInteractions.getAssetMap().getAsset(UnarmedInteractions.DEFAULT_UNARMED_ID);
        if (unarmed == null) {
            throw new IllegalStateException("Default unarmed interactions are missing");
        }
        if (unarmed.getInteractions().containsKey(InteractionType.Secondary)
                && unarmed.getInteractions().containsKey(InteractionType.Use)) {
            return;
        }
        var result = AssetRegistry.getAssetStore(UnarmedInteractions.class)
                .loadAssets("MugHeadStudios:PickUpChests", List.of(new ChestPickupInput(unarmed)));
        if (result.hasFailed()) {
            throw new IllegalStateException("Could not register empty-hand pickup actions: " + result.getFailedToLoadKeys());
        }
    }
}
