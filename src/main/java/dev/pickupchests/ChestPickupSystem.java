package dev.pickupchests;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockEntity;
import com.hypixel.hytale.server.core.modules.block.BlockCarriedEvent;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.event.InteractionChainStartEvent;
import com.hypixel.hytale.server.core.modules.interaction.components.CarriedBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.Set;
import org.joml.Vector3i;

final class ChestPickupSystem extends EntityEventSystem<EntityStore, InteractionChainStartEvent> {
    static final String NO_SPACE_MESSAGE = "Make room in your hotbar or inventory first.";
    static final double PICKUP_RANGE = 5.0;
    static final int PICKUP_BLOCK_FLAGS = SetBlockSettings.NO_DROP_ITEMS | SetBlockSettings.NO_FIRE_ON_BREAK
            | SetBlockSettings.NO_SEND_PARTICLES | SetBlockSettings.NO_SEND_AUDIO;

    ChestPickupSystem() {
        super(InteractionChainStartEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), MovementStatesComponent.getComponentType());
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.lastSet();
    }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commands, InteractionChainStartEvent event) {
        if (event.isCancelled()) {
            return;
        }
        var playerRef = chunk.getReferenceTo(index);
        var player = chunk.getComponent(index, PlayerRef.getComponentType());
        var carried = commands.getComponent(playerRef, CarriedBlock.getComponentType());
        var settings = ChestPickupSettings.get(commands, playerRef);
        var movement = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (carried != null) {
            if (ChestCarrySystem.isCarryingChest(carried)
                    && ChestCarrySystem.blocksInteraction(event.getType(), event.getRootInteractionId(),
                            movement.getMovementStates().crouching)) {
                reject(event, player, "Put down the chest before using your hands or picking up other items.");
            }
            return;
        }
        if (!isPickupGesture(event.getType(), movement.getMovementStates().crouching, settings)) {
            return;
        }
        var target = TargetUtil.getTargetBlock(playerRef, PICKUP_RANGE, commands);
        if (target == null) {
            return;
        }
        var world = store.getExternalData().getWorld();
        var chunks = world.getChunkStore();
        var blocks = chunks.getStore();
        var position = new Vector3i(target);
        var sectionRef = chunks.getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            reject(event, player, "That chest is not loaded. Try again in a moment.");
            return;
        }
        var section = blocks.getComponent(sectionRef, BlockSection.getComponentType());
        var blockType = BlockType.getAssetMap().getAsset(section.get(position.x, position.y, position.z));
        if (!isPickupChest(blockType)) {
            return;
        }
        event.setCancelled(true);
        // Commit sequentially: queued component additions are invisible to other chains in this tick.
        commands.run(next -> pickup(next, playerRef, event, position));
    }

    private static void pickup(Store<EntityStore> commands, Ref<EntityStore> playerRef,
                               InteractionChainStartEvent event, Vector3i position) {
        if (!playerRef.isValid()) {
            return;
        }
        var player = commands.getComponent(playerRef, PlayerRef.getComponentType());
        if (commands.getComponent(playerRef, CarriedBlock.getComponentType()) != null) {
            reject(event, player, "Put down the chest before picking up another chest.");
            return;
        }
        var settings = ChestPickupSettings.get(commands, playerRef);
        var world = commands.getExternalData().getWorld();
        var chunks = world.getChunkStore();
        var blocks = chunks.getStore();
        var sectionRef = chunks.getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            reject(event, player, "That chest is not loaded. Try again in a moment.");
            return;
        }
        var section = blocks.getComponent(sectionRef, BlockSection.getComponentType());
        var blockType = BlockType.getAssetMap().getAsset(section.get(position.x, position.y, position.z));
        if (!isPickupChest(blockType)) {
            reject(event, player, "The chest changed during pickup. Please try again.");
            return;
        }
        int filler = section.getFiller(position.x, position.y, position.z);
        position.sub(FillerBlockUtil.unpackX(filler), FillerBlockUtil.unpackY(filler),
                FillerBlockUtil.unpackZ(filler));
        sectionRef = chunks.getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            reject(event, player, "That chest is not loaded. Try again in a moment.");
            return;
        }
        section = blocks.getComponent(sectionRef, BlockSection.getComponentType());
        var components = blocks.getComponent(sectionRef, BlockComponentSection.getComponentType());
        if (components == null) {
            reject(event, player, "That chest has no loaded container data.");
            return;
        }
        int blockIndex = ChunkUtil.indexBlock(position.x, position.y, position.z);
        var blockRef = components.getBlockReference(blockIndex);
        var storedHolder = components.getBlockHolder(blockIndex);
        var liveContainer = blockRef != null && blockRef.isValid()
                ? blocks.getComponent(blockRef, ItemContainerBlock.getComponentType())
                : storedHolder == null ? null : storedHolder.getComponent(ItemContainerBlock.getComponentType());
        if (liveContainer == null) {
            reject(event, player, "That chest has no loaded container data.");
            return;
        }
        if (!liveContainer.getWindows().isEmpty()) {
            reject(event, player, "Close the chest before picking it up.");
            return;
        }
        if (!world.getGameplayConfig().getWorldConfig().isBlockBreakingAllowed(blockType)) {
            reject(event, player, "Picking up chests is not allowed in this world.");
            return;
        }

        var useEvent = new UseBlockEvent.Pre(InteractionType.Use, event.getContext(),
                new Vector3i(position), blockType);
        commands.invoke(playerRef, useEvent);
        if (useEvent.isCancelled()) {
            reject(event, player, "You cannot pick up that chest here.");
            return;
        }
        int originalBlock = section.get(position.x, position.y, position.z);
        int rotation = section.getRotationIndex(position.x, position.y, position.z);
        var breakEvent = new BreakBlockEvent(event.getContext().getHeldItem(),
                new Vector3i(position), blockType);
        commands.invoke(playerRef, breakEvent);
        if (breakEvent.isCancelled() || !position.equals(breakEvent.getTargetBlock())) {
            reject(event, player, "You cannot pick up that chest here.");
            return;
        }

        if (section.get(position.x, position.y, position.z) != originalBlock
                || !java.util.Objects.equals(blockRef, components.getBlockReference(blockIndex))
                || storedHolder != components.getBlockHolder(blockIndex)
                || !liveContainer.getWindows().isEmpty()
                || commands.getComponent(playerRef, CarriedBlock.getComponentType()) != null) {
            reject(event, player, "The chest changed during pickup. Please try again.");
            return;
        }
        Holder<ChunkStore> snapshot = blockRef != null && blockRef.isValid()
                ? blocks.copyEntity(blockRef) : components.copyBlockHolder(blockIndex);
        var container = snapshot == null ? null : snapshot.getComponent(ItemContainerBlock.getComponentType());
        if (container == null) {
            reject(event, player, "The chest data changed during pickup. Please try again.");
            return;
        }
        container.setStayOpenWhenEmpty(false);
        ItemContainer inventory = null;
        ItemStack packed = null;
        short slot = -1;
        if (settings.carryInHands()) {
            blocks.invoke(snapshot, new BlockCarriedEvent(playerRef));
            commands.addComponent(playerRef, CarriedBlock.getComponentType(),
                    new CarriedBlock(blockType.getId(), snapshot, null));
        } else {
            packed = pack(blockType.getId(), snapshot);
            if (packed == null) {
                reject(event, player, "That chest cannot be converted into an item.");
                return;
            }
            inventory = InventoryComponent.getCombined(commands, playerRef, InventoryComponent.HOTBAR_FIRST);
            slot = findEmptySlot(inventory, packed);
            if (slot < 0) {
                reject(event, player, NO_SPACE_MESSAGE);
                return;
            }
            if (!inventory.setItemStackForSlot(slot, packed).succeeded()) {
                reject(event, player, "The chest could not be added to your inventory.");
                return;
            }
        }

        event.setCancelled(true);
        Holder<ChunkStore> detached = null;
        try {
            // UNLOAD detaches the container without spilling its contents.
            detached = BlockEntity.takeBlockEntity(blocks, components, position.x, position.y, position.z);
            BlockHarvestUtils.performBlockBreak(playerRef, event.getContext().getHeldItem(), position,
                    PICKUP_BLOCK_FLAGS, sectionRef, commands, blocks);
        } finally {
            if (section.get(position.x, position.y, position.z) == originalBlock) {
                if (detached != null) {
                    BlockEntity.setBlockEntity(blocks, sectionRef, components,
                            position.x, position.y, position.z, blockType, rotation, detached);
                }
                if (settings.carryInHands()) {
                    commands.tryRemoveComponent(playerRef, CarriedBlock.getComponentType());
                } else if (!inventory.removeItemStackFromSlot(slot, packed, 1).succeeded()) {
                    throw new IllegalStateException("Could not roll back chest pickup inventory transaction");
                }
            }
        }
        if (section.get(position.x, position.y, position.z) == originalBlock) {
            reject(event, player, "The chest could not be removed. Nothing was picked up.");
            return;
        }
        if (settings.carryInHands()) {
            ChestCarrySystem.guardPickups(commands, playerRef);
            player.sendMessage(Message.raw(
                    "Carrying chest. Release crouch and use Secondary to place it before picking up anything else."));
        } else {
            player.sendMessage(Message.raw("Chest picked up with its contents. Place it like a normal block."));
        }
    }

    static boolean isChest(String itemId) {
        return itemId.startsWith("Furniture_")
                && (itemId.contains("_Chest_") || itemId.contains("_ChestStolen_"));
    }

    static boolean isPickupChest(BlockType blockType) {
        var item = blockType == null ? null : blockType.getItem();
        return item != null && isChest(item.getId())
                && BlockEntity.declaresComponent(blockType, ItemContainerBlock.getComponentType());
    }

    static boolean isPickupGesture(InteractionType type, boolean crouching, ChestPickupSettings settings) {
        return type == settings.interactionType() && crouching;
    }

    static ItemStack pack(String blockId, Holder<ChunkStore> holder) {
        var stack = new CarriedBlock(blockId, holder, null).toItemStack();
        return stack == null ? null : FilledChestItems.decorate(stack, holder);
    }

    static short findEmptySlot(ItemContainer inventory, ItemStack stack) {
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            if (ItemStack.isEmpty(inventory.getItemStack(slot))
                    && inventory.canAddItemStackToSlot(slot, stack, true, true)) {
                return slot;
            }
        }
        return -1;
    }

    private static void reject(InteractionChainStartEvent event, PlayerRef player, String message) {
        event.setCancelled(true);
        player.sendMessage(Message.raw(message));
    }
}
