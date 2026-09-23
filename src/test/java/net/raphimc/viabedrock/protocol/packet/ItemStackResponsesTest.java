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

import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.storage.InventoryTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applying an accepted request to the tracked containers, the way a RESULT_OK response does.
 */
class ItemStackResponsesTest {

    private static final int STONE = 1;
    private static final int DIRT = 3;

    @Test
    void createdItemLandsInAnEmptySlot() {
        // Creative mode: the item comes out of the created output container, which the tracker only knows through the preview
        final InventoryTracker tracker = new InventoryTracker(null);
        tracker.setCreatedOutputPreview(new BedrockItem(STONE, (short) 0, (byte) 1));
        final List<ItemStackRequestAction> actions = List.of(
                ItemStackRequestAction.craftCreative(40, 1),
                ItemStackRequestAction.craftResults(0),
                ItemStackRequestAction.take(16, ItemStackRequestSlots.createdOutputSlot(tracker), ItemStackRequestSlots.playerInventorySlot(tracker, 9))
        );

        ItemStackResponses.applyAcceptedActions(tracker, actions, ItemStackResponses.snapshotSources(tracker, actions));

        final BedrockItem placed = tracker.getInventoryContainer().getItem(9);
        assertEquals(STONE, placed.identifier());
        assertEquals(16, placed.amount());
        assertNull(tracker.takeCreatedOutputPreview(), "the preview belongs to one request only");
    }

    @Test
    void consumedInputsAreRemoved() {
        final InventoryTracker tracker = new InventoryTracker(null);
        // A main inventory slot: changing the selected hotbar slot would send a held item update to the (absent) client
        tracker.getInventoryContainer().setItem(20, new BedrockItem(DIRT, (short) 0, (byte) 5));
        final List<ItemStackRequestAction> actions = List.of(ItemStackRequestAction.consume(2, ItemStackRequestSlots.playerInventorySlot(tracker, 20)));

        ItemStackResponses.applyAcceptedActions(tracker, actions, ItemStackResponses.snapshotSources(tracker, actions));

        assertEquals(3, tracker.getInventoryContainer().getItem(20).amount());
    }

    @Test
    void takeMovesAStackToTheCursor() {
        final InventoryTracker tracker = new InventoryTracker(null);
        tracker.getInventoryContainer().setItem(12, new BedrockItem(DIRT, (short) 0, (byte) 6));
        final List<ItemStackRequestAction> actions = List.of(
                ItemStackRequestAction.take(6, ItemStackRequestSlots.playerInventorySlot(tracker, 12), ItemStackRequestSlots.cursorSlot(tracker))
        );

        ItemStackResponses.applyAcceptedActions(tracker, actions, ItemStackResponses.snapshotSources(tracker, actions));

        assertTrue(tracker.getInventoryContainer().getItem(12).isEmpty());
        final BedrockItem cursor = tracker.getHudContainer().getItem(0);
        assertFalse(cursor.isEmpty());
        assertEquals(6, cursor.amount());
    }

}
