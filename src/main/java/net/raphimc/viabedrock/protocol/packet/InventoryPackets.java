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
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import net.lenni0451.mcstructs_bedrock.forms.Form;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.lenni0451.mcstructs_bedrock.forms.serializer.FormSerializer;
import net.lenni0451.mcstructs_bedrock.forms.types.ActionForm;
import net.lenni0451.mcstructs_bedrock.forms.types.CustomForm;
import net.lenni0451.mcstructs_bedrock.forms.types.ModalForm;
import net.lenni0451.mcstructs_bedrock.text.utils.BedrockTextUtils;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.model.container.BrewingStandContainer;
import net.raphimc.viabedrock.api.model.container.ChestContainer;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.SimpleContainer;
import net.raphimc.viabedrock.api.model.container.UiContainer;
import net.raphimc.viabedrock.api.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ComplexInventoryTransaction_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.EquipmentSlot;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.rewriter.InventoryTransactionRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static net.raphimc.viabedrock.protocol.packet.ContainerClicks.*;
import static net.raphimc.viabedrock.protocol.packet.CraftingTranslator.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackRequestSlots.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackResponses.*;
import static net.raphimc.viabedrock.protocol.packet.SpecialScreenPackets.*;

public class InventoryPackets {

    private static final int DIALOG_BUTTON_WIDTH = 200;
    private static final int DIALOG_FAKE_BUTTON_WIDTH = 300;
    private static final String DIALOG_FAKE_BUTTON_TEXT = "This is not actually a button, but has to be one because dialogs don't support adding text only elements. Clicking it has the same effect as closing the dialog.";

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
        protocol.registerClientbound(ClientboundBedrockPackets.ITEM_STACK_RESPONSE, null, ItemStackResponses::handleItemStackResponse);
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_SET_DATA, ClientboundPackets26_3.CONTAINER_SET_DATA, wrapper -> {
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
        protocol.registerClientbound(ClientboundBedrockPackets.CRAFTING_DATA, null, CraftingTranslator::handleCraftingData);
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
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_OPEN, ClientboundPackets26_3.OPEN_SCREEN, wrapper -> {
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
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "CONTAINER_OPEN from server: id=" + containerId + " type=" + type + " position=" + position);

            if (type == ContainerType.TRADE && inventoryTracker.getCurrentContainer() instanceof UiContainer open && open.type() == ContainerType.TRADE && open.containerId() == containerId) {
                wrapper.cancel(); // Already opened by UPDATE_TRADE
                return;
            }
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
                    final String blockTag = blockStateRewriter.tag(chunkTracker.getBlockState(position));
                    // Barrels and shulker boxes are addressed with their own container names in item stack requests
                    inventoryTracker.setLevelEntityContainerName(blockTag != null && blockTag.contains("barrel") ? ContainerEnumName.BarrelContainer : blockTag != null && blockTag.contains("shulker") ? ContainerEnumName.ShulkerBoxContainer : ContainerEnumName.LevelEntityContainer);
                }
                case MINECART_CHEST, CHEST_BOAT -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 27);
                case WORKBENCH -> container = new CraftingTableContainer(wrapper.user(), containerId, new TranslationComponent("container.crafting"), position, blockTags("crafting_table")); // Java slot 0 is the result slot
                case CRAFTER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 10, blockTags("crafter"));
                case FURNACE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("furnace"));
                case BLAST_FURNACE -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("blast_furnace"));
                case SMOKER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 3, blockTags("smoker"));
                case ANVIL -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_ANVIL_INPUT, UI_ANVIL_MATERIAL, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.AnvilInputContainer, ContainerEnumName.AnvilMaterialContainer, ContainerEnumName.AnvilResultPreviewContainer}, blockTags("anvil"));
                case GRINDSTONE -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_GRINDSTONE_INPUT, UI_GRINDSTONE_ADDITIONAL, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.GrindstoneInputContainer, ContainerEnumName.GrindstoneAdditionalContainer, ContainerEnumName.GrindstoneResultPreviewContainer}, blockTags("grindstone"));
                case ENCHANTMENT -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_ENCHANTING_INPUT, UI_ENCHANTING_MATERIAL}, new ContainerEnumName[]{ContainerEnumName.EnchantingInputContainer, ContainerEnumName.EnchantingMaterialContainer}, blockTags("enchanting_table"));
                case BREWING_STAND -> container = new BrewingStandContainer(wrapper.user(), containerId, title, position, blockTags("brewing_stand"));
                case DISPENSER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 9, blockTags("dispenser"));
                case DROPPER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 9, blockTags("dropper"));
                case HOPPER, MINECART_HOPPER -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 5, blockTags("hopper"));
                case BEACON -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_BEACON_PAYMENT}, new ContainerEnumName[]{ContainerEnumName.BeaconPaymentContainer}, blockTags("beacon"));
                case TRADE -> container = tradeContainer(wrapper.user(), containerId, title, null, true);
                case LOOM -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_LOOM_INPUT, UI_LOOM_DYE, UI_LOOM_MATERIAL, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.LoomInputContainer, ContainerEnumName.LoomDyeContainer, ContainerEnumName.LoomMaterialContainer, ContainerEnumName.LoomResultPreviewContainer}, blockTags("loom"));
                case LECTERN -> container = new SimpleContainer(wrapper.user(), containerId, type, title, position, 1, blockTags("lectern"));
                case STONECUTTER -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_STONECUTTER_INPUT, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.StonecutterInputContainer, ContainerEnumName.StonecutterResultPreviewContainer}, blockTags("stonecutter"));
                case CARTOGRAPHY -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_CARTOGRAPHY_INPUT, UI_CARTOGRAPHY_ADDITIONAL, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.CartographyInputContainer, ContainerEnumName.CartographyAdditionalContainer, ContainerEnumName.CartographyResultPreviewContainer}, blockTags("cartography_table"));
                case SMITHING_TABLE -> container = new UiContainer(wrapper.user(), containerId, type, title, position, new int[]{UI_SMITHING_TEMPLATE, UI_SMITHING_INPUT, UI_SMITHING_MATERIAL, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.SmithingTableTemplateContainer, ContainerEnumName.SmithingTableInputContainer, ContainerEnumName.SmithingTableMaterialContainer, ContainerEnumName.SmithingTableResultPreviewContainer}, blockTags("smithing_table"));
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
            if (type == ContainerType.BEACON) { // The beacon screen needs its data after it opened
                wrapper.send(BedrockProtocol.class);
                wrapper.cancel();
                int primary = -1, secondary = -1;
                if (blockEntity != null && blockEntity.tag() != null) {
                    primary = javaEffectId(blockEntity.tag().getInt("primary"));
                    secondary = javaEffectId(blockEntity.tag().getInt("secondary"));
                }
                sendJavaContainerData(wrapper.user(), container, 0, 4); // pyramid levels: enables every effect button, the server checks the real pyramid
                sendJavaContainerData(wrapper.user(), container, 1, primary + 1); // primary effect (registry id + 1, 0 = none)
                sendJavaContainerData(wrapper.user(), container, 2, secondary + 1); // secondary effect
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_CLOSE, ClientboundPackets26_3.CONTAINER_CLOSE, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.BYTE, Types.VAR_INT); // container id
                handler(wrapper -> {
                    final ContainerType containerType = ContainerType.getByValue(wrapper.read(Types.BYTE)); // type
                    final boolean serverInitiated = wrapper.read(Types.BOOLEAN); // server initiated
                    ViaBedrock.getPlatform().getLogger().log(Level.FINE, "CONTAINER_CLOSE from server: id=" + wrapper.get(Types.VAR_INT, 0) + " type=" + containerType + " serverInitiated=" + serverInitiated);

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
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_CONTENT, ClientboundPackets26_3.CONTAINER_SET_CONTENT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final BedrockItem[] items = wrapper.read(itemRewriter.newItemArrayType()); // items
            final FullContainerName containerName = wrapper.read(BedrockTypes.FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.newItemType()); // storage item

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final Container container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "INVENTORY_CONTENT from server: containerId=" + containerId + " items=" + items.length + " name=" + containerName + " -> " + (container == null ? "unknown container" : container.type()));
            if (container != null && container.setItems(items)) {
                if (container.type() == ContainerType.HUD && inventoryTracker.getCurrentContainer() instanceof UiContainer uiContainer) {
                    for (int i = 0; i < uiContainer.size(); i++) {
                        final int uiSlot = uiContainer.uiSlot(i);
                        if (uiSlot >= 0 && uiSlot < items.length) {
                            uiContainer.setItem(i, items[uiSlot].copy());
                        }
                    }
                    PacketFactory.sendJavaContainerSetContent(wrapper.user(), uiContainer);
                    updateUiScreenResult(wrapper.user(), inventoryTracker);
                }
                PacketFactory.writeJavaContainerSetContent(wrapper, container);
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_SLOT, ClientboundPackets26_3.CONTAINER_SET_SLOT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final int slot = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // slot
            final FullContainerName containerName = wrapper.read(BedrockTypes.OPTIONAL_FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.optionalNewItemType()); // storage item
            final BedrockItem item = wrapper.read(itemRewriter.newItemType()); // item

            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final Container container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "INVENTORY_SLOT from server: containerId=" + containerId + " slot=" + slot + " name=" + containerName + " item=" + (item.isEmpty() ? "empty" : item.identifier() + " x" + item.amount() + " netId=" + item.netId()) + " -> " + (container == null ? "unknown container" : container.type()));
            if (container != null && container.type() == ContainerType.HUD && inventoryTracker.getCurrentContainer() instanceof UiContainer uiContainer && uiContainer.slotOfUiSlot(slot) != -1) {
                // Anvil, enchanting, beacon, trade, ... items live in the UI container: show them in the open Java screen
                final int index = uiContainer.slotOfUiSlot(slot);
                container.setItem(slot, item);
                uiContainer.setItem(index, item.copy());
                wrapper.write(Types.VAR_INT, (int) uiContainer.javaContainerId()); // container id
                wrapper.write(Types.VAR_INT, 0); // revision
                wrapper.write(Types.SHORT, (short) uiContainer.javaSlot(index)); // slot
                wrapper.write(VersionedTypes.V26_3.item, uiContainer.getJavaItem(index)); // item
                wrapper.send(BedrockProtocol.class);
                wrapper.cancel();
                updateUiScreenResult(wrapper.user(), inventoryTracker);
                return;
            }
            if (container != null && container.setItem(slot, item)) {
                if (container.type() == ContainerType.HUD && slot == 0) { // cursor item
                    wrapper.setPacketType(ClientboundPackets26_3.SET_CURSOR_ITEM);
                } else {
                    wrapper.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
                    wrapper.write(Types.VAR_INT, 0); // revision
                    wrapper.write(Types.SHORT, (short) container.javaSlot(slot)); // slot
                }
                wrapper.write(VersionedTypes.V26_3.item, container.getJavaItem(slot)); // item
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MODAL_FORM_REQUEST, ClientboundPackets26_3.SHOW_DIALOG, wrapper -> {
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
        protocol.registerClientbound(ClientboundBedrockPackets.CLOSE_FORM, ClientboundPackets26_3.CLEAR_DIALOG, wrapper -> {
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            if (inventoryTracker.getCurrentForm() != null) {
                inventoryTracker.closeCurrentForm();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_HOTBAR, ClientboundPackets26_3.SET_HELD_SLOT, wrapper -> {
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
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_ARMOR_DAMAGE, ClientboundPackets26_3.SET_EQUIPMENT, wrapper -> {
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
                wrapper.write(VersionedTypes.V26_3.item, wrapper.user().get(ItemRewriter.class).javaItem(item)); // item
            }
        });

        protocol.registerServerbound(ServerboundPackets26_3.CONTAINER_CLICK, null, wrapper -> {
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
            if (!inventoryTracker.hasPendingItemStackRequests()) {
                drainQueuedClicks(wrapper.user(), inventoryTracker); // Clicks queued behind a request whose response never came
            }
            if (inventoryTracker.hasPendingItemStackRequests()) {
                // Wait for the server to confirm the previous move, otherwise this click is built on a stale cursor
                inventoryTracker.queuedClicks().add(new InventoryTracker.QueuedClick(containerId, revision, slot, button, action));
                return;
            }
            processContainerClick(wrapper.user(), containerId, revision, slot, button, action);
        });
        SpecialScreenPackets.register(protocol);
        protocol.registerServerbound(ServerboundPackets26_3.SET_CREATIVE_MODE_SLOT, null, wrapper -> {
            wrapper.cancel();
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final short slot = wrapper.read(Types.SHORT); // slot
            final Item item = wrapper.read(VersionedTypes.V26_3.lengthPrefixedItem); // item

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
                        // Tell the tracker which item the request creates, so the accepted response puts it into the destination slot
                        final BedrockItem created = inventoryTracker.getCreativeItems().get(creativeIndex).item().copy();
                        created.setAmount(amount);
                        inventoryTracker.setCreatedOutputPreview(created);
                        sendItemStackRequest(wrapper.user(), inventoryTracker, List.of(
                                ItemStackRequestAction.craftCreative(creativeNetId, 1),
                                // Vanilla clients acknowledge the craft with an empty deprecated craft results action
                                ItemStackRequestAction.craftResults(0),
                                ItemStackRequestAction.take(amount, createdOutputSlot(inventoryTracker), destination)
                        ));
                        return;
                    }
                }
            }

            PacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
        });
        protocol.registerServerbound(ServerboundPackets26_3.CUSTOM_CLICK_ACTION, ServerboundBedrockPackets.MODAL_FORM_RESPONSE, wrapper -> {
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
        protocol.registerServerbound(ServerboundPackets26_3.CONTAINER_CLOSE, ServerboundBedrockPackets.CONTAINER_CLOSE, new PacketHandlers() {
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
        protocol.registerServerbound(ServerboundPackets26_3.SET_CARRIED_ITEM, ServerboundBedrockPackets.MOB_EQUIPMENT, wrapper -> {
            final short slot = wrapper.read(Types.SHORT); // slot
            wrapper.user().get(InventoryTracker.class).getInventoryContainer().setSelectedHotbarSlot((byte) slot, wrapper); // slot
        });
        protocol.registerServerbound(ServerboundPackets26_3.PICK_ITEM_FROM_BLOCK, ServerboundBedrockPackets.BLOCK_PICK_REQUEST, wrapper -> {
            wrapper.passthroughAndMap(Types.BLOCK_POSITION1_14, BedrockTypes.BLOCK_POSITION); // position
            wrapper.passthrough(Types.BOOLEAN); // include data
            wrapper.write(Types.UNSIGNED_BYTE, (short) 9); // number of empty hotbar slots (vanilla client always sends 9)
        });
        protocol.registerServerbound(ServerboundPackets26_3.PICK_ITEM_FROM_ENTITY, ServerboundBedrockPackets.ENTITY_PICK_REQUEST, wrapper -> {
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

    private static String[] blockTags(final String... tags) {
        final List<String> known = new ArrayList<>();
        for (String tag : tags) {
            if (BedrockProtocol.MAPPINGS.getBedrockCustomBlockTags().containsValue(tag)) {
                known.add(tag);
            }
        }
        return known.toArray(new String[0]);
    }

}
