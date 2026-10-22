package dev.pickupchests;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.modules.interaction.components.CarriedBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

final class ChestPickupHintSystem extends EntityTickingSystem<EntityStore> {
    static final String HUD_KEY = "PickUpChests.PickupHint";
    static final String UI_PATH = "Hud/PickUpChests/PickupHint.ui";

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                     CommandBuffer<EntityStore> commands) {
        var player = chunk.getComponent(index, Player.getComponentType());
        var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        var huds = player.getHudManager();
        var existing = huds.getCustomHud(HUD_KEY);
        var settings = ChestPickupSettings.get(commands, chunk.getReferenceTo(index));
        var carried = commands.getComponent(chunk.getReferenceTo(index), CarriedBlock.getComponentType());
        boolean carrying = ChestCarrySystem.isCarryingChest(carried);
        boolean visible = carrying;
        var ref = chunk.getReferenceTo(index);
        var target = TargetUtil.getTargetBlock(ref, ChestPickupSystem.PICKUP_RANGE, commands);
        if (carried == null && target != null) {
            var chunks = store.getExternalData().getWorld().getChunkStore();
            var sectionRef = chunks.getChunkSectionReferenceAtBlock(target.x, target.y, target.z);
            if (sectionRef != null && sectionRef.isValid()) {
                var section = chunks.getStore().getComponent(sectionRef, BlockSection.getComponentType());
                var block = section == null ? null
                        : BlockType.getAssetMap().getAsset(section.get(target.x, target.y, target.z));
                visible = shouldShow(block)
                        && store.getExternalData().getWorld().getGameplayConfig()
                                .getWorldConfig().isBlockBreakingAllowed(block);
            }
        }
        if (existing instanceof PickupHintHud hint) {
            hint.setState(visible, settings.secondaryAction(), settings.carryInHands(), carrying);
        } else if (visible) {
            huds.addCustomHud(playerRef, new PickupHintHud(playerRef, settings, carrying));
        }
    }

    static boolean shouldShow(BlockType block) {
        return ChestPickupSystem.isPickupChest(block);
    }

    static String hintText(boolean secondaryAction, boolean carryInHands, boolean carrying) {
        if (carrying) {
            return "Release crouch; use Secondary to place chest";
        }
        return "Crouch + " + (secondaryAction ? "Secondary" : "Interact")
                + (carryInHands ? " to carry chest" : " to pick up chest");
    }

    private static final class PickupHintHud extends CustomUIHud {
        private boolean visible = true;
        private boolean secondaryAction;
        private boolean carryInHands;
        private boolean carrying;

        PickupHintHud(PlayerRef playerRef, ChestPickupSettings settings, boolean carrying) {
            super(playerRef, HUD_KEY);
            secondaryAction = settings.secondaryAction();
            carryInHands = settings.carryInHands();
            this.carrying = carrying;
        }

        @Override
        protected void build(UICommandBuilder builder) {
            builder.append(UI_PATH);
            applyState(builder);
        }

        void setState(boolean next, boolean secondary, boolean carry, boolean nextCarrying) {
            if (visible != next || secondaryAction != secondary || carryInHands != carry || carrying != nextCarrying) {
                visible = next;
                secondaryAction = secondary;
                carryInHands = carry;
                carrying = nextCarrying;
                var builder = new UICommandBuilder();
                applyState(builder);
                update(false, builder);
            }
        }

        private void applyState(UICommandBuilder builder) {
            builder.set("#PickupHint.Visible", visible);
            builder.set("#PickupHint.Text", hintText(secondaryAction, carryInHands, carrying));
        }
    }
}
