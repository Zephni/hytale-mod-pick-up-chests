package dev.pickupchests;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.interaction.interaction.UnarmedInteractions;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.components.CarriedBlock;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.bson.BsonDocument;
import org.bson.BsonString;

public final class ChestPickupTest extends JavaPlugin {
    public ChestPickupTest(JavaPluginInit init) {
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
    }

    @Override
    protected void start() {
        var nativeInteractions = java.util.Map.copyOf(Interaction.getAssetMap().getAssetMap());
        var nativeRoots = java.util.Map.copyOf(RootInteraction.getAssetMap().getAssetMap());
        var nativeBlocks = java.util.Map.copyOf(BlockType.getAssetMap().getAssetMap());
        var originalChest = Item.getAssetMap().getAsset("Furniture_Crude_Chest_Small");
        ChestCarrySystem.registerItems();
        assertNativeAssets(nativeInteractions, nativeRoots, nativeBlocks);
        var carryChest = Item.getAssetMap().getAsset(originalChest.getId());
        assert carryChest.getInteractions().equals(originalChest.getInteractions());
        assert carryChest.getIcon().equals(originalChest.getIcon());
        assert carryChest.getPlayerAnimationsId().equals(originalChest.getPlayerAnimationsId());
        assert carryChest.getCarryInteractions().get(InteractionType.Secondary).equals(ChestCarrySystem.PLACE_ROOT);
        assert carryChest.getCarryInteractions().get(InteractionType.Use).equals(ChestPickupInput.ROOT);
        assert carryChest.getCarryInteractions().get(InteractionType.Primary).equals(ChestPickupInput.ROOT);
        assert carryChest.getCarryInteractions().size() == 3;
        assert carryChest.getCarryHudInputBindings().get(InteractionType.Secondary)
                .equals("client.hud.inputBindings.place");
        assert carryChest.getCarryHudInputBindings().size() == 1;
        assert carryChest.toPacket().carryInteractions.size() == 3;
        assert (ChestPickupSystem.PICKUP_BLOCK_FLAGS
                & com.hypixel.hytale.server.core.universe.world.SetBlockSettings.NO_SEND_PARTICLES) != 0;
        assert (ChestPickupSystem.PICKUP_BLOCK_FLAGS
                & com.hypixel.hytale.server.core.universe.world.SetBlockSettings.NO_SEND_AUDIO) != 0;
        FilledChestItems.register();
        assertNativeAssets(nativeInteractions, nativeRoots, nativeBlocks);
        var originalBindings = java.util.Map.copyOf(UnarmedInteractions.getAssetMap()
                .getAsset(UnarmedInteractions.DEFAULT_UNARMED_ID).getInteractions());
        ChestPickupInput.register();
        assertNativeAssets(nativeInteractions, nativeRoots, nativeBlocks);
        var bindings = UnarmedInteractions.getAssetMap()
                .getAsset(UnarmedInteractions.DEFAULT_UNARMED_ID).getInteractions();
        for (var entry : originalBindings.entrySet()) {
            assert bindings.get(entry.getKey()).equals(entry.getValue());
        }
        assert bindings.containsKey(InteractionType.Secondary);
        assert bindings.containsKey(InteractionType.Use);
        assert RootInteraction.getAssetMap().getAsset(ChestPickupInput.ROOT) != null;
        assert RootInteraction.getAssetMap().getAsset(ChestCarrySystem.PLACE_ROOT) != null;
        testSettings();
        testCarryPickupGuard();
        testUiEscapes();
        assert CommonAssetRegistry.getByName(FilledChestItems.ICON) != null;
        var hintAsset = CommonAssetRegistry.getByName("UI/Custom/" + ChestPickupHintSystem.UI_PATH);
        assert hintAsset != null;
        var hintUi = new String(hintAsset.getBlob().join(), StandardCharsets.UTF_8);
        assert hintUi.contains("Label #PickupHint {");
        assert hintUi.contains("Anchor: (Width: 600, Height: 200);");
        assert hintUi.contains("Anchor: (Top: 180, Height: 24);");
        assert !hintUi.contains("HotkeyLabel");
        assert !hintUi.contains("InputBinding");
        assert !hintUi.contains("TexturePath");
        assert hintUi.contains("Text: \"" + ChestPickupHintSystem.hintText(false, true, false) + "\";");
        assert ChestPickupHintSystem.hintText(false, true, false)
                .equals("Crouch + Interact to carry chest");
        assert ChestPickupHintSystem.hintText(true, true, false)
                .equals("Crouch + Secondary to carry chest");
        assert ChestPickupHintSystem.hintText(false, false, false)
                .equals("Crouch + Interact to pick up chest");
        assert ChestPickupHintSystem.hintText(true, false, false)
                .equals("Crouch + Secondary to pick up chest");
        for (boolean secondary : new boolean[]{false, true}) {
            for (boolean carry : new boolean[]{false, true}) {
                assert ChestPickupHintSystem.hintText(secondary, carry, true)
                        .equals("Release crouch; use Secondary to place chest");
            }
        }
        for (String path : new String[]{ChestPickupHintSystem.UI_PATH, ChestPickupSettingsCommand.UI_PATH}) {
            var asset = CommonAssetRegistry.getByName("UI/Custom/" + path);
            assert asset != null : path;
            var ui = new String(asset.getBlob().join(), StandardCharsets.UTF_8);
            assert hasSupportedUiEscapes(ui) : "Unsupported UI string escape in " + path;
        }
        assert !ChestPickupHintSystem.shouldShow(null);
        assert ChestPickupHintSystem.shouldShow(
                BlockType.getAssetMap().getAsset("Furniture_Crude_Chest_Small"));
        assert ChestPickupSystem.isChest("Furniture_Crude_Chest_Small");
        assert ChestPickupSystem.isChest("Furniture_Crude_Chest_Large");
        assert ChestPickupSystem.isChest("Furniture_Goblin_ChestStolen_Small");
        assert !ChestPickupSystem.isChest("Armor_Iron_Chest");
        assert !ChestPickupSystem.isChest("Furniture_Crude_Barrel");
        assert !ChestPickupSystem.isChest("Furniture_Crude_Workbench");
        assert !ChestPickupHintSystem.shouldShow(
                BlockType.getAssetMap().getAsset("Furniture_Crude_Workbench"));

        var inventory = new SimpleItemContainer((short) 2);
        var packed = new ItemStack("Furniture_Crude_Chest_Small", 1)
                .withMetadata("BlockHolder", new BsonDocument("test", new BsonString("contents")));
        assert ChestPickupSystem.findEmptySlot(inventory, packed) == 0;
        assert inventory.setItemStackForSlot((short) 0, packed).succeeded();
        assert ChestPickupSystem.findEmptySlot(inventory, packed) == 1;
        assert inventory.setItemStackForSlot((short) 1, packed).succeeded();
        assert ChestPickupSystem.findEmptySlot(inventory, packed) == -1;
        assert inventory.getItemStack((short) 0).equals(packed);
        var slot0 = inventory.getItemStack((short) 0);
        var slot1 = inventory.getItemStack((short) 1);
        assert ChestPickupSystem.findEmptySlot(inventory, packed) == -1;
        assert inventory.getItemStack((short) 0).equals(slot0);
        assert inventory.getItemStack((short) 1).equals(slot1);
        assert !ChestPickupSystem.NO_SPACE_MESSAGE.isBlank();
        var filtered = new SimpleItemContainer((short) 2);
        filtered.setSlotFilter(FilterActionType.ADD, (short) 0, SlotFilter.DENY);
        assert ChestPickupSystem.findEmptySlot(filtered, packed) == 1;
        filtered.setSlotFilter(FilterActionType.ADD, (short) 1, SlotFilter.DENY);
        assert ChestPickupSystem.findEmptySlot(filtered, packed) == -1;
        int roundTrips = 0;
        for (var block : BlockType.getAssetMap().getAssetMap().values()) {
            var item = block.getItem();
            if (item == null || !ChestPickupSystem.isChest(item.getId()) || block.getBlockEntity() == null) {
                continue;
            }
            var holder = block.getBlockEntity().clone();
            var container = holder.getComponent(ItemContainerBlock.getComponentType());
            if (container == null) {
                continue;
            }
            assert ChestPickupHintSystem.shouldShow(block) : block.getId();
            var contents = new SimpleItemContainer(container.getCapacity());
            container.setItemContainer(contents);
            var emptyChest = ChestPickupSystem.pack(block.getId(), holder);
            assert emptyChest != null;
            assert emptyChest.getItemId().equals(item.getId()) : block.getId();
            assert emptyChest.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC) == null;
            var payload = new ItemStack("Furniture_Crude_Chest_Small", 3)
                    .withMetadata("TestMarker", new BsonString("preserve me"));
            assert contents.setItemStackForSlot((short) 0, payload).succeeded();
            assert contents.setItemStackForSlot((short) (contents.getCapacity() - 1), payload).succeeded();
            container.setItemContainer(contents);
            var chest = ChestPickupSystem.pack(block.getId(), holder);
            assert chest != null : block.getId();
            assert chest.getQuantity() == 1;
            assert chest.getItemId().equals(FilledChestItems.PREFIX + item.getId());
            assert chest.getItem().getIcon().equals(FilledChestItems.ICON);
            assert chest.getItem().toPacket().icon.equals(FilledChestItems.ICON);
            assert chest.getItem().getMaxStack() == 1;
            assert chest.getBlockKey().equals(emptyChest.getBlockKey()) : block.getId();
            assert Item.getAssetMap().getAsset(item.getId()).getIcon().equals(item.getIcon());
            var display = chest.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC);
            assert display != null;
            assert display.getName().getFormattedMessage().equals(chest.getDisplayName().getFormattedMessage());
            assert messageText(display.getName()).contains("Contains items");
            int occupied = contents.getCapacity() == 1 ? 1 : 2;
            assert messageText(display.getDescription()).contains((occupied * 3) + " items in " + occupied + "/")
                    : block.getId() + ": " + messageText(display.getDescription());
            assert messageText(display.getDescription()).contains("3 x ");
            var packetMetadata = BsonDocument.parse(chest.toPacket().metadata);
            assert packetMetadata.containsKey("ItemDisplay") : "Client packet must include tooltip";
            assert packetMetadata.containsKey(ItemStack.Metadata.BLOCK_HOLDER);
            var saved = BsonDocument.parse(ItemStack.CODEC.encode(chest, new ExtraInfo()).asDocument().toJson());
            var savedChest = ItemStack.CODEC.decode(saved, new ExtraInfo());
            assert savedChest.equals(chest) : block.getId();
            var restored = savedChest.getFromMetadataOrNull(ItemStack.Metadata.BLOCK_HOLDER,
                    ChunkStore.REGISTRY.getEntityCodec());
            assert restored != null : block.getId();
            var restoredContents = restored.getComponent(ItemContainerBlock.getComponentType()).getItemContainer();
            assert restoredContents.getCapacity() == contents.getCapacity() : block.getId();
            for (short slot = 0; slot < contents.getCapacity(); slot++) {
                assert java.util.Objects.equals(contents.getItemStack(slot), restoredContents.getItemStack(slot))
                        : block.getId() + " slot " + slot;
            }
            var carriedHolder = EntityStore.REGISTRY.newHolder();
            carriedHolder.addComponent(CarriedBlock.getComponentType(), new CarriedBlock(block.getId(), holder, null));
            var savedCarry = BsonDocument.parse(EntityStore.REGISTRY.serialize(carriedHolder).toJson());
            var loadedCarry = EntityStore.REGISTRY.getEntityCodec().decode(savedCarry, new ExtraInfo())
                    .getComponent(CarriedBlock.getComponentType());
            assert ChestCarrySystem.isCarryingChest(loadedCarry) : block.getId();
            assert loadedCarry.getBlockTypeKey().equals(block.getId());
            assert loadedCarry.getCarriedItemId().equals(item.getId()) : "Must display the original chest";
            var carriedItem = Item.getAssetMap().getAsset(loadedCarry.getCarriedItemId());
            assert carriedItem.getCarryInteractions().get(InteractionType.Secondary)
                    .equals(ChestCarrySystem.PLACE_ROOT) : block.getId();
            assert loadedCarry.toItemStack().getBlockKey().equals(emptyChest.getBlockKey());
            var loadedContents = loadedCarry.getHolder().getComponent(ItemContainerBlock.getComponentType())
                    .getItemContainer();
            assert loadedContents.getCapacity() == contents.getCapacity();
            for (short slot = 0; slot < contents.getCapacity(); slot++) {
                assert java.util.Objects.equals(contents.getItemStack(slot), loadedContents.getItemStack(slot))
                        : block.getId() + " carried slot " + slot;
            }
            roundTrips++;
        }
        testPreviewLimit();
        testLegacyInventory();
        ChestBreakTest.run();
        assert roundTrips >= 20 : "Expected at least 20 chest block variants, got " + roundTrips;
        String result = "PASS: native small/large chest break drops, hotbar/storage pickup change notifications, "
                + "carry-by-default settings, native carry placement controls, quiet pickup flags, "
                + "crouched repeat-pickup and wrong-action guards, configurable crouch + Interact/Secondary, "
                + "saved personal settings, carry pickup guard, "
                + "preserved controls, full inventory, filled icon, "
                + "tooltip preview limits, native block keys and " + roundTrips
                + " inventory/carried chest-content and display persistence round trips.";
        try {
            Files.writeString(Path.of(System.getProperty("pickupchests.testResult")), result);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        getLogger().atInfo().log(result);
    }

    private static void assertNativeAssets(java.util.Map<String, Interaction> interactions,
            java.util.Map<String, RootInteraction> roots, java.util.Map<String, BlockType> blocks) {
        for (var entry : interactions.entrySet()) {
            assert Interaction.getAssetMap().getAsset(entry.getKey()) == entry.getValue()
                    : "Native interaction changed: " + entry.getKey();
        }
        for (var entry : roots.entrySet()) {
            assert RootInteraction.getAssetMap().getAsset(entry.getKey()) == entry.getValue()
                    : "Native root changed: " + entry.getKey();
        }
        for (var entry : blocks.entrySet()) {
            var current = BlockType.getAssetMap().getAsset(entry.getKey());
            assert current != null : "Native block removed: " + entry.getKey();
            assert current.getItem() == entry.getValue().getItem()
                    : "Native block item changed: " + entry.getKey();
            var originalGathering = entry.getValue().getGathering();
            var currentGathering = current.getGathering();
            assert (originalGathering == null) == (currentGathering == null)
                    : "Native gathering removed: " + entry.getKey();
            if (originalGathering != null) {
                var before = BlockGathering.CODEC.encode(originalGathering, new ExtraInfo());
                var after = BlockGathering.CODEC.encode(currentGathering, new ExtraInfo());
                assert before.equals(after)
                        : "Native gathering changed: " + entry.getKey() + "\nBefore: " + before + "\nAfter: " + after;
            }
            var originalHolder = entry.getValue().getBlockEntity();
            var currentHolder = current.getBlockEntity();
            assert (originalHolder == null) == (currentHolder == null)
                    : "Native container removed: " + entry.getKey();
            if (originalHolder != null) {
                assert ChunkStore.REGISTRY.serialize(originalHolder).equals(ChunkStore.REGISTRY.serialize(currentHolder))
                        : "Native container changed: " + entry.getKey();
            }
        }
    }

    private static void testUiEscapes() {
        assert hasSupportedUiEscapes("Text: \"Pick up chest\";");
        assert hasSupportedUiEscapes("Text: \"Quoted: \\\"chest\\\" and \\\\ path\";");
        assert !hasSupportedUiEscapes("Text: \"First\\nsecond\";");
        assert !hasSupportedUiEscapes("Text: \"First\\tsecond\";");
        assert !hasSupportedUiEscapes("\\");
    }

    private static boolean hasSupportedUiEscapes(String ui) {
        for (int index = 0; index < ui.length(); index++) {
            if (ui.charAt(index) == '\\') {
                if (++index == ui.length()) {
                    return false;
                }
                char escaped = ui.charAt(index);
                if (escaped != '\\' && escaped != '"') {
                    return false;
                }
            }
        }
        return true;
    }

    private static void testSettings() {
        var defaults = new ChestPickupSettings();
        assert defaults.carryInHands();
        assert !defaults.secondaryAction();
        assert defaults.interactionType() == InteractionType.Use;
        assert defaults.bindingKey().equals("BlockInteractAction");
        for (boolean carry : new boolean[]{false, true}) {
            for (boolean secondary : new boolean[]{false, true}) {
                var settings = new ChestPickupSettings(carry, secondary);
                for (var type : InteractionType.values()) {
                    assert ChestPickupSystem.isPickupGesture(type, true, settings)
                            == (type == settings.interactionType());
                    assert !ChestPickupSystem.isPickupGesture(type, false, settings);
                }
                assert settings.bindingKey().equals(secondary ? "SecondaryItemAction" : "BlockInteractAction");
                var player = EntityStore.REGISTRY.newHolder();
                player.addComponent(ChestPickupSettings.getComponentType(), settings);
                var encoded = BsonDocument.parse(EntityStore.REGISTRY.serialize(player).toJson());
                var restored = EntityStore.REGISTRY.getEntityCodec().decode(encoded, new ExtraInfo());
                var saved = restored.getComponent(ChestPickupSettings.getComponentType());
                assert saved.carryInHands() == carry;
                assert saved.secondaryAction() == secondary;
                assert saved.clone() != saved;
                assert saved.clone().interactionType() == saved.interactionType();
            }
        }
        assert !ChestCarrySystem.isCarryingChest(null);
        assert !ChestCarrySystem.isCarryingChest(new CarriedBlock());
        var cleared = new CarriedBlock("Furniture_Crude_Chest_Small", null, null);
        assert ChestCarrySystem.isCarryingChest(cleared);
        cleared.clearItemBlockKey();
        assert !ChestCarrySystem.isCarryingChest(cleared);
        assert !ChestCarrySystem.isCarryingChest(new CarriedBlock("Furniture_Crude_Barrel", null, null));
    }

    private static void testCarryPickupGuard() {
        for (var type : new InteractionType[]{InteractionType.Use, InteractionType.Primary, InteractionType.Secondary}) {
            for (boolean crouching : new boolean[]{false, true}) {
                assert ChestCarrySystem.blocksInteraction(type, "Open_Container", crouching);
                assert ChestCarrySystem.blocksInteraction(type, ChestPickupInput.ROOT, crouching);
                assert ChestCarrySystem.blocksInteraction(type, "Root_Carry_Dropped_Block", crouching);
                assert ChestCarrySystem.blocksInteraction(type, "Root_Throw_Carryable_Block", crouching);
                assert ChestCarrySystem.blocksInteraction(type, ChestCarrySystem.PLACE_ROOT, crouching)
                        == (crouching || type != InteractionType.Secondary);
            }
        }
        var original = new SimpleItemContainer((short) 3);
        var item = new ItemStack("Furniture_Crude_Chest_Small", 1);
        assert original.setItemStackForSlot((short) 0, item).succeeded();
        original.setSlotFilter(FilterActionType.ADD, (short) 2, SlotFilter.DENY);
        var carrying = new java.util.concurrent.atomic.AtomicBoolean(true);
        var guarded = new ChestCarrySystem.GuardedInventory(original, carrying::get);
        var rejected = guarded.addItemStack(item);
        assert rejected.getRemainder().equals(item);
        assert original.getItemStack((short) 0).equals(item) : "Must not merge ground loot while carrying";
        assert ItemStack.isEmpty(original.getItemStack((short) 1));
        assert !guarded.canAddItemStackToSlot((short) 1, item, true, true);
        assert guarded.removeItemStackFromSlot((short) 0, item, 1).succeeded();
        carrying.set(false);
        assert ItemStack.isEmpty(guarded.addItemStack(item).getRemainder());
        assert original.getItemStack((short) 0).equals(item);
        assert guarded.canAddItemStackToSlot((short) 1, item, true, true);
        assert !guarded.canAddItemStackToSlot((short) 2, item, true, true) : "Keep the original filters";
        carrying.set(true);
        assert guarded.addItemStack(item).getRemainder().equals(item);
        carrying.set(false);
        var saved = SimpleItemContainer.CODEC.encode(original, new ExtraInfo());
        var restored = SimpleItemContainer.CODEC.decode(saved, new ExtraInfo());
        assert restored.getItemStack((short) 0).equals(item);
        assert restored.canAddItemStackToSlot((short) 1, item, true, true);
        testCarryInventoryUpdates();
    }

    private static void testCarryInventoryUpdates() {
        var hotbar = new SimpleItemContainer((short) 1);
        var storage = new SimpleItemContainer((short) 2);
        var hotbarUpdates = new java.util.concurrent.atomic.AtomicInteger();
        var storageUpdates = new java.util.concurrent.atomic.AtomicInteger();
        hotbar.registerChangeEvent(event -> hotbarUpdates.incrementAndGet());
        storage.registerChangeEvent(event -> storageUpdates.incrementAndGet());
        var nativeInventory = new com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer(
                new com.hypixel.hytale.server.core.inventory.container.FetchedItemContainer(() -> hotbar),
                new com.hypixel.hytale.server.core.inventory.container.FetchedItemContainer(() -> storage));
        var carrying = new java.util.concurrent.atomic.AtomicBoolean(true);
        var guarded = new ChestCarrySystem.GuardedInventory(nativeInventory, carrying::get);
        var item = new ItemStack("Furniture_Crude_Chest_Small", 1);
        assert guarded.addItemStack(item).getRemainder().equals(item);
        assert hotbarUpdates.get() == 0;
        assert storageUpdates.get() == 0;
        carrying.set(false);
        assert ItemStack.isEmpty(guarded.addItemStack(item).getRemainder());
        assert item.equals(hotbar.getItemStack((short) 0));
        assert hotbarUpdates.get() > 0 : "Pickup must notify the real hotbar for client sync and saving";
        var payload = new ItemStack("Wood_Softwood_Trunk", 7);
        assert ItemStack.isEmpty(guarded.addItemStack(payload).getRemainder());
        assert payload.equals(storage.getItemStack((short) 0));
        assert storageUpdates.get() > 0 : "Pickup must notify the real storage for client sync and saving";
        int beforeMerge = storageUpdates.get();
        assert ItemStack.isEmpty(guarded.addItemStack(payload).getRemainder());
        assert storage.getItemStack((short) 0).getQuantity() == 14;
        assert storageUpdates.get() > beforeMerge : "Merged loot must also update the client";
        int beforeRemove = hotbarUpdates.get();
        assert guarded.removeItemStackFromSlot((short) 0, item, 1).succeeded();
        assert ItemStack.isEmpty(hotbar.getItemStack((short) 0));
        assert hotbarUpdates.get() > beforeRemove : "Removing an item must update the client";
        var saved = SimpleItemContainer.CODEC.encode(storage, new ExtraInfo());
        var restored = SimpleItemContainer.CODEC.decode(saved, new ExtraInfo());
        assert restored.getItemStack((short) 0).getQuantity() == 14;
    }

    private static void testPreviewLimit() {
        var block = BlockType.getAssetMap().getAsset("Furniture_Crude_Chest_Small");
        var holder = block.getBlockEntity().clone();
        var container = holder.getComponent(ItemContainerBlock.getComponentType());
        var contents = new SimpleItemContainer(container.getCapacity());
        for (short slot = 0; slot < 5; slot++) {
            assert contents.setItemStackForSlot(slot, new ItemStack("Furniture_Crude_Chest_Small", slot + 1)).succeeded();
        }
        container.setItemContainer(contents);
        var chest = ChestPickupSystem.pack(block.getId(), holder);
        var description = chest.getDisplayDescription();
        String text = messageText(description);
        assert text.contains("15 items in 5/18 slots.") : text;
        assert text.contains("2 more occupied slots.") : text;
        assert description.getChildren().size() == 5 : description;
        var firstPreview = description.getChildren().getFirst().getChildren();
        assert firstPreview.getLast().getMessageId() != null : "Item names must remain localized";
    }

    private static void testLegacyInventory() {
        var block = BlockType.getAssetMap().getAsset("Furniture_Crude_Chest_Small");
        var holder = block.getBlockEntity().clone();
        var container = holder.getComponent(ItemContainerBlock.getComponentType());
        var contents = new SimpleItemContainer(container.getCapacity());
        assert contents.setItemStackForSlot((short) 0, new ItemStack("Furniture_Crude_Chest_Small", 2)).succeeded();
        container.setItemContainer(contents);
        var legacy = new CarriedBlock(block.getId(), holder, null).toItemStack();
        var inventory = new SimpleItemContainer((short) 3);
        var ordinary = new ItemStack(block.getItem().getId(), 1);
        assert inventory.setItemStackForSlot((short) 0, legacy).succeeded();
        assert inventory.setItemStackForSlot((short) 1, ordinary).succeeded();
        FilledChestItems.refreshInventory(inventory);
        assert inventory.getItemStack((short) 0).getItemId().startsWith(FilledChestItems.PREFIX);
        assert inventory.getItemStack((short) 0).getDisplayDescription() != null;
        assert inventory.getItemStack((short) 1).equals(ordinary);
        assert ItemStack.isEmpty(inventory.getItemStack((short) 2));
        var refreshed = inventory.getItemStack((short) 0);
        FilledChestItems.refreshInventory(inventory);
        assert inventory.getItemStack((short) 0).equals(refreshed);
    }

    private static String messageText(com.hypixel.hytale.server.core.Message message) {
        var text = new StringBuilder();
        if (message.getRawText() != null) {
            text.append(message.getRawText());
        }
        for (var child : message.getChildren()) {
            text.append(messageText(child));
        }
        return text.toString();
    }
}
