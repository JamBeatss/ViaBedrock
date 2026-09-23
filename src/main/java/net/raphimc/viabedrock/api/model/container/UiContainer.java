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

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ContainerType;

/**
 * A screen whose items live in the player's UI container on Bedrock (anvil, enchanting table, beacon, trading, ...).
 * Slot indices follow the Java menu layout; each slot maps to a Bedrock UI slot offset and container name.
 * A UI slot of -1 marks the Java result slot, which Bedrock only fills through the created output container.
 */
public class UiContainer extends SimpleContainer {

    private final int[] uiSlots;
    private final ContainerEnumName[] containerNames;

    public UiContainer(final UserConnection user, final byte containerId, final ContainerType type, final TextComponent title, final BlockPosition position, final int[] uiSlots, final ContainerEnumName[] containerNames, final String... validBlockTags) {
        super(user, containerId, type, title, position, uiSlots.length, validBlockTags);
        if (uiSlots.length != containerNames.length) {
            throw new IllegalArgumentException("UI slot and container name counts differ");
        }
        this.uiSlots = uiSlots;
        this.containerNames = containerNames;
    }

    public int uiSlot(final int slot) {
        return slot >= 0 && slot < this.uiSlots.length ? this.uiSlots[slot] : -1;
    }

    public ContainerEnumName containerName(final int slot) {
        return slot >= 0 && slot < this.containerNames.length ? this.containerNames[slot] : null;
    }

    /**
     * @return the slot index for a Bedrock UI slot offset, or -1 if this screen doesn't use it
     */
    public int slotOfUiSlot(final int uiSlot) {
        for (int i = 0; i < this.uiSlots.length; i++) {
            if (this.uiSlots[i] == uiSlot && uiSlot != -1) {
                return i;
            }
        }
        return -1;
    }

    public boolean handlesContainerName(final ContainerEnumName containerName) {
        for (ContainerEnumName name : this.containerNames) {
            if (name == containerName) {
                return true;
            }
        }
        return false;
    }

    public int resultSlot() {
        for (int i = 0; i < this.uiSlots.length; i++) {
            if (this.uiSlots[i] == -1) {
                return i;
            }
        }
        return -1;
    }

}
