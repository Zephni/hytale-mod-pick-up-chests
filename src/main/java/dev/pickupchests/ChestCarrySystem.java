package dev.pickupchests;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockPlacementSettings;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem;
import com.hypixel.hytale.server.core.modules.interaction.components.CarriedBlock;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionTypeUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.bson.BsonDocument;

final class ChestCarrySystem extends EntityTickingSystem<EntityStore> {
    static final String PLACE_ROOT = "Root_Carry_Place_Block";

    static void registerItems() {
        var items = new ArrayList<Item>();
        for (var item : Item.getAssetMap().getAssetMap().values()) {
            if (ChestPickupSystem.isChest(item.getId())) {
                items.add(new CarryItem(item));
            }
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("No chest items were loaded");
        }
        var result = Item.getAssetStore().loadAssets("MugHeadStudios:PickUpChests", items);
        if (result.hasFailed()) {
            throw new IllegalStateException("Could not register chest carry controls: " + result.getFailedToLoadKeys());
        }
    }

    static void registerPreviews() {
        // Item loading regenerates block states, so apply this after registering all item variants.
        var blocks = new ArrayList<BlockType>();
        for (var block : BlockType.getAssetMap().getAssetMap().values()) {
            if (ChestPickupSystem.isPickupChest(block)) {
                blocks.add(new PreviewBlock(block));
            }
        }
        if (blocks.isEmpty()) {
            throw new IllegalStateException("No chest blocks were loaded");
        }
        var blockResult = BlockType.getAssetStore().loadAssets("MugHeadStudios:PickUpChests", blocks);
        if (blockResult.hasFailed()) {
            throw new IllegalStateException("Could not register chest placement previews: "
                    + blockResult.getFailedToLoadKeys());
        }
    }

    private static final class PreviewBlock extends BlockType {
        PreviewBlock(BlockType original) {
            super(original);
            var settings = original.getPlacementSettings();
            var encoded = settings == null ? new BsonDocument()
                    : BlockPlacementSettings.CODEC.encode(settings, new ExtraInfo()).asDocument();
            encoded.put("BlockPreviewVisibility", BlockPlacementSettings.BlockPreviewVisibility.CODEC.encode(
                    BlockPlacementSettings.BlockPreviewVisibility.ALWAYS_VISIBLE, new ExtraInfo()));
            placementSettings = BlockPlacementSettings.CODEC.decode(encoded, new ExtraInfo());
        }
    }

    private static final class CarryItem extends Item {
        CarryItem(Item original) {
            super(original);
            carryInteractions = Map.of(
                    InteractionType.Secondary, PLACE_ROOT,
                    InteractionType.Use, ChestPickupInput.ROOT,
                    InteractionType.Primary, ChestPickupInput.ROOT);
            carryHudInputBindings = Map.of(InteractionType.Secondary, "client.hud.inputBindings.place");
            invalidatePacketCache();
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), CarriedBlock.getComponentType());
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, PlayerItemEntityPickupSystem.class));
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                     CommandBuffer<EntityStore> commands) {
        if (isCarryingChest(chunk.getComponent(index, CarriedBlock.getComponentType()))) {
            guardPickups(store, chunk.getReferenceTo(index));
        }
    }

    static boolean isCarryingChest(CarriedBlock carried) {
        return carried != null && carried.getBlockTypeKey() != null && ChestPickupSystem.isPickupChest(
                BlockType.getAssetMap().getAsset(carried.getBlockTypeKey()));
    }

    static boolean blocksInteraction(InteractionType type, String root, boolean crouching) {
        return !(type == InteractionType.Secondary && PLACE_ROOT.equals(root) && !crouching)
                && (InteractionTypeUtils.STANDARD_INPUT.contains(type)
                || "Root_Carry_Dropped_Block".equals(root) || "Root_Throw_Carryable_Block".equals(root));
    }

    static void guardPickups(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        guardPickupOrder(accessor, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        guardPickupOrder(accessor, ref, InventoryComponent.STORAGE_HOTBAR_BACKPACK);
        guardPickupOrder(accessor, ref, InventoryComponent.BACKPACK_STORAGE_HOTBAR);
    }

    private static void guardPickupOrder(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
            ComponentType<EntityStore, ? extends InventoryComponent>[] order) {
        var inventory = InventoryComponent.getCombined(accessor, ref, order);
        if (!(inventory instanceof GuardedInventory)) {
            var guarded = new GuardedInventory(inventory,
                    () -> ref.isValid() && isCarryingChest(
                            ref.getStore().getComponent(ref, CarriedBlock.getComponentType())));
            accessor.ensureAndGetComponent(ref, InventoryComponent.Combined.getComponentType())
                    .getInventories().put(order, guarded);
        }
    }

    // Guard the transient combined views, leaving real inventories, filters and saved data untouched.
    static final class GuardedInventory extends CombinedItemContainer {
        private final BooleanSupplier blocked;

        GuardedInventory(ItemContainer original, BooleanSupplier blocked) {
            // Combined views forward updates recursively; a delegate around one stops that propagation.
            super(original);
            this.blocked = blocked;
        }

        @Override
        protected boolean cantAddToSlot(short slot, ItemStack item, ItemStack existing) {
            return blocked.getAsBoolean() || super.cantAddToSlot(slot, item, existing);
        }
    }

    static final class InteractivePickupGuard extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
        InteractivePickupGuard() {
            super(InteractivelyPickupItemEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(PlayerRef.getComponentType(), CarriedBlock.getComponentType());
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, InteractivelyPickupItemEvent event) {
            if (!event.isCancelled() && isCarryingChest(chunk.getComponent(index, CarriedBlock.getComponentType()))) {
                event.setCancelled(true);
                chunk.getComponent(index, PlayerRef.getComponentType())
                        .sendMessage(Message.raw("Put down the chest before picking up other items."));
            }
        }

    }

    static final class DropGuard extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {
        DropGuard() {
            super(DropItemEvent.PlayerRequest.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(PlayerRef.getComponentType(), CarriedBlock.getComponentType());
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, DropItemEvent.PlayerRequest event) {
            if (!event.isCancelled() && isCarryingChest(chunk.getComponent(index, CarriedBlock.getComponentType()))) {
                event.setCancelled(true);
                chunk.getComponent(index, PlayerRef.getComponentType())
                        .sendMessage(Message.raw("Place the chest down before dropping items."));
            }
        }
    }
}
