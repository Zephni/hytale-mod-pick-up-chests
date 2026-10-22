package dev.pickupchests;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.ArrayList;
import java.util.List;

final class FilledChestItems {
    static final String PREFIX = "PickUpChests_Filled_";
    static final String ICON = "Icons/PickUpChests/FilledChest.png";
    static final int PREVIEW_LIMIT = 3;

    private FilledChestItems() {
    }

    static void onPlayerReady(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var world = ref.getStore().getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid() || ref.getStore().getExternalData().getWorld() != world) {
                return;
            }
            refreshInventory(InventoryComponent.getCombined(ref.getStore(), ref,
                    InventoryComponent.HOTBAR_STORAGE_BACKPACK));
        });
    }

    static void refreshInventory(ItemContainer inventory) {
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            var stack = inventory.getItemStack(slot);
            if (ItemStack.isEmpty(stack) || stack.getQuantity() != 1
                    || !ChestPickupSystem.isChest(stack.getItemId())) {
                continue;
            }
            var holder = stack.getFromMetadataOrNull(ItemStack.Metadata.BLOCK_HOLDER, ChunkStore.REGISTRY.getEntityCodec());
            if (holder == null) {
                continue;
            }
            var refreshed = decorate(stack, holder);
            if (!refreshed.equals(stack) && !inventory.setItemStackForSlot(slot, refreshed).succeeded()) {
                throw new IllegalStateException("Could not update legacy filled chest in inventory slot " + slot);
            }
        }
    }

    static void register() {
        if (!CommonAssetRegistry.hasCommonAsset(ICON)) {
            throw new IllegalStateException("Missing filled chest icon: " + ICON);
        }
        List<Item> variants = new ArrayList<>();
        for (var item : Item.getAssetMap().getAssetMap().values()) {
            if (ChestPickupSystem.isChest(item.getId())) {
                variants.add(new FilledItem(item));
            }
        }
        if (variants.isEmpty()) {
            throw new IllegalStateException("No chest items were loaded");
        }
        var result = Item.getAssetStore().loadAssets("MugHeadStudios:PickUpChests", variants);
        if (result.hasFailed()) {
            throw new IllegalStateException("Could not register filled chest items: " + result.getFailedToLoadKeys());
        }
    }

    static ItemStack decorate(ItemStack original, Holder<ChunkStore> holder) {
        var block = holder.getComponent(ItemContainerBlock.getComponentType());
        if (block == null || block.getItemContainer() == null) {
            return original;
        }
        var contents = block.getItemContainer();
        int occupied = 0;
        long quantity = 0;
        List<Message> preview = new ArrayList<>();
        for (short slot = 0; slot < contents.getCapacity(); slot++) {
            var content = contents.getItemStack(slot);
            if (ItemStack.isEmpty(content)) {
                continue;
            }
            occupied++;
            quantity += content.getQuantity();
            if (preview.size() < PREVIEW_LIMIT) {
                // Use the base item name rather than recursively previewing nested filled chests.
                preview.add(Message.join(Message.raw("\n" + content.getQuantity() + " x "),
                        content.getItem().getTranslationMessage()));
            }
        }
        if (occupied == 0) {
            return original;
        }
        String filledId = PREFIX + original.getItemId();
        if (Item.getAssetMap().getAsset(filledId) == null) {
            throw new IllegalStateException("Missing filled chest item: " + filledId);
        }
        var name = Message.join(original.getDisplayName(), Message.raw(" (Contains items)"));
        var description = Message.raw("Contains " + quantity + " items in " + occupied + "/"
                + contents.getCapacity() + " slots.");
        description.insertAll(preview);
        if (occupied > PREVIEW_LIMIT) {
            description.insert("\n... and " + (occupied - PREVIEW_LIMIT) + " more occupied slots.");
        }
        description.insert("\nPlace the chest to access its contents.");
        return new ItemStack(filledId, 1)
                .withMetadata(ItemStack.Metadata.BLOCK_HOLDER, ChunkStore.REGISTRY.serialize(holder))
                .withMetadata(ItemDisplayMetadata.KEYED_CODEC, new ItemDisplayMetadata(name, description));
    }

    private static final class FilledItem extends Item {
        FilledItem(Item original) {
            super(original);
            id = PREFIX + original.getId();
            icon = ICON;
            maxStack = 1;
            recipeToGenerate = null;
            invalidatePacketCache();
        }
    }
}
