package dev.pickupchests;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

public final class PickUpChestsPlugin extends JavaPlugin {
    public PickUpChestsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ChestPickupSettings.register(getEntityStoreRegistry());
        getEntityStoreRegistry().registerSystem(new ChestPickupSystem());
        getEntityStoreRegistry().registerSystem(new ChestPickupHintSystem());
        getEntityStoreRegistry().registerSystem(new ChestCarrySystem());
        getEntityStoreRegistry().registerSystem(new ChestCarrySystem.InteractivePickupGuard());
        getEntityStoreRegistry().registerSystem(new ChestCarrySystem.DropGuard());
        getCommandRegistry().registerCommand(new ChestPickupSettingsCommand());
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, FilledChestItems::onPlayerReady);
    }

    @Override
    protected void start() {
        ChestCarrySystem.registerItems();
        FilledChestItems.register();
        ChestPickupInput.register();
        ChestCarrySystem.registerPreviews();
    }
}
