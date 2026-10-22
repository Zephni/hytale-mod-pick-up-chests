package dev.pickupchests;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockEntity;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.provider.EmptyChunkStorageProvider;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.resources.EmptyResourceStorageProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.joml.Vector3i;

final class ChestBreakTest {
    static void run() {
        var config = new WorldConfig();
        config.setWorldGenProvider(new VoidWorldGenProvider());
        config.setChunkStorageProvider(new EmptyChunkStorageProvider());
        config.setResourceStorageProvider(new EmptyResourceStorageProvider());
        config.setSavingConfig(false);
        config.setSavingPlayers(false);
        config.setCanSaveChunks(false);
        config.setSpawningNPC(false);
        config.setIsSpawnMarkersEnabled(false);
        config.setTicking(false);
        var universe = Universe.get();
        var name = "pickupchests-break-test";
        var world = universe.makeWorld(name, universe.getWorldsPath().resolve(name), config).join();
        try {
            var sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlockAsync(8, 64, 8).join();
            var done = new CompletableFuture<Void>();
            world.execute(() -> {
                try {
                    var chunks = world.getChunkStore();
                    var blocks = chunks.getStore();
                    blocks.tryRemoveComponent(sectionRef, ChunkStore.REGISTRY.getNonTickingComponentType());
                    var entities = world.getEntityStore().getStore();
                    var position = new Vector3i(8, 64, 8);
                    for (var id : new String[]{"Furniture_Crude_Chest_Small", "Furniture_Crude_Chest_Large"}) {
                        for (boolean filled : new boolean[]{false, true}) {
                            var block = BlockType.getAssetMap().getAsset(id);
                            int blockId = BlockType.getAssetMap().getIndex(id);
                            assert BlockOperations.setBlock(chunks, sectionRef, 8, 64, 8,
                                    blockId, block, 0, 0, 0);
                            var components = blocks.getComponent(sectionRef, BlockComponentSection.getComponentType());
                            var containerRef = BlockEntity.ensureBlockEntity(blocks, sectionRef, components,
                                    8, 64, 8, block, 0);
                            var container = blocks.getComponent(containerRef, ItemContainerBlock.getComponentType());
                            var payload = new ItemStack("Wood_Softwood_Trunk", 7);
                            if (filled) {
                                assert container.getItemContainer().setItemStackForSlot((short) 0, payload).succeeded();
                            }
                            var breaking = block.getGathering().getBreaking();
                            BlockHarvestUtils.performBlockBreak(new Vector3i(position), block, null,
                                    breaking.getQuantity(), breaking.getItemId(), breaking.getDropListId(),
                                    0, null, sectionRef, entities, blocks);
                            assert blocks.getComponent(sectionRef, BlockSection.getComponentType()).get(8, 64, 8) == 0
                                    : "Chest was not broken: " + id;
                            world.consumeTaskQueue();
                            var drops = new ArrayList<ItemStack>();
                            entities.forEachChunk(ItemComponent.getComponentType(), (chunk, commands) -> {
                                for (int index = 0; index < chunk.size(); index++) {
                                    drops.add(chunk.getComponent(index, ItemComponent.getComponentType()).getItemStack());
                                    commands.removeEntity(chunk.getReferenceTo(index),
                                            com.hypixel.hytale.component.RemoveReason.REMOVE);
                                }
                            });
                            var chestDrops = drops.stream()
                                    .filter(stack -> !stack.getItemId().equals(payload.getItemId())).toList();
                            assert chestDrops.equals(BlockHarvestUtils.getDrops(block, breaking.getQuantity(),
                                    breaking.getItemId(), breaking.getDropListId()))
                                    : "Chest drop lost: " + id + " " + drops;
                            assert drops.stream().filter(stack -> stack.getItemId().equals(payload.getItemId()))
                                    .mapToInt(ItemStack::getQuantity).sum() == (filled ? 7 : 0)
                                    : "Contents lost: " + id + " " + drops;
                        }
                    }
                    done.complete(null);
                } catch (Throwable error) {
                    done.completeExceptionally(error);
                }
            });
            done.orTimeout(30, TimeUnit.SECONDS).join();
        } finally {
            universe.removeWorld(name);
        }
    }
}
