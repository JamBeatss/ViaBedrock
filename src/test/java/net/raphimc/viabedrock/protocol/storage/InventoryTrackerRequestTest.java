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
package net.raphimc.viabedrock.protocol.storage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTrackerRequestTest {

    @Test
    void requestIdsAreNegativeAndOdd() {
        final InventoryTracker tracker = new InventoryTracker(null);
        assertEquals(-1, tracker.peekNextItemStackRequestId());
        assertEquals(-1, tracker.nextItemStackRequestId());
        assertEquals(-3, tracker.nextItemStackRequestId());
        assertEquals(-5, tracker.peekNextItemStackRequestId());
    }

    @Test
    void pendingRequestBlocksClicksUntilAnswered() {
        final InventoryTracker tracker = new InventoryTracker(null);
        tracker.trackItemStackRequest(-1, List.of(), List.of());
        assertTrue(tracker.hasPendingItemStackRequests());
        assertNotNull(tracker.takePendingItemStackRequest(-1));
        assertFalse(tracker.hasPendingItemStackRequests());
        assertNull(tracker.takePendingItemStackRequest(-1), "a response is applied once");
    }

    @Test
    void filterStringsAndCreatedOutputAreConsumedOnce() {
        final InventoryTracker tracker = new InventoryTracker(null);
        tracker.setPendingFilterStrings(List.of("Sword"));
        assertEquals(List.of("Sword"), tracker.takePendingFilterStrings());
        assertEquals(List.of(), tracker.takePendingFilterStrings());
        assertNull(tracker.takeCreatedOutputPreview());
    }

}
