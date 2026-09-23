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

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.UiContainer;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ComplexInventoryTransaction_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.protocol.model.inventory.InventorySource;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryTransactionData;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequest;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.rewriter.InventoryTransactionRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static net.raphimc.viabedrock.protocol.packet.CraftingTranslator.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackRequestSlots.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackResponses.*;
import static net.raphimc.viabedrock.protocol.packet.SpecialScreenPackets.*;

/**
 * Translates Java container clicks (pickup, shift-click, drag, throw) into item stack requests or legacy inventory transactions.
 */
final class ContainerClicks {

    private ContainerClicks() {
    }

    // Fallback max stack size for merge predictions. Items with smaller stacks (e.g. ender pearls)
    // get rejected by the server and fall back to a resync, which keeps the inventory consistent
    static final int MAX_STACK_SIZE = 64;

    // Java's SWAP click uses button 40 for the offhand key
    static final int JAVA_OFFHAND_SWAP_BUTTON = 40;

    /**
     * Bedrock inventory indices in the order Java fills a free slot: main inventory first, then the hotbar.
     */
    static final int[] INVENTORY_FILL_ORDER = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 0, 1, 2, 3, 4, 5, 6, 7, 8};

    /**
     * Translates a Java container click into item stack request actions (server-auth inventory).
     * Returns null when the click can't be mapped and the containers need a resync instead.
     */
    static List<ItemStackRequestAction> buildItemStackRequestActions(final InventoryTracker inventoryTracker, final Container viewContainer, final int rawJavaSlot, final byte button, final ContainerInput action) {
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
        if (viewContainer instanceof UiContainer screen && container == viewContainer && javaSlot == screen.resultSlot()) {
            if (action != ContainerInput.PICKUP && action != ContainerInput.QUICK_MOVE) {
                return new ArrayList<>();
            }
            return buildScreenResultActions(inventoryTracker, screen, action);
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
                        final int movable = Math.min(cursorItem.amount(), Math.max(0, maxStackOf(inventoryTracker, clicked) - clicked.amount()));
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
                        final int movable = Math.min(1, Math.max(0, maxStackOf(inventoryTracker, clicked) - clicked.amount()));
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
                } else if (button == JAVA_OFFHAND_SWAP_BUTTON) {
                    return List.of(ItemStackRequestAction.swap(source, new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.OffhandContainer, null), (byte) OFFHAND_REQUEST_SLOT, netIdOf(inventoryTracker.getOffhandContainer().getItem(0)))));
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

    static void resyncClick(final UserConnection user, final InventoryTracker inventoryTracker, final Container container) {
        if (container.type() != ContainerType.INVENTORY) {
            PacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer());
        }
        PacketFactory.sendJavaContainerSetContent(user, container);
    }

    /**
     * Client-authoritative path: translates a Java container click into a legacy inventory transaction.
     * Returns false when the click can't be mapped and the containers need a resync instead.
     */
    static boolean translateClickToInventoryTransaction(final UserConnection user, final InventoryTracker inventoryTracker, final Container container, final int javaSlot, final byte button, final ContainerInput action) {
        final int bedSlot = container.bedrockSlot(javaSlot & 0xFFFF);
        if (bedSlot < 0 || bedSlot >= container.size()) {
            return false;
        }
        final BedrockItem clicked = container.getItem(bedSlot);
        final BedrockItem cursorItem = inventoryTracker.getHudContainer().getItem(0);
        final InventorySource slotSource = new InventorySource(InventorySourceType.Container_Inventory, container.containerId(), InventorySourceFlags.No_Flag);
        final InventorySource cursorSource = new InventorySource(InventorySourceType.Container_Inventory, ContainerID.CONTAINER_ID_PLAYER_ONLY_UI.getValue(), InventorySourceFlags.No_Flag);

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
                        final int movable = Math.min(cursorItem.amount(), Math.max(0, maxStackOf(inventoryTracker, clicked) - clicked.amount()));
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
                        if (clicked.amount() >= maxStackOf(inventoryTracker, clicked)) {
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
                actions.add(new InventoryActionData(new InventorySource(InventorySourceType.World_Interaction, ContainerID.CONTAINER_ID_NONE.getValue(), InventorySourceFlags.No_Flag), 0, BedrockItem.empty(), dropped));
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

    static int armorSlotFor(final InventoryTracker inventoryTracker, final BedrockItem item) {
        final String identifier = inventoryTracker.user().get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (identifier == null) return -1;
        if (identifier.endsWith("_helmet") || identifier.equals("minecraft:carved_pumpkin") || identifier.endsWith("_skull") || identifier.endsWith("_head")) return 0;
        if (identifier.endsWith("_chestplate") || identifier.equals("minecraft:elytra")) return 1;
        if (identifier.endsWith("_leggings")) return 2;
        if (identifier.endsWith("_boots")) return 3;
        return -1;
    }

    static List<ItemStackRequestAction> buildQuickMoveActions(final InventoryTracker inventoryTracker, final Container viewContainer, final Container clickedContainer, final int javaSlot, final boolean containerView) {
        final Container inventory = inventoryTracker.getInventoryContainer();
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        if (clickedContainer == inventory && !containerView) {
            final Container armor = inventoryTracker.getArmorContainer();
            if (javaSlot >= 5 && javaSlot <= 8) { // Armor slot -> first free main inventory slot, then hotbar
                final int armorIndex = javaSlot - 5;
                final BedrockItem worn = armor.getItem(armorIndex);
                if (worn == null || worn.isEmpty()) return actions;
                final ItemStackRequestSlot armorSource = new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.ArmorContainer, null), (byte) armorIndex, netIdOf(worn));
                for (int i : INVENTORY_FILL_ORDER) {
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
                if (viewContainer.type() == ContainerType.ENCHANTMENT && i != ("minecraft:lapis_lazuli".equals(itemIdentifier(inventoryTracker, moving)) ? 1 : 0)) continue; // Lapis goes to its own slot
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
        if (clickedContainer == inventory && containerView && (viewContainer.type() == ContainerType.BEACON || (viewContainer.type() == ContainerType.ENCHANTMENT && !"minecraft:lapis_lazuli".equals(itemIdentifier(inventoryTracker, moving))))) {
            remaining = Math.min(remaining, 1); // Beacon payment and enchanting input hold a single item
        }
        final int maxStack = maxStackOf(inventoryTracker, moving);
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
            if (existing != null && !existing.isEmpty() && !existing.isDifferent(held) && existing.amount() + held.amount() <= maxStackOf(inventoryTracker, held)) target = i;
        }
        for (int i = 0; i < 36 && target == -1; i++) {
            final BedrockItem existing = inventory.getItem(i);
            if (existing == null || existing.isEmpty()) target = i;
        }
        final List<ItemStackRequestAction> actions = target == -1
                ? List.of(ItemStackRequestAction.drop(held.amount(), cursorSlot(inventoryTracker), false))
                : List.of(ItemStackRequestAction.place(held.amount(), cursorSlot(inventoryTracker), playerInventorySlot(inventoryTracker, target)));
        final ItemStackRequest request = new ItemStackRequest(inventoryTracker.nextItemStackRequestId(), actions, new ArrayList<>(), 0);
        ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Item stack request (return cursor on close): id=" + request.requestId() + " actions=" + actions);
        inventoryTracker.trackItemStackRequest(request.requestId(), actions, snapshotSources(inventoryTracker, actions));
        final PacketWrapper requestPacket = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, user);
        requestPacket.write(BedrockTypes.ITEM_STACK_REQUEST, request);
        requestPacket.sendToServer(BedrockProtocol.class);
    }

    /**
     * Sends an item stack request and tracks it, so the accepted response can be applied to the tracked containers.
     * Every request goes through here: clicks, special screens and creative mode.
     */
    static void sendItemStackRequest(final UserConnection user, final InventoryTracker inventoryTracker, final List<ItemStackRequestAction> actions) {
        if (actions == null || actions.isEmpty()) return;
        final List<String> filterStrings = inventoryTracker.takePendingFilterStrings();
        final ItemStackRequest request = new ItemStackRequest(inventoryTracker.nextItemStackRequestId(), actions, filterStrings, filterStrings.isEmpty() ? 0 : TextProcessingEventOrigin.AnvilText.getValue());
        ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Item stack request: id=" + request.requestId() + " actions=" + actions + (request.stringsToFilter().isEmpty() ? "" : " strings=" + request.stringsToFilter()));
        inventoryTracker.trackItemStackRequest(request.requestId(), actions, snapshotSources(inventoryTracker, actions));
        final PacketWrapper requestPacket = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, user);
        requestPacket.write(BedrockTypes.ITEM_STACK_REQUEST, request);
        requestPacket.sendToServer(BedrockProtocol.class);
    }

    static void drainQueuedClicks(final UserConnection user, final InventoryTracker inventoryTracker) {
        while (!inventoryTracker.queuedClicks().isEmpty() && !inventoryTracker.hasPendingItemStackRequests()) {
            final InventoryTracker.QueuedClick click = inventoryTracker.queuedClicks().poll();
            processContainerClick(user, click.containerId(), click.revision(), click.slot(), click.button(), click.action());
        }
    }

    static void processContainerClick(final UserConnection user, final int containerId, final int revision, final short slot, final byte button, final ContainerInput action) {
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
                ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Sent INTERACT OpenInventory to the server");
                PacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer());
            }
            return;
        }

        final List<ItemStackRequestAction> actions;
        final BedrockItem trackedCursor = inventoryTracker.getHudContainer().getItem(0);
        ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Container click: container=" + container.type() + " slot=" + slot + " button=" + button + " action=" + action + " serverAuthoritative=" + gameSession.isInventoryServerAuthoritative() + " trackedCursor=" + (trackedCursor == null || trackedCursor.isEmpty() ? "empty" : trackedCursor.identifier() + " x" + trackedCursor.amount() + " netId=" + trackedCursor.netId()));
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
            sendItemStackRequest(user, inventoryTracker, actions);
        } else if (gameSession.isInventoryServerAuthoritative() && actions == null) {
            resyncClick(user, inventoryTracker, container);
        }
    }

    static List<ItemStackRequestAction> buildDragActions(final InventoryTracker inventoryTracker, final Container viewContainer, final int rawJavaSlot, final byte button) {
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

    /**
     * Vanilla max stack size by item identifier (Bedrock doesn't send it for vanilla items). Everything not listed stacks to 64.
     */
    static int maxStackOf(final InventoryTracker inventoryTracker, final BedrockItem item) {
        if (item == null || item.isEmpty()) return MAX_STACK_SIZE;
        final String id = inventoryTracker.user().get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (id == null) return MAX_STACK_SIZE;
        final String name = id.startsWith("minecraft:") ? id.substring(10) : id;
        if (name.equals("ender_pearl") || name.equals("snowball") || name.equals("egg") || name.equals("blue_egg") || name.equals("brown_egg")
                || name.equals("bucket") || name.equals("honey_bottle") || name.equals("armor_stand") || name.endsWith("_sign") || name.equals("sign")
                || name.endsWith("_banner") || name.equals("banner") || name.equals("wind_charge")) {
            return 16;
        }
        if (name.endsWith("_sword") || name.endsWith("_pickaxe") || name.endsWith("_axe") || name.endsWith("_shovel") || name.endsWith("_hoe")
                || name.endsWith("_helmet") || name.endsWith("_chestplate") || name.endsWith("_leggings") || name.endsWith("_boots")
                || name.endsWith("_bucket") || name.endsWith("_stew") || name.endsWith("_soup") || name.contains("potion") || name.endsWith("_boat") || name.endsWith("_raft")
                || name.endsWith("minecart") || name.endsWith("_bed") || name.equals("bed") || name.endsWith("shulker_box") || name.startsWith("music_disc")
                || name.equals("bow") || name.equals("crossbow") || name.equals("trident") || name.equals("shield") || name.equals("elytra") || name.equals("mace")
                || name.equals("fishing_rod") || name.equals("carrot_on_a_stick") || name.equals("warped_fungus_on_a_stick") || name.equals("flint_and_steel")
                || name.equals("shears") || name.equals("saddle") || name.equals("totem_of_undying") || name.equals("enchanted_book") || name.equals("writable_book")
                || name.equals("written_book") || name.equals("cake") || name.equals("spyglass") || name.equals("brush") || name.equals("bundle") || name.endsWith("_bundle")
                || name.equals("goat_horn") || name.equals("filled_map") || name.equals("turtle_helmet") || name.equals("wolf_armor") || name.endsWith("horse_armor")) {
            return 1;
        }
        return MAX_STACK_SIZE;
    }

}
