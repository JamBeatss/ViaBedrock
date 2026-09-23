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
package net.raphimc.viabedrock.api.model.container;

import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerSlotMappingTest {

    private static UiContainer anvil() {
        return new UiContainer(null, (byte) 3, ContainerType.ANVIL, null, null, new int[]{1, 2, UiContainer.RESULT_PREVIEW},
                new ContainerEnumName[]{ContainerEnumName.AnvilInputContainer, ContainerEnumName.AnvilMaterialContainer, ContainerEnumName.AnvilResultPreviewContainer});
    }

    @Test
    void uiContainerMapsJavaSlotsToBedrockUiSlots() {
        final UiContainer anvil = anvil();
        assertEquals(1, anvil.uiSlot(0));
        assertEquals(2, anvil.uiSlot(1));
        assertEquals(UiContainer.RESULT_PREVIEW, anvil.uiSlot(2));
        assertEquals(ContainerEnumName.AnvilMaterialContainer, anvil.containerName(1));
        assertEquals(2, anvil.resultSlot());
    }

    @Test
    void uiContainerMapsBedrockUiSlotsBack() {
        final UiContainer anvil = anvil();
        assertEquals(0, anvil.slotOfUiSlot(1));
        assertEquals(1, anvil.slotOfUiSlot(2));
        assertEquals(-1, anvil.slotOfUiSlot(14), "a UI slot this screen doesn't use");
        assertEquals(-1, anvil.slotOfUiSlot(UiContainer.RESULT_PREVIEW), "the result preview marker is never a real UI slot");
        assertTrue(anvil.handlesContainerName(ContainerEnumName.AnvilInputContainer));
        assertFalse(anvil.handlesContainerName(ContainerEnumName.EnchantingInputContainer));
    }

    @Test
    void uiContainerWithoutResultSlot() {
        final UiContainer beacon = new UiContainer(null, (byte) 4, ContainerType.BEACON, null, null, new int[]{27}, new ContainerEnumName[]{ContainerEnumName.BeaconPaymentContainer});
        assertEquals(-1, beacon.resultSlot());
        assertEquals(0, beacon.slotOfUiSlot(27));
    }

    @Test
    void uiContainerRejectsMismatchedDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> new UiContainer(null, (byte) 5, ContainerType.ANVIL, null, null, new int[]{1, 2}, new ContainerEnumName[]{ContainerEnumName.AnvilInputContainer}));
    }

    @Test
    void brewingStandReordersBedrockSlotsForJava() {
        // Bedrock: 0 ingredient, 1-3 bottles, 4 fuel. Java: 0-2 bottles, 3 ingredient, 4 fuel.
        final BrewingStandContainer brewingStand = new BrewingStandContainer(null, (byte) 6, null, null);
        assertEquals(3, brewingStand.javaSlot(0));
        assertEquals(0, brewingStand.javaSlot(1));
        assertEquals(2, brewingStand.javaSlot(3));
        assertEquals(4, brewingStand.javaSlot(4));
        for (int bedrockSlot = 0; bedrockSlot < 5; bedrockSlot++) {
            assertEquals(bedrockSlot, brewingStand.bedrockSlot(brewingStand.javaSlot(bedrockSlot)), "round trip of bedrock slot " + bedrockSlot);
        }
        assertEquals(5, brewingStand.bedrockSlot(5), "the player inventory region starts right after the stand");
    }

}
