/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.fastutil.ints.IntObjectPair;
import com.viaversion.viaversion.libs.mcstructs.converter.impl.v1_21_5.NbtConverter_v1_21_5;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.dialog.ActionButton;
import com.viaversion.viaversion.libs.mcstructs.dialog.AfterAction;
import com.viaversion.viaversion.libs.mcstructs.dialog.Dialog;
import com.viaversion.viaversion.libs.mcstructs.dialog.Input;
import com.viaversion.viaversion.libs.mcstructs.dialog.action.CustomAllAction;
import com.viaversion.viaversion.libs.mcstructs.dialog.body.PlainMessageBody;
import com.viaversion.viaversion.libs.mcstructs.dialog.impl.MultiActionDialog;
import com.viaversion.viaversion.libs.mcstructs.dialog.impl.NoticeDialog;
import com.viaversion.viaversion.libs.mcstructs.dialog.input.BooleanInput;
import com.viaversion.viaversion.libs.mcstructs.dialog.input.NumberRangeInput;
import com.viaversion.viaversion.libs.mcstructs.dialog.input.SingleOptionInput;
import com.viaversion.viaversion.libs.mcstructs.dialog.input.TextInput;
import com.viaversion.viaversion.libs.mcstructs.dialog.serializer.DialogSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import net.lenni0451.mcstructs_bedrock.forms.Form;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.lenni0451.mcstructs_bedrock.forms.serializer.FormSerializer;
import net.lenni0451.mcstructs_bedrock.forms.types.ActionForm;
import net.lenni0451.mcstructs_bedrock.forms.types.CustomForm;
import net.lenni0451.mcstructs_bedrock.forms.types.ModalForm;
import net.lenni0451.mcstructs_bedrock.text.utils.BedrockTextUtils;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.model.container.ChestContainer;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackRequestActionType;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.SimpleContainer;
import net.raphimc.viabedrock.api.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ComplexInventoryTransaction_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.EquipmentSlot;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.protocol.model.inventory.InventorySource;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryTransactionData;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequest;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponse;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.rewriter.InventoryTransactionRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class InventoryPackets {

    private static final int DIALOG_BUTTON_WIDTH = 200;
    private static final int DIALOG_FAKE_BUTTON_WIDTH = 300;
    private static final String DIALOG_FAKE_BUTTON_TEXT = "This is not actually a button, but has to be one because dialogs don't support adding text only elements. Clicking it has the same effect as closing the dialog.";
    // Fallback max stack size for merge predictions. Items with smaller stacks (e.g. ender pearls)
    // get rejected by the server and fall back to a resync, which keeps the inventory consistent
    private static final int MAX_STACK_SIZE = 64;

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_TRANSACTION, null, wrapper -> {
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);

            wrapper.cancel();
            final BedrockInventoryTransaction inventoryTransaction = wrapper.read(inventoryTransactionRewriter.getInventoryTransactionType());

            if (inventoryTransaction.legacyRequestId() != 0) {
                // Ignore legacy inventory transactions for now
                return;
            }

            if (inventoryTransaction.actions() != null && !inventoryTransaction.actions().isEmpty()) {
                // Apply all container-inventory actions, then send one content packet per changed container
                final List<Container> changedContainers = new ArrayList<>();
                for (InventoryActionData action : inventoryTransaction.actions()) {
                    if (action.source().type() == InventorySourceType.Container_Inventory) {
                        final Container container = inventoryTracker.getContainerClientbound((byte) action.source().containerId(), null, null);

                        if (container != null) {
                            container.setItem(action.slot(), action.toItem());
                            if (!changedContainers.contains(container)) {
                                changedContainers.add(container);
                            }
                        } else {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received inventory action for unknown container ID: " + action.source().containerId());
                        }
                    }
                }
                for (Container container : changedContainers) {
                    PacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
                }
            }

            if (inventoryTransaction.transactionType() != ComplexInventoryTransaction_Type.NormalTransaction) {
                ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Received unsupported inventory transaction type: " + inventoryTransaction.transactionType());
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ITEM_STACK_RESPONSE, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);

            final ItemStackResponse response = wrapper.read(BedrockTypes.ITEM_STACK_RESPONSE);
            if (response == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Item stack response: empty");
                return;
            }
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Item stack response: result=" + response.result() + " requestId=" + response.requestId() + " containers=" + response.containers());

            if (response.result() == ItemStackResponse.RESULT_OK) {
                // Apply the accepted request's moves to the tracked containers first (the response only carries net ids and amounts)
                final InventoryTracker.PendingItemStackRequest accepted = inventoryTracker.takePendingItemStackRequest(response.requestId());
                if (accepted != null) {
                    applyAcceptedActions(inventoryTracker, accepted.actions(), accepted.sourceSnapshots());
                }
                // The response is the only authoritative sync for accepted requests: apply the returned
                // net ids + amounts to the tracked containers (vanilla sends no follow-up inventory packets)
                if (response.containers() != null) {
                    final List<Container> changedContainers = new ArrayList<>();
                    for (ItemStackResponse.Container responseContainer : response.containers()) {
                        final Container container = resolveResponseContainer(wrapper.user(), inventoryTracker, responseContainer.containerName());
                        if (container == null) {
                            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Received item stack response for unknown container: " + responseContainer.containerName());
                            continue;
                        }
                        for (ItemStackResponse.Slot responseSlot : responseContainer.slots()) {
                            final int slotIndex = container == inventoryTracker.getOffhandContainer() ? 0 : container instanceof CraftingTableContainer ? (responseSlot.slot() & 0xFF) - 32 : responseSlot.slot() & 0xFF; // The second slot field is the authoritative slot index (offhand is addressed as slot 1, the crafting grid as 32-40)
                            if (slotIndex < 0 || slotIndex >= container.size()) {
                                continue;
                            }
                            final BedrockItem tracked = container.getItem(slotIndex);
                            if (responseSlot.amount() <= 0 || (tracked.isEmpty() && responseSlot.serverNetId() == 0)) {
                                if (!tracked.isEmpty()) {
                                    container.setItem(slotIndex, BedrockItem.empty());
                                    if (!changedContainers.contains(container)) {
                                        changedContainers.add(container);
                                    }
                                }
                                continue;
                            }
                            if (tracked.isEmpty()) {
                                continue; // Can't reconcile an item we don't track
                            }
                            final BedrockItem updated = tracked.copy();
                            updated.setAmount(responseSlot.amount());
                            updated.setNetId(responseSlot.serverNetId() > 0 ? responseSlot.serverNetId() : tracked.netId());
                            container.setItem(slotIndex, updated);
                            if (!changedContainers.contains(container)) {
                                changedContainers.add(container);
                            }
                        }
                    }
                    for (Container container : changedContainers) {
                        PacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
                    }
                }
                // Push the resulting state to the Java client: inventory, open container and cursor
                PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
                if (inventoryTracker.getCurrentContainer() != null && inventoryTracker.getCurrentContainer().type() != ContainerType.INVENTORY) {
                    PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getCurrentContainer());
                }
                final PacketWrapper acceptedCursorPacket = PacketWrapper.create(ClientboundPackets26_1.SET_CURSOR_ITEM, wrapper.user());
                acceptedCursorPacket.write(VersionedTypes.V26_2.item, inventoryTracker.getHudContainer().getJavaItem(0)); // cursor item
                acceptedCursorPacket.send(BedrockProtocol.class);
                updateCraftingResult(wrapper.user(), inventoryTracker);
                drainQueuedClicks(wrapper.user(), inventoryTracker);
                return;
            }
            inventoryTracker.takePendingItemStackRequest(response.requestId());

            // The request was rejected: resync the inventory + open container + cursor to the server state
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Item stack request " + response.requestId() + " rejected with result " + response.result());
            PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
            if (inventoryTracker.getCurrentContainer() != null) {
                PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getCurrentContainer());
            }
            final PacketWrapper cursorPacket = PacketWrapper.create(ClientboundPackets26_1.SET_CURSOR_ITEM, wrapper.user());
            cursorPacket.write(VersionedTypes.V26_2.item, inventoryTracker.getHudContainer().getJavaItem(0)); // cursor item
            cursorPacket.send(BedrockProtocol.class);
            inventoryTracker.queuedClicks().clear(); // Queued clicks were built on the rejected state
            updateCraftingResult(wrapper.user(), inventoryTracker);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_SET_DATA, ClientboundPackets26_1.CONTAINER_SET_DATA, wrapper -> {
            final int containerId = wrapper.read(Types.UNSIGNED_BYTE); // container id
            final int id = wrapper.read(BedrockTypes.VAR_INT); // property id
            final int value = wrapper.read(BedrockTypes.VAR_INT); // value

            final Container container = wrapper.user().get(InventoryTracker.class).getContainerClientbound((byte) containerId, null, null);
            if (container == null) {
                wrapper.cancel();
                return;
            }

            // Map Bedrock container data properties to Java container data ids. The property ids are
            // overloaded per container type (brewing and furnace use 0-2 differently)
            final int javaId;
            switch (container.type()) {
                case BREWING_STAND -> javaId = switch (id) {
                    case 0 -> 0; // Brew time -> Java: brew time
                    case 1 -> 1; // Brew fuel amount -> Java: fuel
                    default -> -1; // Fuel total is not synced by the Java client
                };
                case FURNACE, BLAST_FURNACE, SMOKER -> javaId = switch (id) {
                    case 0 -> 2; // Furnace tick count -> Java: cooking progress
                    case 1 -> 0; // Furnace lit time -> Java: lit time remaining
                    case 2 -> 1; // Furnace lit duration -> Java: lit duration
                    default -> -1; // Stored XP / fuel aux are not synced by the Java client
                };
                default -> javaId = -1;
            }
            if (javaId == -1) {
                ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Dropping container data property " + id + " for " + container.type());
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
            wrapper.write(Types.SHORT, (short) javaId); // property id
            wrapper.write(Types.SHORT, (short) value); // value
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CRAFTING_DATA, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final List<InventoryTracker.Recipe> recipes = new ArrayList<>();
            try {
                for (int list = 0; list < 2; list++) { // Shaped recipes first, then shapeless; the remaining lists are not needed
                    final boolean shaped = list == 0;
                    final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                    for (int i = 0; i < count; i++) {
                        wrapper.read(BedrockTypes.STRING); // recipe id
                        int width = 0, height = 0;
                        if (shaped) {
                            width = wrapper.read(BedrockTypes.VAR_INT); // width
                            height = wrapper.read(BedrockTypes.VAR_INT); // height
                        }
                        final int ingredientCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                        final List<InventoryTracker.Ingredient> ingredients = new ArrayList<>(ingredientCount);
                        for (int j = 0; j < ingredientCount; j++) {
                            ingredients.add(readRecipeIngredient(wrapper));
                        }
                        final int resultCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                        BedrockItem result = BedrockItem.empty();
                        for (int j = 0; j < resultCount; j++) {
                            final BedrockItem output = wrapper.read(itemRewriter.itemInstanceType()); // result
                            if (j == 0) result = output;
                        }
                        wrapper.read(BedrockTypes.UUID); // uuid
                        final String tag = wrapper.read(BedrockTypes.STRING); // crafting tag
                        wrapper.read(BedrockTypes.VAR_INT); // priority
                        if (shaped) {
                            wrapper.read(Types.BOOLEAN); // assume symmetry
                        }
                        if (wrapper.read(Types.BOOLEAN)) { // has unlocking requirement
                            wrapper.read(BedrockTypes.VAR_INT); // unlocking context
                            if (wrapper.read(Types.BOOLEAN)) {
                                final int requirementCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                                for (int j = 0; j < requirementCount; j++) {
                                    readRecipeIngredient(wrapper);
                                }
                            }
                        }
                        final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // recipe net id
                        if (!result.isEmpty() && (tag.equals("crafting_table") || tag.equals("deprecated"))) {
                            recipes.add(new InventoryTracker.Recipe(netId, tag, shaped, width, height, ingredients, result));
                        }
                    }
                }
            } catch (Throwable e) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to read crafting recipes (" + recipes.size() + " read so far)", e);
            }
            inventoryTracker.recipes().clear();
            inventoryTracker.recipes().addAll(recipes);
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Loaded " + recipes.size() + " crafting recipes");
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CREATIVE_CONTENT, null, wrapper -> {
            wrapper.cancel();
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);

            final int groupsCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // item groups count
            for (int i = 0; i < groupsCount; i++) {
                wrapper.read(Types.UNSIGNED_BYTE); // category
                wrapper.read(BedrockTypes.STRING); // name
                wrapper.read(itemRewriter.itemInstanceType()); // icon item
            }

            final int itemsCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // item entries count
            final List<InventoryTracker.CreativeItem> creativeItems = new ArrayList<>(itemsCount);
            for (int i = 0; i < itemsCount; i++) {
                final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // net id
                final BedrockItem item = wrapper.read(itemRewriter.itemInstanceType()); // item
                wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // group id
                if (!item.isEmpty()) {
                    creativeItems.add(new InventoryTracker.CreativeItem(item, netId));
                }
            }
            wrapper.user().get(InventoryTracker.class).setCreativeItems(creativeItems);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_OPEN, ClientboundPackets26_1.OPEN_SCREEN, wrapper -> {
            final ChunkTracker chunkTracker = wrapper.user().get(ChunkTracker.class);
            final BlockStateRewriter blockStateRewriter = wrapper.user().get(BlockStateRewriter.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final byte containerId = wrapper.read(Types.BYTE); // container id
            final byte rawType = wrapper.read(Types.BYTE); // type
            final ContainerType type = ContainerType.getByValue(rawType);
            if (type == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown ContainerType: " + rawType);
                wrapper.cancel();
                return;
            }
            final BlockPosition position = wrapper.read(BedrockTypes.BLOCK_POSITION); // position
            wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "CONTAINER_OPEN from server: id=" + containerId + " type=" + type + " position=" + position);

            if (inventoryTracker.isAnyScreenOpen()) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Server tried to open container while another container is open");
                PacketFactory.sendBedrockContainerClose(wrapper.user(), (byte) -1, ContainerType.NONE);
                wrapper.cancel();
                return;
            }
            final BedrockBlockEntity blockEntity = chunkTracker.getBlockEntity(position);
            TextComponent title = new TranslationComponent("container." + blockStateRewriter.tag(chunkTracker.getBlockState(position)));
            if (blockEntity != null && blockEntity.tag().get("CustomName") instanceof StringTag customNameTag) {
                title = TextUtil.stringToTextComponent(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(customNameTag.getValue()));
            }

            final Container container;
            switch (type) {
                case INVENTORY -> {
                    inventoryTracker.setCurrentContainer(new InventoryContainer(wrapper.user(), containerId, position, inventoryTracker.getInventoryContainer()));
                    wrapper.cancel();
                    return;
                }
                case CONTAINER -> {
                    int size = 27;
                    final BedrockBlockEntity chestBlockEntity = chunkTracker.getBlockEntity(position);
                    if (chestBlockEntity != null && chestBlockEntity.tag() != null && (chestBlockEntity.tag().contains("pairx") || chestBlockEntity.tag().contains("pairz"))) {
                        size = 54; // Double chest
                    }
                    container = new ChestContainer(wrapper.user(), containerId, title, position, size);
                }
                case MINECART_CHEST, CHEST_BOAT -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 27);
                case WORKBENCH -> container = new CraftingTableContainer(wrapper.user(), containerId, new TranslationComponent("container.crafting"), position, blockTags("crafting_table")); // Java slot 0 is the result slot
                case CRAFTER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 10, blockTags("crafter"));
                case FURNACE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("furnace"));
                case BLAST_FURNACE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("blast_furnace"));
                case SMOKER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("smoker"));
                case ANVIL -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("anvil"));
                case GRINDSTONE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("grindstone"));
                case ENCHANTMENT -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 2, blockTags("enchanting_table"));
                case BREWING_STAND -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 5, blockTags("brewing_stand"));
                case DISPENSER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 9, blockTags("dispenser"));
                case DROPPER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 9, blockTags("dropper"));
                case HOPPER, MINECART_HOPPER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 5, blockTags("hopper"));
                case BEACON -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 1, blockTags("beacon"));
                case TRADE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3);
                case LOOM -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 4, blockTags("loom"));
                case LECTERN -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 1, blockTags("lectern"));
                case STONECUTTER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 2, blockTags("stonecutter"));
                case CARTOGRAPHY -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("cartography_table"));
                case SMITHING_TABLE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 4, blockTags("smithing_table"));
                case NONE, CAULDRON, JUKEBOX, ARMOR, HAND, HUD, DECORATED_POT -> { // Bedrock client can't open these containers
                    wrapper.cancel();
                    return;
                }
                default -> {
                    wrapper.cancel();
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to open unimplemented container: " + type);
                    PacketFactory.sendBedrockContainerClose(wrapper.user(), containerId, ContainerType.NONE);
                    return;
                }
            }
            inventoryTracker.setCurrentContainer(container);

            wrapper.write(Types.VAR_INT, (int) containerId); // container id
            int javaMenuType = BedrockProtocol.MAPPINGS.getBedrockToJavaContainers().get(type);
            if (type == ContainerType.CONTAINER && container.size() == 54) {
                javaMenuType += 3; // generic_9x3 -> generic_9x6 (Java menu registry order)
            }
            wrapper.write(Types.VAR_INT, javaMenuType); // type
            wrapper.write(Types.TAG, TextUtil.textComponentToNbt(title)); // title
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_CLOSE, ClientboundPackets26_1.CONTAINER_CLOSE, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.BYTE, Types.VAR_INT); // container id
                handler(wrapper -> {
                    final ContainerType containerType = ContainerType.getByValue(wrapper.read(Types.BYTE)); // type
                    final boolean serverInitiated = wrapper.read(Types.BOOLEAN); // server initiated
                    ViaBedrock.getPlatform().getLogger().log(Level.INFO, "CONTAINER_CLOSE from server: id=" + wrapper.get(Types.VAR_INT, 0) + " type=" + containerType + " serverInitiated=" + serverInitiated);

                    final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
                    final Container container = serverInitiated ? inventoryTracker.getCurrentContainer() : inventoryTracker.getPendingCloseContainer();
                    if (container == null) {
                        wrapper.cancel();
                        return;
                    }

                    if (serverInitiated && containerType != container.type()) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Server tried to close container, but container type was not correct");
                        wrapper.cancel();
                        return;
                    }
                    inventoryTracker.setCurrentContainerClosed(serverInitiated);
                });
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_CONTENT, ClientboundPackets26_1.CONTAINER_SET_CONTENT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final BedrockItem[] items = wrapper.read(itemRewriter.newItemArrayType()); // items
            final FullContainerName containerName = wrapper.read(BedrockTypes.FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.newItemType()); // storage item

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final Container container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "INVENTORY_CONTENT from server: containerId=" + containerId + " items=" + items.length + " name=" + containerName + " -> " + (container == null ? "unknown container" : container.type()));
            if (container != null && container.setItems(items)) {
                PacketFactory.writeJavaContainerSetContent(wrapper, container);
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_SLOT, ClientboundPackets26_1.CONTAINER_SET_SLOT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final int slot = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // slot
            final FullContainerName containerName = wrapper.read(BedrockTypes.OPTIONAL_FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.optionalNewItemType()); // storage item
            final BedrockItem item = wrapper.read(itemRewriter.newItemType()); // item

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final Container container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "INVENTORY_SLOT from server: containerId=" + containerId + " slot=" + slot + " name=" + containerName + " item=" + (item.isEmpty() ? "empty" : item.identifier() + " x" + item.amount() + " netId=" + item.netId()) + " -> " + (container == null ? "unknown container" : container.type()));
            if (container != null && container.setItem(slot, item)) {
                if (container.type() == ContainerType.HUD && slot == 0) { // cursor item
                    wrapper.setPacketType(ClientboundPackets26_1.SET_CURSOR_ITEM);
                } else {
                    wrapper.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
                    wrapper.write(Types.VAR_INT, 0); // revision
                    wrapper.write(Types.SHORT, (short) container.javaSlot(slot)); // slot
                }
                wrapper.write(VersionedTypes.V26_2.item, container.getJavaItem(slot)); // item
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MODAL_FORM_REQUEST, ClientboundPackets26_1.SHOW_DIALOG, wrapper -> {
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // id
            final String data = wrapper.read(BedrockTypes.STRING); // data

            if (inventoryTracker.getCurrentContainer() != null || inventoryTracker.getCurrentForm() != null) {
                final PacketWrapper modalFormResponse = PacketWrapper.create(ServerboundBedrockPackets.MODAL_FORM_RESPONSE, wrapper.user());
                modalFormResponse.write(BedrockTypes.UNSIGNED_VAR_INT, id); // id
                modalFormResponse.write(Types.BOOLEAN, false); // has response
                modalFormResponse.write(Types.BOOLEAN, true); // has cancel reason
                modalFormResponse.write(Types.BYTE, (byte) ModalFormCancelReason.UserBusy.getValue()); // cancel reason
                modalFormResponse.sendToServer(BedrockProtocol.class);
                wrapper.cancel();
                return;
            }

            final Form form;
            try {
                form = FormSerializer.deserialize(data);
            } catch (Throwable e) { // Bedrock client shows error modal form
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Error while deserializing form data: " + data, e);
                wrapper.cancel();
                return;
            }
            final ResourcePackStorage resourcePackStorage = wrapper.user().get(ResourcePackStorage.class);
            form.setTranslator(resourcePackStorage.getTexts()::translate);
            inventoryTracker.setCurrentForm(IntObjectPair.of(id, form));

            final Identifier responseIdentifier = Identifier.of("viabedrock", "form/" + id);
            final CompoundTag exitButtonAdditions = new CompoundTag();
            exitButtonAdditions.putBoolean("exit", true);
            final ActionButton exitButton = new ActionButton(new StringComponent(resourcePackStorage.getTexts().get("gui.close")), DIALOG_BUTTON_WIDTH, new CustomAllAction(responseIdentifier, exitButtonAdditions));

            final Dialog dialog;
            if (form instanceof ModalForm modalForm) {
                final MultiActionDialog actionDialog = new MultiActionDialog(TextUtil.stringToTextComponent(form.getTitle()), true, false, AfterAction.CLOSE, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), exitButton, 1);
                addTextToDialog(wrapper.user(), actionDialog, modalForm.getText());
                final CompoundTag button1Additions = new CompoundTag();
                button1Additions.putInt("button_id", 0);
                actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(modalForm.getButton1()), DIALOG_BUTTON_WIDTH, new CustomAllAction(responseIdentifier, button1Additions)));
                final CompoundTag button2Additions = new CompoundTag();
                button2Additions.putInt("button_id", 1);
                actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(modalForm.getButton2()), DIALOG_BUTTON_WIDTH, new CustomAllAction(responseIdentifier, button2Additions)));
                dialog = actionDialog;
            } else if (form instanceof ActionForm actionForm) {
                if (actionForm.getElements().length == 0) { // Text only form
                    final NoticeDialog noticeDialog = new NoticeDialog(TextUtil.stringToTextComponent(form.getTitle()), true, false, AfterAction.CLOSE, new ArrayList<>(), new ArrayList<>(), exitButton);
                    addTextToDialog(wrapper.user(), noticeDialog, actionForm.getText());
                    dialog = noticeDialog;
                } else {
                    final MultiActionDialog actionDialog = new MultiActionDialog(TextUtil.stringToTextComponent(form.getTitle()), true, false, AfterAction.CLOSE, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), exitButton, 1);
                    addTextToDialog(wrapper.user(), actionDialog, actionForm.getText());
                    int buttonIndex = 0;
                    for (int elementIndex = 0; elementIndex < actionForm.getElements().length; elementIndex++) {
                        final FormElement element = actionForm.getElements()[elementIndex];
                        if (element instanceof ButtonFormElement button) {
                            final CompoundTag buttonAdditions = new CompoundTag();
                            buttonAdditions.putInt("button_id", buttonIndex);
                            actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(button.getText()), DIALOG_BUTTON_WIDTH, new CustomAllAction(responseIdentifier, buttonAdditions)));
                            buttonIndex++;
                        } else if (element instanceof HeaderFormElement header) {
                            actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(header.getText()), new StringComponent(DIALOG_FAKE_BUTTON_TEXT), DIALOG_FAKE_BUTTON_WIDTH, exitButton.getAction()));
                        } else if (element instanceof LabelFormElement label) {
                            actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(label.getText()), new StringComponent(DIALOG_FAKE_BUTTON_TEXT), DIALOG_FAKE_BUTTON_WIDTH, exitButton.getAction()));
                        } else if (element instanceof DividerFormElement) {
                        } else {
                            throw new IllegalArgumentException("Unhandled form element type: " + element.getClass().getSimpleName());
                        }
                    }
                    dialog = actionDialog;
                }
            } else if (form instanceof CustomForm customForm) {
                final MultiActionDialog actionDialog = new MultiActionDialog(TextUtil.stringToTextComponent(form.getTitle()), true, false, AfterAction.CLOSE, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), exitButton, 1);
                for (int elementIndex = 0; elementIndex < customForm.getElements().length; elementIndex++) {
                    final FormElement element = customForm.getElements()[elementIndex];
                    final String inputKey = String.valueOf(elementIndex);
                    if (element instanceof CheckboxFormElement checkbox) {
                        final BooleanInput booleanInput = new BooleanInput(TextUtil.stringToTextComponent(checkbox.getText()));
                        booleanInput.setInitial(checkbox.getDefaultValue());
                        actionDialog.getInputs().add(new Input(inputKey, booleanInput));
                    } else if (element instanceof DropdownFormElement dropdown) {
                        final SingleOptionInput singleOptionInput = new SingleOptionInput(new ArrayList<>(dropdown.getOptions().length), TextUtil.stringToTextComponent(dropdown.getText()));
                        for (int dropdownIndex = 0; dropdownIndex < dropdown.getOptions().length; dropdownIndex++) {
                            final String option = dropdown.getOptions()[dropdownIndex];
                            singleOptionInput.getOptions().add(new SingleOptionInput.Entry(String.valueOf(dropdownIndex), TextUtil.stringToTextComponent(option), dropdownIndex == dropdown.getDefaultOption()));
                        }
                        actionDialog.getInputs().add(new Input(inputKey, singleOptionInput));
                    } else if (element instanceof SliderFormElement slider) {
                        final NumberRangeInput numberRangeInput = new NumberRangeInput(TextUtil.stringToTextComponent(slider.getText()), new NumberRangeInput.Range(slider.getMin(), slider.getMax(), slider.getDefaultValue(), slider.getStep()));
                        actionDialog.getInputs().add(new Input(inputKey, numberRangeInput));
                    } else if (element instanceof StepSliderFormElement stepSlider) {
                        final SingleOptionInput singleOptionInput = new SingleOptionInput(new ArrayList<>(stepSlider.getSteps().length), TextUtil.stringToTextComponent(stepSlider.getText()));
                        for (int stepIndex = 0; stepIndex < stepSlider.getSteps().length; stepIndex++) {
                            final String step = stepSlider.getSteps()[stepIndex];
                            final String stepKey = String.valueOf(stepIndex);
                            singleOptionInput.getOptions().add(new SingleOptionInput.Entry(stepKey, TextUtil.stringToTextComponent(step), stepIndex == stepSlider.getDefaultStep()));
                        }
                        actionDialog.getInputs().add(new Input(inputKey, singleOptionInput));
                    } else if (element instanceof TextFieldFormElement textField) {
                        final TextInput textInput = new TextInput(TextUtil.stringToTextComponent(textField.getText()));
                        textInput.setMaxLength(100);
                        textInput.setInitial(textField.getDefaultValue());
                        actionDialog.getInputs().add(new Input(inputKey, textInput));
                    } else if (element instanceof HeaderFormElement header) {
                        addTextToDialog(wrapper.user(), actionDialog, header.getText());
                    } else if (element instanceof LabelFormElement label) {
                        addTextToDialog(wrapper.user(), actionDialog, label.getText());
                    } else if (element instanceof DividerFormElement) {
                        if (wrapper.user().getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_6)) {
                            final TextInput textInput = new TextInput(new StringComponent());
                            textInput.setLabelVisible(false);
                            textInput.setMaxLength(Integer.MAX_VALUE);
                            textInput.setMultiline(new TextInput.MultilineOptions(null, 1));
                            actionDialog.getInputs().add(new Input("dummy", textInput));
                        }
                    } else {
                        throw new IllegalArgumentException("Unhandled form element type: " + element.getClass().getSimpleName());
                    }
                }
                actionDialog.getActions().add(new ActionButton(TextUtil.stringToTextComponent(resourcePackStorage.getTexts().get("gui.submit")), DIALOG_BUTTON_WIDTH, new CustomAllAction(responseIdentifier, null)));
                dialog = actionDialog;
            } else {
                throw new IllegalArgumentException("Unhandled form type: " + form.getClass().getSimpleName());
            }

            wrapper.write(Types.TRUSTED_COMPOUND_TAG_HOLDER, Holder.of((CompoundTag) DialogSerializer.V1_21_6.getDirectCodec().serialize(NbtConverter_v1_21_5.INSTANCE, dialog).get())); // dialog data
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CLOSE_FORM, ClientboundPackets26_1.CLEAR_DIALOG, wrapper -> {
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (inventoryTracker.getCurrentForm() != null) {
                inventoryTracker.closeCurrentForm();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_HOTBAR, ClientboundPackets26_1.SET_HELD_SLOT, wrapper -> {
            final InventoryContainer inventoryContainer = wrapper.user().get(InventoryTracker.class).getInventoryContainer();
            final int slot = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // selected slot
            final byte containerId = wrapper.read(Types.BYTE); // container id
            final boolean shouldSelectSlot = wrapper.read(Types.BOOLEAN); // should select slot
            if (slot >= 0 && slot < 9 && containerId == inventoryContainer.containerId() && shouldSelectSlot) {
                wrapper.write(Types.VAR_INT, slot); // slot
            } else {
                wrapper.cancel();
                if (containerId != inventoryContainer.containerId()) { // Bedrock client doesn't render hotbar selection and held item anymore
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to set hotbar slot with wrong container id: " + containerId);
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_REGISTRY_CLEANUP, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final FullContainerName[] removedContainers = wrapper.read(BedrockTypes.FULL_CONTAINER_NAME_ARRAY); // removed containers
            for (FullContainerName containerName : removedContainers) {
                inventoryTracker.removeDynamicContainer(containerName);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_ARMOR_DAMAGE, ClientboundPackets26_1.SET_EQUIPMENT, wrapper -> {
            if (!wrapper.user().get(GameSessionStorage.class).isInventoryServerAuthoritative()) {
                wrapper.cancel();
                return;
            }
            final int size = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
            if (size <= 0) {
                wrapper.cancel();
                return;
            }
            final Container armorContainer = wrapper.user().get(InventoryTracker.class).getArmorContainer();

            wrapper.write(Types.VAR_INT, wrapper.user().get(EntityTracker.class).getClientPlayer().javaId()); // entity id
            for (int i = 0; i < size; i++) {
                final int rawArmorSlot = wrapper.read(BedrockTypes.VAR_INT); // armor slot
                final SharedTypes_Legacy_ArmorSlot armorSlot = SharedTypes_Legacy_ArmorSlot.getByValue(rawArmorSlot);
                if (armorSlot == null) { // Bedrock client ignores the whole packet if an unknown armor slot is sent
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown SharedTypes_Legacy_ArmorSlot: " + rawArmorSlot);
                    wrapper.cancel();
                    return;
                }
                final short damage = wrapper.read(BedrockTypes.SHORT_LE); // damage

                final BedrockItem item = armorSlot.getValue() < armorContainer.size() ? armorContainer.getItem(armorSlot.getValue()) : BedrockItem.empty();
                if (item.tag() == null) {
                    item.setTag(new CompoundTag());
                }
                item.tag().putInt("Damage", damage);

                final EquipmentSlot equipmentSlot = switch (armorSlot) {
                    case Head -> EquipmentSlot.HEAD;
                    case Torso -> EquipmentSlot.CHEST;
                    case Legs -> EquipmentSlot.LEGS;
                    case Feet -> EquipmentSlot.FEET;
                    case Body -> EquipmentSlot.BODY;
                };
                wrapper.write(Types.BYTE, (byte) (equipmentSlot.ordinal() | (i < (size - 1) ? Byte.MIN_VALUE : 0))); // slot
                wrapper.write(VersionedTypes.V26_2.item, wrapper.user().get(ItemRewriter.class).javaItem(item)); // item
            }
        });

        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_CLICK, null, wrapper -> {
            wrapper.cancel();
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int containerId = wrapper.read(Types.VAR_INT); // container id
            final int revision = wrapper.read(Types.VAR_INT); // state id
            final short slot = wrapper.read(Types.SHORT); // slot
            final byte button = wrapper.read(Types.BYTE); // button
            final ContainerInput[] containerInputs = ContainerInput.values();
            final int actionOrdinal = wrapper.read(Types.VAR_INT); // action
            if (actionOrdinal < 0 || actionOrdinal >= containerInputs.length) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown container input action: " + actionOrdinal);
                resyncClick(wrapper.user(), inventoryTracker, inventoryTracker.getInventoryContainer());
                return;
            }
            final ContainerInput action = containerInputs[actionOrdinal];
            if (inventoryTracker.hasPendingItemStackRequests()) {
                // Wait for the server to confirm the previous move, otherwise this click is built on a stale cursor
                inventoryTracker.queuedClicks().add(new QueuedClick(containerId, revision, slot, button, action));
                return;
            }
            processContainerClick(wrapper.user(), containerId, revision, slot, button, action);
        });
        protocol.registerServerbound(ServerboundPackets26_1.SET_CREATIVE_MODE_SLOT, null, wrapper -> {
            wrapper.cancel();
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final short slot = wrapper.read(Types.SHORT); // slot
            final Item item = wrapper.read(VersionedTypes.V26_2.lengthPrefixedItem); // item

            if (inventoryTracker.getPendingCloseContainer() != null) {
                return;
            }

            if (gameSession.isInventoryServerAuthoritative() && !item.isEmpty()) {
                // Translate to a craft creative request using the creative content cache
                final int creativeIndex = inventoryTracker.findCreativeItemIndex(wrapper.user().get(ItemRewriter.class), item);
                if (creativeIndex != -1) {
                    final int creativeNetId = inventoryTracker.getCreativeItemNetId(creativeIndex);
                    final ItemStackRequestSlot destination = inventoryRequestSlot(inventoryTracker, slot & 0xFFFF);
                    if (destination != null) {
                        final int amount = Math.max(1, item.amount());
                        // The crafted item materializes in the created output container; its net id is unknown until the server responds
                        final ItemStackRequestSlot createdOutput = new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) 0, 0);
                        final ItemStackRequest request = new ItemStackRequest(inventoryTracker.nextItemStackRequestId(), List.of(
                                ItemStackRequestAction.craftCreative(creativeNetId, 1),
                                // Vanilla clients acknowledge the craft with an empty deprecated craft results action
                                ItemStackRequestAction.craftResultsDeprecated(),
                                ItemStackRequestAction.take(amount, createdOutput, destination)
                        ), new ArrayList<>(), 0);
                        final PacketWrapper requestPacket = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, wrapper.user());
                        requestPacket.write(BedrockTypes.ITEM_STACK_REQUEST, request);
                        requestPacket.sendToServer(BedrockProtocol.class);
                        return;
                    }
                }
            }

            PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
        });
        protocol.registerServerbound(ServerboundPackets26_1.CUSTOM_CLICK_ACTION, ServerboundBedrockPackets.MODAL_FORM_RESPONSE, wrapper -> {
            final String id = wrapper.read(Types.STRING); // id
            final CompoundTag payload = (CompoundTag) wrapper.read(Types.CUSTOM_CLICK_ACTION_TAG); // payload
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (inventoryTracker.getCurrentForm() == null) {
                wrapper.cancel();
                return;
            }

            final Form form = inventoryTracker.getCurrentForm().right();
            final int formId = inventoryTracker.getCurrentForm().leftInt();
            if (!id.equals("viabedrock:form/" + formId)) {
                wrapper.cancel();
                return;
            }

            inventoryTracker.setCurrentForm(null);
            if (payload.contains("exit") && payload.getBoolean("exit")) {
                wrapper.write(BedrockTypes.UNSIGNED_VAR_INT, formId); // id
                wrapper.write(Types.BOOLEAN, false); // has response
                wrapper.write(Types.BOOLEAN, true); // has cancel reason
                wrapper.write(Types.BYTE, (byte) ModalFormCancelReason.UserClosed.getValue()); // cancel reason
                return;
            }

            if (form instanceof ModalForm modalForm) {
                modalForm.setClickedButton(payload.getInt("button_id"));
            } else if (form instanceof ActionForm actionForm) {
                actionForm.setClickedButton(payload.getInt("button_id"));
            } else if (form instanceof CustomForm customForm) {
                for (int elementIndex = 0; elementIndex < customForm.getElements().length; elementIndex++) {
                    final String inputKey = String.valueOf(elementIndex);
                    if (!payload.contains(inputKey)) continue;
                    final FormElement element = customForm.getElements()[elementIndex];
                    if (element instanceof CheckboxFormElement checkbox) {
                        checkbox.setChecked(payload.getBoolean(inputKey));
                    } else if (element instanceof DropdownFormElement dropdown) {
                        dropdown.setSelected(Integer.parseInt(payload.getString(inputKey)));
                    } else if (element instanceof SliderFormElement slider) {
                        slider.setCurrent(payload.getFloat(inputKey));
                    } else if (element instanceof StepSliderFormElement stepSlider) {
                        stepSlider.setSelected(Integer.parseInt(payload.getString(inputKey)));
                    } else if (element instanceof TextFieldFormElement textField) {
                        textField.setValue(payload.getString(inputKey));
                    }
                }
            } else {
                throw new IllegalArgumentException("Unhandled form type: " + form.getClass().getSimpleName());
            }

            wrapper.write(BedrockTypes.UNSIGNED_VAR_INT, formId); // id
            wrapper.write(Types.BOOLEAN, true); // has response
            wrapper.write(BedrockTypes.STRING, form.serializeResponse() + '\n'); // response
            wrapper.write(Types.BOOLEAN, false); // has cancel reason
        });
        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_CLOSE, ServerboundBedrockPackets.CONTAINER_CLOSE, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.VAR_INT, Types.BYTE); // container id
                create(Types.BYTE, (byte) ContainerType.NONE.getValue()); // type
                create(Types.BOOLEAN, false); // server initiated
                handler(wrapper -> {
                    final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
                    final byte containerId = wrapper.get(Types.BYTE, 0);
                    final Container container = inventoryTracker.getContainerServerbound(containerId);
                    if (container == null) {
                        wrapper.cancel();
                        return;
                    }

                    returnCursorToInventory(wrapper.user(), inventoryTracker);
                    if (container.javaContainerId() != container.containerId()) {
                        wrapper.set(Types.BYTE, 0, container.containerId());
                    }
                    inventoryTracker.markPendingClose(container);
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets26_1.SET_CARRIED_ITEM, ServerboundBedrockPackets.MOB_EQUIPMENT, wrapper -> {
            final short slot = wrapper.read(Types.SHORT); // slot
            wrapper.user().get(InventoryTracker.class).getInventoryContainer().setSelectedHotbarSlot((byte) slot, wrapper); // slot
        });
        protocol.registerServerbound(ServerboundPackets26_1.PICK_ITEM_FROM_BLOCK, ServerboundBedrockPackets.BLOCK_PICK_REQUEST, wrapper -> {
            wrapper.passthroughAndMap(Types.BLOCK_POSITION1_14, BedrockTypes.BLOCK_POSITION); // position
            wrapper.passthrough(Types.BOOLEAN); // include data
            wrapper.write(Types.UNSIGNED_BYTE, (short) 9); // number of empty hotbar slots (vanilla client always sends 9)
        });
        protocol.registerServerbound(ServerboundPackets26_1.PICK_ITEM_FROM_ENTITY, ServerboundBedrockPackets.ENTITY_PICK_REQUEST, wrapper -> {
            final int entityId = wrapper.read(Types.VAR_INT); // entity id
            final boolean includeData = wrapper.read(Types.BOOLEAN); // include data

            final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByJid(entityId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            wrapper.write(BedrockTypes.LONG_LE, entity.uniqueId()); // entity unique id
            wrapper.write(Types.UNSIGNED_BYTE, (short) 9); // number of empty hotbar slots (vanilla client always sends 9)
            wrapper.write(Types.BOOLEAN, includeData); // include data
        });
    }

    private static void addTextToDialog(final UserConnection userConnection, final Dialog dialog, final String text) {
        if (dialog.getInputs().isEmpty()) {
            for (String line : BedrockTextUtils.split(text, "\n")) {
                dialog.getBody().add(new PlainMessageBody(TextUtil.stringToTextComponent(line)));
            }
        } else {
            if (userConnection.getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_6)) {
                for (String line : BedrockTextUtils.split(text, "\n")) {
                    final TextInput textInput = new TextInput(TextUtil.stringToTextComponent(line));
                    textInput.setMaxLength(Integer.MAX_VALUE);
                    textInput.setMultiline(new TextInput.MultilineOptions(null, 1));
                    dialog.getInputs().add(new Input("dummy", textInput));
                }
            } else { // VB compatibility
                dialog.getInputs().add(new Input("dummy", new BooleanInput(TextUtil.stringToTextComponent(text))));
            }
        }
    }

    /**
     * Translates a Java container click into item stack request actions (server-auth inventory).
     * Returns null when the click can't be mapped and the containers need a resync instead.
     */
    private static List<ItemStackRequestAction> buildItemStackRequestActions(final InventoryTracker inventoryTracker, final Container viewContainer, final int rawJavaSlot, final byte button, final ContainerInput action) {
        final BedrockItem heldItem = inventoryTracker.getHudContainer().getItem(0);
        if ((short) rawJavaSlot == -999) { // Click outside the window: throw the cursor item
            if (action == ContainerInput.PICKUP && heldItem != null && !heldItem.isEmpty()) {
                return List.of(ItemStackRequestAction.drop(button == 1 ? 1 : heldItem.amount(), cursorSlot(inventoryTracker), false));
            }
            return new ArrayList<>();
        }
        Container container = viewContainer.type() == ContainerType.INVENTORY ? inventoryTracker.getInventoryContainer() : viewContainer;
        int javaSlot = rawJavaSlot & 0xFFFF;
        final boolean containerView = viewContainer.type() != ContainerType.INVENTORY && viewContainer != inventoryTracker.getInventoryContainer();
        if (containerView) {
            final int playerRegionStart = viewContainer.javaSlot(viewContainer.size() - 1) + 1;
            if (javaSlot >= playerRegionStart && javaSlot < playerRegionStart + 36) {
                container = inventoryTracker.getInventoryContainer();
                javaSlot = javaSlot - playerRegionStart + 9; // Java player inventory slot numbering (9-35 main, 36-44 hotbar)
            }
        }
        final boolean craftingView = viewContainer instanceof CraftingTableContainer || viewContainer.type() == ContainerType.INVENTORY;
        if (craftingView && javaSlot == 0 && (action == ContainerInput.PICKUP || action == ContainerInput.QUICK_MOVE)) {
            return buildCraftActions(inventoryTracker, viewContainer, action);
        }
        if (action == ContainerInput.QUICK_CRAFT) {
            return buildDragActions(inventoryTracker, viewContainer, rawJavaSlot, button);
        }
        if (action == ContainerInput.QUICK_MOVE) {
            return buildQuickMoveActions(inventoryTracker, viewContainer, container, javaSlot, containerView);
        }
        final ItemStackRequestSlot source = requestSlotInfo(inventoryTracker, container, javaSlot & 0xFFFF);
        final ItemStackRequestSlot cursor = cursorSlot(inventoryTracker);
        final BedrockItem cursorItem = inventoryTracker.getHudContainer().getItem(0);
        // Read the clicked item from the container the request slot really addresses (armor, offhand, crafting grid, ...)
        final TrackedSlot clickedSlot = resolveRequestSlot(inventoryTracker, source);
        final BedrockItem clickedItem = clickedSlot != null ? clickedSlot.container().getItem(clickedSlot.slot()) : null;
        final BedrockItem clicked = clickedItem != null ? clickedItem : BedrockItem.empty();

        switch (action) {
            case PICKUP -> {
                if (source == null) {
                    return null;
                }
                if (cursorItem.isEmpty() && clicked.isEmpty()) {
                    return new ArrayList<>(); // No-op click: don't spam the server or the Java client with resyncs
                }
                if (button == 0) {
                    if (cursorItem.isEmpty() && !clicked.isEmpty()) {
                        return List.of(ItemStackRequestAction.take(clicked.amount(), source, cursor));
                    } else if (!cursorItem.isEmpty() && clicked.isEmpty()) {
                        return List.of(ItemStackRequestAction.place(cursorItem.amount(), cursor, source));
                    } else if (!cursorItem.isEmpty() && !cursorItem.isDifferent(clicked)) {
                        // Placing onto the same item type: cap at the max stack size, the server syncs any remainder
                        final int movable = Math.min(cursorItem.amount(), Math.max(0, MAX_STACK_SIZE - clicked.amount()));
                        if (movable <= 0) {
                            return new ArrayList<>();
                        }
                        return List.of(ItemStackRequestAction.place(movable, cursor, source));
                    } else if (!cursorItem.isEmpty() && cursorItem.isDifferent(clicked)) {
                        return List.of(ItemStackRequestAction.swap(cursor, source));
                    }
                } else if (button == 1) {
                    if (cursorItem.isEmpty() && !clicked.isEmpty()) {
                        return List.of(ItemStackRequestAction.take((clicked.amount() + 1) / 2, source, cursor));
                    } else if (!cursorItem.isEmpty() && clicked.isEmpty()) {
                        return List.of(ItemStackRequestAction.place(1, cursor, source));
                    } else if (!cursorItem.isEmpty() && !cursorItem.isDifferent(clicked)) {
                        final int movable = Math.min(1, Math.max(0, MAX_STACK_SIZE - clicked.amount()));
                        if (movable <= 0) {
                            return new ArrayList<>();
                        }
                        return List.of(ItemStackRequestAction.place(1, cursor, source));
                    } else if (!cursorItem.isEmpty() && cursorItem.isDifferent(clicked)) {
                        return List.of(ItemStackRequestAction.swap(cursor, source));
                    }
                }
                return null;
            }
            case SWAP -> {
                if (source == null) {
                    return null;
                }
                if (button >= 0 && button <= 8) {
                    return List.of(ItemStackRequestAction.swap(source, hotbarRequestSlot(button, inventoryTracker.getInventoryContainer().getItem(button))));
                } else if (button == 40) {
                    return List.of(ItemStackRequestAction.swap(source, new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.OffhandContainer, null), (byte) 1, netIdOf(inventoryTracker.getOffhandContainer().getItem(0)))));
                }
                return null;
            }
            case THROW -> {
                if (source == null || clicked.isEmpty()) {
                    return null;
                }
                if (button == 0) {
                    return List.of(ItemStackRequestAction.drop(1, source, false));
                } else if (button == 1) {
                    return List.of(ItemStackRequestAction.drop(clicked.amount(), source, false));
                }
                return null;
            }
            default -> {
                // QUICK_MOVE, CLONE, QUICK_CRAFT and PICKUP_ALL need destination computation and fall back to a resync
                return null;
            }
        }
    }

    private static void resyncClick(final UserConnection user, final InventoryTracker inventoryTracker, final Container container) {
        if (container.type() != ContainerType.INVENTORY) {
            PacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer());
        }
        PacketFactory.sendJavaContainerSetContent(user, container);
    }

    /**
     * Resolves a container from a response FullContainerName to the tracked container.
     */
    private static Container resolveResponseContainer(final UserConnection user, final InventoryTracker inventoryTracker, final FullContainerName containerName) {
        if (containerName == null) {
            return null;
        }
        return switch (containerName.name()) {
            case InventoryContainer, HotbarContainer, CombinedHotbarAndInventoryContainer -> inventoryTracker.getInventoryContainer();
            case CursorContainer -> inventoryTracker.getHudContainer();
            case CraftingInputContainer -> inventoryTracker.getCurrentContainer() instanceof CraftingTableContainer table ? table : inventoryTracker.getHudContainer();
            case CreatedOutputContainer -> null;
            case ArmorContainer -> inventoryTracker.getArmorContainer();
            case OffhandContainer -> inventoryTracker.getOffhandContainer();
            case LevelEntityContainer, CrafterLevelEntityContainer -> inventoryTracker.getCurrentContainer();
            default -> {
                // Per-type container names (anvil input, furnace fuel, ...) all address the open container
                final Container currentContainer = inventoryTracker.getCurrentContainer();
                yield currentContainer != null ? currentContainer : inventoryTracker.getContainerClientbound((byte) ContainerID.CONTAINER_ID_REGISTRY.getValue(), containerName, null);
            }
        };
    }

    /**
     * Java player inventory slot -> Bedrock item stack request slot info.
     * Container names follow the vanilla client: INVENTORY for the main inventory, HOTBAR for the hotbar.
     */
    private static ItemStackRequestSlot inventoryRequestSlot(final InventoryTracker inventoryTracker, final int javaSlot) {
        if (javaSlot >= 9 && javaSlot <= 35) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.InventoryContainer, null), (byte) javaSlot, netIdOf(inventoryTracker.getInventoryContainer().getItem(javaSlot)));
        } else if (javaSlot >= 36 && javaSlot <= 44) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) (javaSlot - 36), netIdOf(inventoryTracker.getInventoryContainer().getItem(javaSlot - 36)));
        } else if (javaSlot >= 5 && javaSlot <= 8) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.ArmorContainer, null), (byte) (javaSlot - 5), netIdOf(inventoryTracker.getArmorContainer().getItem(javaSlot - 5)));
        } else if (javaSlot == 45) {
            // The vanilla client sends slot 1 for the offhand (a known client quirk since 1.19.70)
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.OffhandContainer, null), (byte) 1, netIdOf(inventoryTracker.getOffhandContainer().getItem(0)));
        } else if (javaSlot >= 1 && javaSlot <= 4) {
            // The vanilla client uses the UI slot offsets 28-31 for the 2x2 crafting input
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) (28 + javaSlot - 1), netIdOf(inventoryTracker.getHudContainer().getItem(28 + javaSlot - 1)));
        }
        return null; // Crafting result slot and unknown slots
    }

    private static ItemStackRequestSlot requestSlotInfo(final InventoryTracker inventoryTracker, final Container container, final int javaSlot) {
        if (container.type() == ContainerType.INVENTORY || container == inventoryTracker.getInventoryContainer()) {
            return inventoryRequestSlot(inventoryTracker, javaSlot);
        }
        // Open containers are anchored to block entities: Bedrock networked as level entity containers
        final int bedrockSlot = container.bedrockSlot(javaSlot);
        if (container instanceof CraftingTableContainer) { // The 3x3 grid is addressed through the crafting input UI slots 32-40
            if (bedrockSlot < 0 || bedrockSlot >= 9) return null;
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) (32 + bedrockSlot), netIdOf(container.getItem(bedrockSlot)));
        }
        final ContainerEnumName containerName = bedrockContainerName(container.type(), bedrockSlot);
        if (bedrockSlot < 0 || bedrockSlot >= container.size()) {
            return null;
        }
        return new ItemStackRequestSlot(new FullContainerName(containerName, null), (byte) bedrockSlot, netIdOf(container.getItem(bedrockSlot)));
    }

    private static ItemStackRequestSlot cursorSlot(final InventoryTracker inventoryTracker) {
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CursorContainer, null), (byte) 0, netIdOf(inventoryTracker.getHudContainer().getItem(0)));
    }

    private static ItemStackRequestSlot hotbarRequestSlot(final int hotbarSlot, final BedrockItem item) {
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) hotbarSlot, netIdOf(item));
    }

    private static int netIdOf(final BedrockItem item) {
        return item == null || item.isEmpty() || item.netId() == null ? 0 : item.netId();
    }

    /**
     * Client-authoritative path: translates a Java container click into a legacy inventory transaction.
     * Returns false when the click can't be mapped and the containers need a resync instead.
     */
    private static boolean translateClickToInventoryTransaction(final UserConnection user, final InventoryTracker inventoryTracker, final Container container, final int javaSlot, final byte button, final ContainerInput action) {
        final int bedSlot = container.bedrockSlot(javaSlot & 0xFFFF);
        if (bedSlot < 0 || bedSlot >= container.size()) {
            return false;
        }
        final BedrockItem clicked = container.getItem(bedSlot);
        final BedrockItem cursorItem = inventoryTracker.getHudContainer().getItem(0);
        final InventorySource slotSource = new InventorySource(InventorySourceType.Container_Inventory, container.containerId(), InventorySource_InventorySourceFlags.No_Flag);
        final InventorySource cursorSource = new InventorySource(InventorySourceType.Container_Inventory, ContainerID.CONTAINER_ID_PLAYER_ONLY_UI.getValue(), InventorySource_InventorySourceFlags.No_Flag);

        final List<InventoryActionData> actions = new ArrayList<>();
        switch (action) {
            case PICKUP -> {
                if (cursorItem.isEmpty() && clicked.isEmpty()) {
                    return true; // No-op click
                }
                if (button == 0) {
                    if (cursorItem.isEmpty() && !clicked.isEmpty()) {
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, BedrockItem.empty()));
                        actions.add(new InventoryActionData(cursorSource, 0, BedrockItem.empty(), clicked.copy()));
                        container.setItem(bedSlot, BedrockItem.empty());
                        inventoryTracker.getHudContainer().setItem(0, clicked.copy());
                    } else if (!cursorItem.isEmpty() && clicked.isEmpty()) {
                        actions.add(new InventoryActionData(cursorSource, 0, cursorItem, BedrockItem.empty()));
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, cursorItem.copy()));
                        container.setItem(bedSlot, cursorItem.copy());
                        inventoryTracker.getHudContainer().setItem(0, BedrockItem.empty());
                    } else if (!cursorItem.isEmpty() && !cursorItem.isDifferent(clicked)) {
                        // Placing onto the same item type: cap at the max stack size
                        final int movable = Math.min(cursorItem.amount(), Math.max(0, MAX_STACK_SIZE - clicked.amount()));
                        if (movable <= 0) {
                            return true;
                        }
                        final BedrockItem newCursor;
                        if (cursorItem.amount() > movable) {
                            newCursor = cursorItem.copy();
                            newCursor.setAmount(cursorItem.amount() - movable);
                        } else {
                            newCursor = BedrockItem.empty();
                        }
                        final BedrockItem newSlot = clicked.copy();
                        newSlot.setAmount(clicked.amount() + movable);
                        actions.add(new InventoryActionData(cursorSource, 0, cursorItem, newCursor));
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, newSlot));
                        container.setItem(bedSlot, newSlot);
                        inventoryTracker.getHudContainer().setItem(0, newCursor);
                    } else if (!cursorItem.isEmpty() && cursorItem.isDifferent(clicked)) {
                        actions.add(new InventoryActionData(cursorSource, 0, cursorItem, clicked.copy()));
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, cursorItem.copy()));
                        container.setItem(bedSlot, cursorItem.copy());
                        inventoryTracker.getHudContainer().setItem(0, clicked.copy());
                    } else {
                        return false;
                    }
                } else if (button == 1) {
                    if (cursorItem.isEmpty() && !clicked.isEmpty()) {
                        final BedrockItem half = clicked.copy();
                        half.setAmount((clicked.amount() + 1) / 2);
                        final BedrockItem remaining = clicked.copy();
                        remaining.setAmount(clicked.amount() - half.amount());
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, remaining));
                        actions.add(new InventoryActionData(cursorSource, 0, BedrockItem.empty(), half));
                        container.setItem(bedSlot, remaining);
                        inventoryTracker.getHudContainer().setItem(0, half);
                    } else if (!cursorItem.isEmpty() && (clicked.isEmpty() || !cursorItem.isDifferent(clicked))) {
                        if (clicked.amount() >= MAX_STACK_SIZE) {
                            return true; // Can't place more onto a full stack
                        }
                        // Place one item from the cursor
                        final BedrockItem newCursor;
                        if (cursorItem.amount() > 1) {
                            newCursor = cursorItem.copy();
                            newCursor.setAmount(cursorItem.amount() - 1);
                        } else {
                            newCursor = BedrockItem.empty();
                        }
                        final BedrockItem newSlot = clicked.isEmpty() ? cursorItem.copy() : clicked.copy();
                        newSlot.setAmount(clicked.amount() + 1);
                        actions.add(new InventoryActionData(cursorSource, 0, cursorItem, newCursor));
                        actions.add(new InventoryActionData(slotSource, bedSlot, clicked, newSlot));
                        container.setItem(bedSlot, newSlot);
                        inventoryTracker.getHudContainer().setItem(0, newCursor);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            case THROW -> {
                if (clicked.isEmpty()) {
                    return true;
                }
                final BedrockItem dropped = clicked.copy();
                dropped.setAmount(button == 0 ? 1 : Math.max(1, clicked.amount()));
                final BedrockItem predictedTo;
                if (button == 0 && clicked.amount() > 1) {
                    predictedTo = clicked.copy();
                    predictedTo.setAmount(clicked.amount() - 1);
                } else {
                    predictedTo = BedrockItem.empty();
                }
                actions.add(new InventoryActionData(new InventorySource(InventorySourceType.World_Interaction, ContainerID.CONTAINER_ID_NONE.getValue(), InventorySource_InventorySourceFlags.No_Flag), 0, BedrockItem.empty(), dropped));
                actions.add(new InventoryActionData(slotSource, bedSlot, clicked, predictedTo));
                container.setItem(bedSlot, predictedTo);
            }
            default -> {
                return false;
            }
        }

        final BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                0, // legacy request id
                null,
                actions,
                ComplexInventoryTransaction_Type.NormalTransaction,
                new InventoryTransactionData.NormalTransactionData()
        );
        final PacketWrapper transactionPacket = PacketWrapper.create(ServerboundBedrockPackets.INVENTORY_TRANSACTION, user);
        transactionPacket.write(user.get(InventoryTransactionRewriter.class).getInventoryTransactionType(), inventoryTransaction);
        transactionPacket.sendToServer(BedrockProtocol.class);
        return true;
    }


    private static String[] blockTags(final String... tags) {
        final java.util.List<String> known = new java.util.ArrayList<>();
        for (String tag : tags) {
            if (BedrockProtocol.MAPPINGS.getBedrockCustomBlockTags().containsValue(tag)) {
                known.add(tag);
            }
        }
        return known.toArray(new String[0]);
    }


    private record TrackedSlot(Container container, int slot) {
    }

    private static TrackedSlot resolveRequestSlot(final InventoryTracker inventoryTracker, final ItemStackRequestSlot slot) {
        if (slot == null || slot.containerName() == null) {
            return null;
        }
        final int index = slot.slot() & 0xFF;
        final Container container = switch (slot.containerName().name()) {
            case InventoryContainer, HotbarContainer, CombinedHotbarAndInventoryContainer -> inventoryTracker.getInventoryContainer();
            case CursorContainer -> inventoryTracker.getHudContainer();
            case ArmorContainer -> inventoryTracker.getArmorContainer();
            case OffhandContainer -> inventoryTracker.getOffhandContainer();
            case CraftingInputContainer -> index >= 32 && inventoryTracker.getCurrentContainer() instanceof CraftingTableContainer table ? table : inventoryTracker.getHudContainer();
            default -> inventoryTracker.getCurrentContainer();
        };
        if (container == null) {
            return null;
        }
        final int resolvedIndex = slot.containerName().name() == ContainerEnumName.OffhandContainer ? 0 : container instanceof CraftingTableContainer ? index - 32 : index;
        if (resolvedIndex < 0 || resolvedIndex >= container.size()) {
            return null;
        }
        return new TrackedSlot(container, resolvedIndex);
    }

    private static List<BedrockItem> snapshotSources(final InventoryTracker inventoryTracker, final List<ItemStackRequestAction> actions) {
        final List<BedrockItem> snapshots = new ArrayList<>(actions.size());
        for (ItemStackRequestAction action : actions) {
            if (action.source() != null && action.source().containerName() != null && action.source().containerName().name() == ContainerEnumName.CreatedOutputContainer) {
                snapshots.add(inventoryTracker.matchedRecipe() != null ? inventoryTracker.matchedRecipe().result().copy() : null);
                continue;
            }
            final TrackedSlot from = resolveRequestSlot(inventoryTracker, action.source());
            final BedrockItem item = from != null ? from.container().getItem(from.slot()) : null;
            snapshots.add(item == null || item.isEmpty() ? null : item.copy());
        }
        return snapshots;
    }

    private static void moveTracked(final TrackedSlot from, final TrackedSlot to, final int amount, final BedrockItem sourceSnapshot) {
        if (from == null && to != null && sourceSnapshot != null) { // Crafted output: nothing to take it from
            final BedrockItem target = to.container().getItem(to.slot());
            final BedrockItem placed = (target == null || target.isEmpty()) ? sourceSnapshot.copy() : target.copy();
            placed.setAmount((target == null || target.isEmpty()) ? amount : target.amount() + amount);
            to.container().setItem(to.slot(), placed);
            return;
        }
        if (from == null || to == null) {
            return;
        }
        BedrockItem source = from.container().getItem(from.slot());
        if (source == null || source.isEmpty()) {
            if (sourceSnapshot == null) {
                return;
            }
            // The server already emptied the source slot before answering: move the snapshot taken at request time
            source = sourceSnapshot;
        }
        final int moved = Math.min(amount, source.amount());
        final BedrockItem target = to.container().getItem(to.slot());
        if (target == null || target.isEmpty()) {
            final BedrockItem placed = source.copy();
            placed.setAmount(moved);
            to.container().setItem(to.slot(), placed);
        } else {
            final BedrockItem merged = target.copy();
            merged.setAmount(target.amount() + moved);
            to.container().setItem(to.slot(), merged);
        }
        if (source == sourceSnapshot) {
            return; // The source slot is already empty on our side
        }
        if (source.amount() - moved <= 0) {
            from.container().setItem(from.slot(), BedrockItem.empty());
        } else {
            final BedrockItem remaining = source.copy();
            remaining.setAmount(source.amount() - moved);
            from.container().setItem(from.slot(), remaining);
        }
    }

    private static void applyAcceptedActions(final InventoryTracker inventoryTracker, final List<ItemStackRequestAction> actions, final List<BedrockItem> sourceSnapshots) {
        for (int i = 0; i < actions.size(); i++) {
            final ItemStackRequestAction action = actions.get(i);
            final BedrockItem snapshot = sourceSnapshots != null && i < sourceSnapshots.size() ? sourceSnapshots.get(i) : null;
            switch (action.type()) {
                case Take, Place, PlaceInItemContainer, TakeFromItemContainer -> moveTracked(resolveRequestSlot(inventoryTracker, action.source()), resolveRequestSlot(inventoryTracker, action.destination()), action.amount() == null ? 0 : action.amount(), snapshot);
                case Swap -> {
                    final TrackedSlot a = resolveRequestSlot(inventoryTracker, action.source());
                    final TrackedSlot b = resolveRequestSlot(inventoryTracker, action.destination());
                    if (a != null && b != null) {
                        final BedrockItem itemA = a.container().getItem(a.slot());
                        final BedrockItem itemB = b.container().getItem(b.slot());
                        a.container().setItem(a.slot(), itemB == null ? BedrockItem.empty() : itemB.copy());
                        b.container().setItem(b.slot(), itemA == null ? BedrockItem.empty() : itemA.copy());
                    }
                }
                case Drop, Destroy, Consume -> {
                    final TrackedSlot from = resolveRequestSlot(inventoryTracker, action.source());
                    if (from != null && action.amount() != null) {
                        final BedrockItem source = from.container().getItem(from.slot());
                        if (source != null && !source.isEmpty()) {
                            if (source.amount() - action.amount() <= 0) {
                                from.container().setItem(from.slot(), BedrockItem.empty());
                            } else {
                                final BedrockItem remaining = source.copy();
                                remaining.setAmount(source.amount() - action.amount());
                                from.container().setItem(from.slot(), remaining);
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
    }


    private static ItemStackRequestSlot playerInventorySlot(final InventoryTracker inventoryTracker, final int bedrockIndex) {
        final BedrockItem item = inventoryTracker.getInventoryContainer().getItem(bedrockIndex);
        if (bedrockIndex < 9) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) bedrockIndex, netIdOf(item));
        }
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.InventoryContainer, null), (byte) bedrockIndex, netIdOf(item));
    }

    private static int armorSlotFor(final InventoryTracker inventoryTracker, final BedrockItem item) {
        final String identifier = inventoryTracker.user().get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (identifier == null) return -1;
        if (identifier.endsWith("_helmet") || identifier.equals("minecraft:carved_pumpkin") || identifier.endsWith("_skull") || identifier.endsWith("_head")) return 0;
        if (identifier.endsWith("_chestplate") || identifier.equals("minecraft:elytra")) return 1;
        if (identifier.endsWith("_leggings")) return 2;
        if (identifier.endsWith("_boots")) return 3;
        return -1;
    }

    private static List<ItemStackRequestAction> buildQuickMoveActions(final InventoryTracker inventoryTracker, final Container viewContainer, final Container clickedContainer, final int javaSlot, final boolean containerView) {
        final Container inventory = inventoryTracker.getInventoryContainer();
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        if (clickedContainer == inventory && !containerView) {
            final Container armor = inventoryTracker.getArmorContainer();
            if (javaSlot >= 5 && javaSlot <= 8) { // Armor slot -> first free main inventory slot, then hotbar
                final int armorIndex = javaSlot - 5;
                final BedrockItem worn = armor.getItem(armorIndex);
                if (worn == null || worn.isEmpty()) return actions;
                final ItemStackRequestSlot armorSource = new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.ArmorContainer, null), (byte) armorIndex, netIdOf(worn));
                for (int i : new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 0, 1, 2, 3, 4, 5, 6, 7, 8}) {
                    final BedrockItem existing = inventory.getItem(i);
                    if (existing == null || existing.isEmpty()) {
                        actions.add(ItemStackRequestAction.place(worn.amount(), armorSource, playerInventorySlot(inventoryTracker, i)));
                        return actions;
                    }
                }
                return actions;
            }
            final int sourceIndex = inventory.bedrockSlot(javaSlot);
            if (sourceIndex >= 0 && sourceIndex < 36) { // Armor piece -> its empty armor slot
                final BedrockItem moving = inventory.getItem(sourceIndex);
                if (moving != null && !moving.isEmpty()) {
                    final int armorIndex = armorSlotFor(inventoryTracker, moving);
                    if (armorIndex != -1) {
                        final BedrockItem worn = armor.getItem(armorIndex);
                        if (worn == null || worn.isEmpty()) {
                            actions.add(ItemStackRequestAction.place(1, playerInventorySlot(inventoryTracker, sourceIndex), new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.ArmorContainer, null), (byte) armorIndex, 0)));
                            return actions;
                        }
                    }
                }
            }
        }
        final int sourceIndex = clickedContainer.bedrockSlot(javaSlot);
        if (sourceIndex < 0 || sourceIndex >= clickedContainer.size()) {
            return null;
        }
        final BedrockItem moving = clickedContainer.getItem(sourceIndex);
        if (moving == null || moving.isEmpty()) {
            return actions;
        }
        final ItemStackRequestSlot source = clickedContainer == inventory ? playerInventorySlot(inventoryTracker, sourceIndex) : requestSlotInfo(inventoryTracker, clickedContainer, javaSlot);
        if (source == null) {
            return null;
        }

        // Destination candidates, in the order the client fills them
        final List<ItemStackRequestSlot> candidates = new ArrayList<>();
        final List<BedrockItem> candidateItems = new ArrayList<>();
        if (clickedContainer == inventory && containerView) { // Player inventory -> open container
            for (int i = 0; i < viewContainer.size(); i++) {
                if (isFurnaceType(viewContainer.type()) && i == 2) continue; // Result slot never accepts items
                final ItemStackRequestSlot slot = requestSlotInfo(inventoryTracker, viewContainer, viewContainer.javaSlot(i));
                if (slot != null) {
                    candidates.add(slot);
                    candidateItems.add(viewContainer.getItem(i));
                }
            }
        } else if (clickedContainer == inventory) { // Inside the player inventory: hotbar <-> main inventory
            if (sourceIndex < 9) {
                for (int i = 9; i < 36; i++) {
                    candidates.add(playerInventorySlot(inventoryTracker, i));
                    candidateItems.add(inventory.getItem(i));
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    candidates.add(playerInventorySlot(inventoryTracker, i));
                    candidateItems.add(inventory.getItem(i));
                }
            }
        } else if (isFurnaceType(viewContainer.type()) && sourceIndex != 2) { // Furnace input/fuel -> player inventory: Java fills main inventory first, then the hotbar
            for (int i = 9; i < 36; i++) {
                candidates.add(playerInventorySlot(inventoryTracker, i));
                candidateItems.add(inventory.getItem(i));
            }
            for (int i = 0; i < 9; i++) {
                candidates.add(playerInventorySlot(inventoryTracker, i));
                candidateItems.add(inventory.getItem(i));
            }
        } else { // Open container -> player inventory, in Java's order: hotbar right to left, then main inventory bottom-right to top-left
            for (int i = 8; i >= 0; i--) {
                candidates.add(playerInventorySlot(inventoryTracker, i));
                candidateItems.add(inventory.getItem(i));
            }
            for (int i = 35; i >= 9; i--) {
                candidates.add(playerInventorySlot(inventoryTracker, i));
                candidateItems.add(inventory.getItem(i));
            }
        }
        for (int i = candidates.size() - 1; i >= 0; i--) { // Never move a stack onto itself
            final ItemStackRequestSlot candidate = candidates.get(i);
            if (candidate.containerName().name() == source.containerName().name() && candidate.slot() == source.slot()) {
                candidates.remove(i);
                candidateItems.remove(i);
            }
        }

        int remaining = moving.amount();
        final int maxStack = MAX_STACK_SIZE;
        for (int i = 0; i < candidates.size() && remaining > 0; i++) { // Merge into matching stacks first
            final BedrockItem existing = candidateItems.get(i);
            if (existing != null && !existing.isEmpty() && !existing.isDifferent(moving) && existing.amount() < maxStack) {
                final int amount = Math.min(remaining, maxStack - existing.amount());
                actions.add(ItemStackRequestAction.place(amount, source, candidates.get(i)));
                remaining -= amount;
            }
        }
        for (int i = 0; i < candidates.size() && remaining > 0; i++) { // Then the first empty slot
            final BedrockItem existing = candidateItems.get(i);
            if (existing == null || existing.isEmpty()) {
                actions.add(ItemStackRequestAction.place(remaining, source, candidates.get(i)));
                remaining = 0;
            }
        }
        return actions;
    }

    /**
     * Returns the cursor item to the player inventory before a container closes (the Java client keeps it otherwise).
     */
    static void returnCursorToInventory(final UserConnection user, final InventoryTracker inventoryTracker) {
        final BedrockItem held = inventoryTracker.getHudContainer().getItem(0);
        if (held == null || held.isEmpty() || !user.get(GameSessionStorage.class).isInventoryServerAuthoritative()) {
            return;
        }
        final Container inventory = inventoryTracker.getInventoryContainer();
        int target = -1;
        for (int i = 0; i < 36 && target == -1; i++) {
            final BedrockItem existing = inventory.getItem(i);
            if (existing != null && !existing.isEmpty() && !existing.isDifferent(held) && existing.amount() + held.amount() <= MAX_STACK_SIZE) target = i;
        }
        for (int i = 0; i < 36 && target == -1; i++) {
            final BedrockItem existing = inventory.getItem(i);
            if (existing == null || existing.isEmpty()) target = i;
        }
        final List<ItemStackRequestAction> actions = target == -1
                ? List.of(ItemStackRequestAction.drop(held.amount(), cursorSlot(inventoryTracker), false))
                : List.of(ItemStackRequestAction.place(held.amount(), cursorSlot(inventoryTracker), playerInventorySlot(inventoryTracker, target)));
        final ItemStackRequest request = new ItemStackRequest(inventoryTracker.nextItemStackRequestId(), actions, new ArrayList<>(), 0);
        ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Item stack request (return cursor on close): id=" + request.requestId() + " actions=" + actions);
        inventoryTracker.trackItemStackRequest(request.requestId(), actions, snapshotSources(inventoryTracker, actions));
        final PacketWrapper requestPacket = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, user);
        requestPacket.write(BedrockTypes.ITEM_STACK_REQUEST, request);
        requestPacket.sendToServer(BedrockProtocol.class);
    }


    private static boolean isFurnaceType(final ContainerType type) {
        return type == ContainerType.FURNACE || type == ContainerType.BLAST_FURNACE || type == ContainerType.SMOKER;
    }

    private static ContainerEnumName bedrockContainerName(final ContainerType type, final int bedrockSlot) {
        if (isFurnaceType(type)) {
            return switch (bedrockSlot) {
                case 0 -> type == ContainerType.BLAST_FURNACE ? ContainerEnumName.BlastFurnaceIngredientContainer : type == ContainerType.SMOKER ? ContainerEnumName.SmokerIngredientContainer : ContainerEnumName.FurnaceIngredientContainer;
                case 1 -> ContainerEnumName.FurnaceFuelContainer;
                default -> ContainerEnumName.FurnaceResultContainer;
            };
        }
        return type == ContainerType.CRAFTER ? ContainerEnumName.CrafterLevelEntityContainer : ContainerEnumName.LevelEntityContainer;
    }


    private record QueuedClick(int containerId, int revision, short slot, byte button, ContainerInput action) {
    }

    private static void drainQueuedClicks(final UserConnection user, final InventoryTracker inventoryTracker) {
        while (!inventoryTracker.queuedClicks().isEmpty() && !inventoryTracker.hasPendingItemStackRequests()) {
            final QueuedClick click = (QueuedClick) inventoryTracker.queuedClicks().poll();
            processContainerClick(user, click.containerId(), click.revision(), click.slot(), click.button(), click.action());
        }
    }

    private static void processContainerClick(final UserConnection user, final int containerId, final int revision, final short slot, final byte button, final ContainerInput action) {
        final GameSessionStorage gameSession = user.get(GameSessionStorage.class);
        final InventoryTracker inventoryTracker = user.get(InventoryTracker.class);

        if (inventoryTracker.getPendingCloseContainer() != null) {
            return;
        }
        final Container container = inventoryTracker.getContainerServerbound((byte) containerId);
        if (container == null) {
            if (containerId == ContainerID.CONTAINER_ID_INVENTORY.getValue()) {
                // Bedrock client can send multiple OpenInventory requests if the server doesn't respond, so this is fine here
                final PacketWrapper interact = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, user);
                interact.write(Types.UNSIGNED_BYTE, (short) InteractPacketPayload_Action.OpenInventory.getValue()); // action
                interact.write(BedrockTypes.UNSIGNED_VAR_LONG, user.get(EntityTracker.class).getClientPlayer().runtimeId()); // target entity runtime id
                interact.write(BedrockTypes.OPTIONAL_POSITION_3F, null); // position
                interact.sendToServer(BedrockProtocol.class);
                ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Sent INTERACT OpenInventory to the server");
                PacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer());
            }
            return;
        }

        final List<ItemStackRequestAction> actions;
        final BedrockItem trackedCursor = inventoryTracker.getHudContainer().getItem(0);
        ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Container click: container=" + container.type() + " slot=" + slot + " button=" + button + " action=" + action + " serverAuthoritative=" + gameSession.isInventoryServerAuthoritative() + " trackedCursor=" + (trackedCursor == null || trackedCursor.isEmpty() ? "empty" : trackedCursor.identifier() + " x" + trackedCursor.amount() + " netId=" + trackedCursor.netId()));
        if (gameSession.isInventoryServerAuthoritative()) {
            actions = buildItemStackRequestActions(inventoryTracker, container, slot, button, action);
        } else {
            // Client-authoritative: clicks are communicated with legacy inventory transactions
            actions = null;
            if (!translateClickToInventoryTransaction(user, inventoryTracker, container, slot, button, action)) {
                resyncClick(user, inventoryTracker, container);
            }
        }
        if (actions != null && !actions.isEmpty()) {
            final ItemStackRequest request = new ItemStackRequest(inventoryTracker.nextItemStackRequestId(), actions, new ArrayList<>(), 0);
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Item stack request: id=" + request.requestId() + " actions=" + actions);
            inventoryTracker.trackItemStackRequest(request.requestId(), actions, snapshotSources(inventoryTracker, actions));
            final PacketWrapper requestPacket = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, user);
            requestPacket.write(BedrockTypes.ITEM_STACK_REQUEST, request);
            requestPacket.sendToServer(BedrockProtocol.class);
        } else if (gameSession.isInventoryServerAuthoritative() && actions == null) {
            resyncClick(user, inventoryTracker, container);
        }
    }


    private static InventoryTracker.Ingredient readRecipeIngredient(final PacketWrapper wrapper) {
        final int typeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // descriptor type
        InventoryTracker.Ingredient ingredient;
        if (typeId == 0) {
            wrapper.read(BedrockTypes.VAR_INT); // aux
            ingredient = new InventoryTracker.Ingredient(0, null, 0, 0);
        } else {
            final String kind = wrapper.read(BedrockTypes.STRING); // descriptor name
            switch (kind) {
                case "default" -> {
                    final String id = wrapper.read(BedrockTypes.STRING);
                    final int aux = wrapper.read(BedrockTypes.VAR_INT);
                    ingredient = new InventoryTracker.Ingredient(1, id, aux, 0);
                }
                case "item_tag" -> {
                    final String tag = wrapper.read(BedrockTypes.STRING);
                    wrapper.read(BedrockTypes.VAR_INT); // aux
                    ingredient = new InventoryTracker.Ingredient(2, tag, 0, 0);
                }
                case "molang" -> {
                    final String expression = wrapper.read(BedrockTypes.STRING);
                    wrapper.read(BedrockTypes.SHORT_LE); // molang version
                    ingredient = new InventoryTracker.Ingredient(3, expression, 0, 0);
                }
                default -> throw new IllegalStateException("Unknown recipe item descriptor: " + kind);
            }
        }
        final int count = wrapper.read(BedrockTypes.VAR_INT); // count
        return new InventoryTracker.Ingredient(ingredient.kind(), ingredient.value(), ingredient.aux(), count);
    }

    private static boolean ingredientMatches(final UserConnection user, final InventoryTracker.Ingredient ingredient, final BedrockItem item) {
        final boolean empty = item == null || item.isEmpty();
        if (ingredient.kind() == 0) return empty;
        if (empty) return false;
        final String identifier = user.get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (identifier == null) return false;
        return switch (ingredient.kind()) {
            case 1 -> Key.namespaced(ingredient.value()).equals(identifier) && (ingredient.aux() == 32767 || ingredient.aux() == -1 || ingredient.aux() == item.data());
            case 2 -> {
                final java.util.Set<String> tags = BedrockProtocol.MAPPINGS.getBedrockItemTags().get(identifier);
                yield tags != null && tags.contains(Key.namespaced(ingredient.value())) || tags != null && tags.contains(ingredient.value());
            }
            default -> false;
        };
    }

    private static BedrockItem[] craftingGrid(final InventoryTracker inventoryTracker, final Container viewContainer) {
        if (viewContainer instanceof CraftingTableContainer table) {
            return table.getItems();
        }
        final BedrockItem[] grid = new BedrockItem[4];
        for (int i = 0; i < 4; i++) grid[i] = inventoryTracker.getHudContainer().getItem(28 + i);
        return grid;
    }

    private static InventoryTracker.Recipe matchRecipe(final UserConnection user, final InventoryTracker inventoryTracker, final BedrockItem[] grid) {
        final int size = grid.length == 9 ? 3 : 2;
        int minX = size, minY = size, maxX = -1, maxY = -1;
        final List<BedrockItem> nonEmpty = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null && !grid[i].isEmpty()) {
                final int x = i % size, y = i / size;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                nonEmpty.add(grid[i]);
            }
        }
        if (nonEmpty.isEmpty()) return null;
        final int boxWidth = maxX - minX + 1, boxHeight = maxY - minY + 1;
        for (InventoryTracker.Recipe recipe : inventoryTracker.recipes()) {
            if (recipe.shaped()) {
                if (recipe.width() != boxWidth || recipe.height() != boxHeight) continue;
                for (int mirror = 0; mirror < 2; mirror++) {
                    boolean ok = true;
                    for (int y = 0; y < boxHeight && ok; y++) {
                        for (int x = 0; x < boxWidth && ok; x++) {
                            final int rx = mirror == 0 ? x : boxWidth - 1 - x;
                            final InventoryTracker.Ingredient ingredient = recipe.ingredients().get(y * recipe.width() + rx);
                            ok = ingredientMatches(user, ingredient, grid[(minY + y) * size + minX + x]);
                        }
                    }
                    if (ok) return recipe;
                }
            } else {
                final List<InventoryTracker.Ingredient> needed = new ArrayList<>();
                for (InventoryTracker.Ingredient ingredient : recipe.ingredients()) {
                    if (ingredient.kind() == 0) continue;
                    for (int c = 0; c < Math.max(1, ingredient.count()); c++) needed.add(ingredient);
                }
                if (needed.size() != nonEmpty.size() || needed.size() > size * size) continue;
                if (assignShapeless(user, needed, nonEmpty, new boolean[nonEmpty.size()], 0)) return recipe;
            }
        }
        return null;
    }

    private static boolean assignShapeless(final UserConnection user, final List<InventoryTracker.Ingredient> needed, final List<BedrockItem> items, final boolean[] used, final int index) {
        if (index == needed.size()) return true;
        for (int i = 0; i < items.size(); i++) {
            if (!used[i] && ingredientMatches(user, needed.get(index), items.get(i))) {
                used[i] = true;
                if (assignShapeless(user, needed, items, used, index + 1)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    /**
     * Recomputes the crafting result for the open crafting grid and shows it in Java slot 0.
     */
    static void updateCraftingResult(final UserConnection user, final InventoryTracker inventoryTracker) {
        final Container current = inventoryTracker.getCurrentContainer();
        if (!(current instanceof CraftingTableContainer) && (current == null || current.type() != ContainerType.INVENTORY)) {
            inventoryTracker.setMatchedRecipe(null);
            return;
        }
        final InventoryTracker.Recipe recipe = matchRecipe(user, inventoryTracker, craftingGrid(inventoryTracker, current));
        inventoryTracker.setMatchedRecipe(recipe);
        final BedrockItem result = recipe != null ? recipe.result() : BedrockItem.empty();
        if (current instanceof CraftingTableContainer table) {
            table.setResult(result);
        }
        final PacketWrapper setSlot = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_SLOT, user);
        setSlot.write(Types.VAR_INT, current instanceof CraftingTableContainer ? (int) current.javaContainerId() : (int) inventoryTracker.getInventoryContainer().javaContainerId()); // container id
        setSlot.write(Types.VAR_INT, 0); // revision
        setSlot.write(Types.SHORT, (short) 0); // slot
        setSlot.write(VersionedTypes.V26_2.item, user.get(ItemRewriter.class).javaItem(result)); // item
        setSlot.send(BedrockProtocol.class);
    }

    private static List<ItemStackRequestAction> buildCraftActions(final InventoryTracker inventoryTracker, final Container viewContainer, final ContainerInput action) {
        final InventoryTracker.Recipe recipe = inventoryTracker.matchedRecipe();
        if (recipe == null) return new ArrayList<>();
        final BedrockItem result = recipe.result();
        final BedrockItem held = inventoryTracker.getHudContainer().getItem(0);
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        final int requestId = inventoryTracker.peekNextItemStackRequestId();
        final ItemStackRequestSlot output = new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) 50, requestId);
        ItemStackRequestSlot destination;
        if (action == ContainerInput.QUICK_MOVE) {
            destination = null;
            for (int i : new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 0, 1, 2, 3, 4, 5, 6, 7, 8}) {
                final BedrockItem existing = inventoryTracker.getInventoryContainer().getItem(i);
                if (existing == null || existing.isEmpty()) {
                    destination = playerInventorySlot(inventoryTracker, i);
                    break;
                }
            }
            if (destination == null) return new ArrayList<>();
        } else {
            if (held != null && !held.isEmpty() && (held.isDifferent(result) || held.amount() + result.amount() > MAX_STACK_SIZE)) {
                return new ArrayList<>(); // Can't pick the result up onto a different or full cursor stack
            }
            destination = cursorSlot(inventoryTracker);
        }
        actions.add(new ItemStackRequestAction(ItemStackRequestActionType.CraftRecipe, null, null, null, null, null, null, null, recipe.netId(), 1, null, null, null, null, null, null, null));
        actions.add(new ItemStackRequestAction(ItemStackRequestActionType.CraftResults, null, null, null, null, null, null, null, null, 1, null, null, null, null, null, null, null));
        final BedrockItem[] grid = craftingGrid(inventoryTracker, viewContainer);
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == null || grid[i].isEmpty()) continue;
            final int uiSlot = grid.length == 9 ? 32 + i : 28 + i;
            actions.add(ItemStackRequestAction.consume(1, new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) uiSlot, netIdOf(grid[i]))));
        }
        if (action == ContainerInput.QUICK_MOVE) {
            actions.add(ItemStackRequestAction.place(result.amount(), output, destination));
        } else {
            actions.add(ItemStackRequestAction.take(result.amount(), output, destination));
        }
        return actions;
    }

    private static List<ItemStackRequestAction> buildDragActions(final InventoryTracker inventoryTracker, final Container viewContainer, final int rawJavaSlot, final byte button) {
        final int stage = button & 3; // 0 start, 1 add slot, 2 end
        if (stage == 0) {
            inventoryTracker.dragSlots().clear();
            return new ArrayList<>();
        }
        if (stage == 1) {
            if (!inventoryTracker.dragSlots().contains((short) rawJavaSlot)) inventoryTracker.dragSlots().add((short) rawJavaSlot);
            return new ArrayList<>();
        }
        final List<Short> slots = new ArrayList<>(inventoryTracker.dragSlots());
        inventoryTracker.dragSlots().clear();
        final BedrockItem held = inventoryTracker.getHudContainer().getItem(0);
        if (held == null || held.isEmpty() || slots.isEmpty()) return new ArrayList<>();
        final boolean rightDrag = (button >> 2) == 1;
        final int perSlot = rightDrag ? 1 : Math.max(1, held.amount() / slots.size());
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        int remaining = held.amount();
        for (short javaSlot : slots) {
            if (remaining <= 0) break;
            Container container = viewContainer.type() == ContainerType.INVENTORY ? inventoryTracker.getInventoryContainer() : viewContainer;
            int slot = javaSlot;
            if (viewContainer.type() != ContainerType.INVENTORY) {
                final int playerRegionStart = viewContainer.javaSlot(viewContainer.size() - 1) + 1;
                if (slot >= playerRegionStart && slot < playerRegionStart + 36) {
                    container = inventoryTracker.getInventoryContainer();
                    slot = slot - playerRegionStart + 9;
                }
            }
            final ItemStackRequestSlot target = requestSlotInfo(inventoryTracker, container, slot);
            if (target == null) continue;
            final TrackedSlot tracked = resolveRequestSlot(inventoryTracker, target);
            final BedrockItem existing = tracked != null ? tracked.container().getItem(tracked.slot()) : null;
            if (existing != null && !existing.isEmpty() && existing.isDifferent(held)) continue;
            final int amount = Math.min(perSlot, remaining);
            actions.add(ItemStackRequestAction.place(amount, cursorSlot(inventoryTracker), target));
            remaining -= amount;
        }
        return actions;
    }

}
