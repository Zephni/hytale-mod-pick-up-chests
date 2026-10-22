package dev.pickupchests;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.components.CarriedBlock;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class ChestPickupSettingsCommand extends AbstractPlayerCommand {
    static final String UI_PATH = "Pages/PickUpChests/Settings.ui";

    ChestPickupSettingsCommand() {
        super("pickupchests", "Choose how to pick up chests and which action to use.");
        requireNoPermission();
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef playerRef, World world) {
        store.getComponent(ref, Player.getComponentType()).getPageManager()
                .openCustomPage(ref, store, new SettingsPage(playerRef));
    }

    static final class SettingsEvent {
        static final BuilderCodec<SettingsEvent> CODEC = BuilderCodec
                .builder(SettingsEvent.class, SettingsEvent::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (event, value) -> event.action = value, event -> event.action).add()
                .build();
        private String action;
    }

    private static final class SettingsPage extends InteractiveCustomUIPage<SettingsEvent> {
        SettingsPage(PlayerRef playerRef) {
            super(playerRef, CustomPageLifetime.CanDismiss, SettingsEvent.CODEC);
        }

        @Override
        public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events,
                          Store<EntityStore> store) {
            commands.append(UI_PATH);
            updateSelection(commands, ChestPickupSettings.get(store, ref));
            for (String action : new String[]{"Inventory", "Carry", "Interact", "Secondary", "Close"}) {
                events.addEventBinding(CustomUIEventBindingType.Activating, "#" + action,
                        EventData.of("Action", action));
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SettingsEvent event) {
            if ("Close".equals(event.action)) {
                close();
                return;
            }
            if (ChestCarrySystem.isCarryingChest(store.getComponent(ref, CarriedBlock.getComponentType()))) {
                playerRef.sendMessage(Message.raw("Put down the chest before changing pickup settings."));
                sendUpdate();
                return;
            }
            var current = ChestPickupSettings.get(store, ref);
            var next = switch (event.action == null ? "" : event.action) {
                case "Inventory" -> new ChestPickupSettings(false, current.secondaryAction());
                case "Carry" -> new ChestPickupSettings(true, current.secondaryAction());
                case "Interact" -> new ChestPickupSettings(current.carryInHands(), false);
                case "Secondary" -> new ChestPickupSettings(current.carryInHands(), true);
                default -> null;
            };
            if (next == null) {
                playerRef.sendMessage(Message.raw("Unknown pickup setting. Reopen /pickupchests and try again."));
                sendUpdate();
                return;
            }
            store.putComponent(ref, ChestPickupSettings.getComponentType(), next);
            var commands = new UICommandBuilder();
            updateSelection(commands, next);
            sendUpdate(commands);
        }

        private static void updateSelection(UICommandBuilder commands, ChestPickupSettings settings) {
            commands.set("#Inventory.Text", settings.carryInHands() ? "Inventory" : "Inventory (selected)");
            commands.set("#Carry.Text", settings.carryInHands() ? "Carry in hands (selected)" : "Carry in hands");
            commands.set("#Interact.Text", settings.secondaryAction()
                    ? "Crouch + Interact" : "Crouch + Interact (selected)");
            commands.set("#Secondary.Text", settings.secondaryAction()
                    ? "Crouch + Secondary (selected)" : "Crouch + Secondary");
        }
    }
}
