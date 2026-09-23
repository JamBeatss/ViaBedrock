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
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.UiContainer;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponse;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static net.raphimc.viabedrock.protocol.packet.ContainerClicks.*;
import static net.raphimc.viabedrock.protocol.packet.CraftingTranslator.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackRequestSlots.*;
import static net.raphimc.viabedrock.protocol.packet.SpecialScreenPackets.*;

/**
 * Applies the server's item stack responses to the tracked containers and pushes the result to the Java client.
 */
final class ItemStackResponses {

    private ItemStackResponses() {
    }

    static void handleItemStackResponse(final PacketWrapper wrapper) {
        wrapper.cancel();
        final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);

        final ItemStackResponse response = wrapper.read(BedrockTypes.ITEM_STACK_RESPONSE);
        if (response == null) {
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Item stack response: empty");
            return;
        }
        ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Item stack response: result=" + response.result() + " requestId=" + response.requestId() + " containers=" + response.containers());

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
                        final int slotIndex = container == inventoryTracker.getOffhandContainer() ? 0 : container instanceof CraftingTableContainer ? (responseSlot.slot() & 0xFF) - UI_CRAFTING_3X3_FIRST : container instanceof UiContainer uiContainer ? uiContainer.slotOfUiSlot(responseSlot.slot() & 0xFF) : responseSlot.slot() & 0xFF; // The second slot field is the authoritative slot index (offhand is addressed as slot 1, the crafting grid as 32-40)
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
                        if (responseSlot.customName() != null && !responseSlot.customName().isEmpty()) { // Anvil renames report the new name here
                            setCustomName(updated, responseSlot.customName());
                        }
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
            final PacketWrapper acceptedCursorPacket = PacketWrapper.create(ClientboundPackets26_3.SET_CURSOR_ITEM, wrapper.user());
            acceptedCursorPacket.write(VersionedTypes.V26_3.item, inventoryTracker.getHudContainer().getJavaItem(0)); // cursor item
            acceptedCursorPacket.send(BedrockProtocol.class);
            updateCraftingResult(wrapper.user(), inventoryTracker);
            updateUiScreenResult(wrapper.user(), inventoryTracker);
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
        final PacketWrapper cursorPacket = PacketWrapper.create(ClientboundPackets26_3.SET_CURSOR_ITEM, wrapper.user());
        cursorPacket.write(VersionedTypes.V26_3.item, inventoryTracker.getHudContainer().getJavaItem(0)); // cursor item
        cursorPacket.send(BedrockProtocol.class);
        inventoryTracker.queuedClicks().clear(); // Queued clicks were built on the rejected state
        inventoryTracker.takeCreatedOutputPreview();
        updateCraftingResult(wrapper.user(), inventoryTracker);
        updateUiScreenResult(wrapper.user(), inventoryTracker);
    }

    static List<BedrockItem> snapshotSources(final InventoryTracker inventoryTracker, final List<ItemStackRequestAction> actions) {
        final List<BedrockItem> snapshots = new ArrayList<>(actions.size());
        for (ItemStackRequestAction action : actions) {
            final ItemStackRequestSlot source = action instanceof ItemStackRequestAction.SourceAction sourceAction ? sourceAction.source() : null;
            if (source != null && source.containerName() != null && source.containerName().name() == ContainerEnumName.CreatedOutputContainer) {
                final BedrockItem createdOutput = inventoryTracker.takeCreatedOutputPreview();
                snapshots.add(createdOutput != null ? createdOutput.copy() : inventoryTracker.matchedRecipe() != null ? inventoryTracker.matchedRecipe().result().copy() : null);
                continue;
            }
            final TrackedSlot from = resolveRequestSlot(inventoryTracker, source);
            final BedrockItem item = from != null ? from.container().getItem(from.slot()) : null;
            snapshots.add(item == null || item.isEmpty() ? null : item.copy());
        }
        return snapshots;
    }

    static void moveTracked(final TrackedSlot from, final TrackedSlot to, final int amount, final BedrockItem sourceSnapshot) {
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

    static void applyAcceptedActions(final InventoryTracker inventoryTracker, final List<ItemStackRequestAction> actions, final List<BedrockItem> sourceSnapshots) {
        for (int i = 0; i < actions.size(); i++) {
            final ItemStackRequestAction action = actions.get(i);
            final BedrockItem snapshot = sourceSnapshots != null && i < sourceSnapshots.size() ? sourceSnapshots.get(i) : null;
            if (action instanceof ItemStackRequestAction.Transfer transfer) {
                moveTracked(resolveRequestSlot(inventoryTracker, transfer.source()), resolveRequestSlot(inventoryTracker, transfer.destination()), transfer.amount(), snapshot);
            } else if (action instanceof ItemStackRequestAction.Swap swap) {
                final TrackedSlot a = resolveRequestSlot(inventoryTracker, swap.source());
                final TrackedSlot b = resolveRequestSlot(inventoryTracker, swap.destination());
                if (a != null && b != null) {
                    final BedrockItem itemA = a.container().getItem(a.slot());
                    final BedrockItem itemB = b.container().getItem(b.slot());
                    a.container().setItem(a.slot(), itemB == null ? BedrockItem.empty() : itemB.copy());
                    b.container().setItem(b.slot(), itemA == null ? BedrockItem.empty() : itemA.copy());
                }
            } else if (action instanceof ItemStackRequestAction.Removal removal) { // Drop, Destroy, Consume
                final TrackedSlot from = resolveRequestSlot(inventoryTracker, removal.source());
                if (from != null) {
                    final BedrockItem source = from.container().getItem(from.slot());
                    if (source != null && !source.isEmpty()) {
                        if (source.amount() - removal.amount() <= 0) {
                            from.container().setItem(from.slot(), BedrockItem.empty());
                        } else {
                            final BedrockItem remaining = source.copy();
                            remaining.setAmount(source.amount() - removal.amount());
                            from.container().setItem(from.slot(), remaining);
                        }
                    }
                }
            }
        }
    }

    static void setCustomName(final BedrockItem item, final String name) {
        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }
        CompoundTag display = item.tag().getCompoundTag("display");
        if (display == null) {
            display = new CompoundTag();
            item.tag().put("display", display);
        }
        display.putString("Name", name);
    }

}
