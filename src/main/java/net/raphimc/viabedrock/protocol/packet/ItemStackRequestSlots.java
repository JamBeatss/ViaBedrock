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
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.api.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.storage.*;

import static net.raphimc.viabedrock.protocol.packet.ContainerClicks.*;
import static net.raphimc.viabedrock.protocol.packet.CraftingTranslator.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackResponses.*;

/**
 * Addresses Java container slots as Bedrock item stack request slots (container names, UI slot offsets, net ids).
 */
final class ItemStackRequestSlots {

    private ItemStackRequestSlots() {
    }

    // Slot offsets inside the Bedrock player UI container, as the vanilla client addresses them
    static final int UI_CRAFTING_2X2_FIRST = 28; // 28-31
    static final int UI_CRAFTING_3X3_FIRST = 32; // 32-40
    static final int UI_CREATED_OUTPUT = 50;

    // The vanilla client addresses the offhand as slot 1 (a known client quirk since 1.19.70)
    static final int OFFHAND_REQUEST_SLOT = 1;

    /**
     * Resolves a container from a response FullContainerName to the tracked container.
     */
    static Container resolveResponseContainer(final UserConnection user, final InventoryTracker inventoryTracker, final FullContainerName containerName) {
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
    static ItemStackRequestSlot inventoryRequestSlot(final InventoryTracker inventoryTracker, final int javaSlot) {
        if (javaSlot >= 9 && javaSlot <= 35) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.InventoryContainer, null), (byte) javaSlot, netIdOf(inventoryTracker.getInventoryContainer().getItem(javaSlot)));
        } else if (javaSlot >= 36 && javaSlot <= 44) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) (javaSlot - 36), netIdOf(inventoryTracker.getInventoryContainer().getItem(javaSlot - 36)));
        } else if (javaSlot >= 5 && javaSlot <= 8) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.ArmorContainer, null), (byte) (javaSlot - 5), netIdOf(inventoryTracker.getArmorContainer().getItem(javaSlot - 5)));
        } else if (javaSlot == 45) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.OffhandContainer, null), (byte) OFFHAND_REQUEST_SLOT, netIdOf(inventoryTracker.getOffhandContainer().getItem(0)));
        } else if (javaSlot >= 1 && javaSlot <= 4) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) (UI_CRAFTING_2X2_FIRST + javaSlot - 1), netIdOf(inventoryTracker.getHudContainer().getItem(UI_CRAFTING_2X2_FIRST + javaSlot - 1)));
        }
        return null; // Crafting result slot and unknown slots
    }

    static ItemStackRequestSlot requestSlotInfo(final InventoryTracker inventoryTracker, final Container container, final int javaSlot) {
        if (container.type() == ContainerType.INVENTORY || container == inventoryTracker.getInventoryContainer()) {
            return inventoryRequestSlot(inventoryTracker, javaSlot);
        }
        // Open containers are anchored to block entities: Bedrock networked as level entity containers
        final int bedrockSlot = container.bedrockSlot(javaSlot);
        if (container instanceof CraftingTableContainer) { // The 3x3 grid is addressed through the crafting input UI slots
            if (bedrockSlot < 0 || bedrockSlot >= 9) return null;
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) (UI_CRAFTING_3X3_FIRST + bedrockSlot), netIdOf(container.getItem(bedrockSlot)));
        }
        if (bedrockSlot < 0 || bedrockSlot >= container.size()) {
            return null;
        }
        final ContainerEnumName containerName = bedrockContainerName(container.type(), bedrockSlot);
        return new ItemStackRequestSlot(new FullContainerName(containerName, null), (byte) bedrockSlot, netIdOf(container.getItem(bedrockSlot)));
    }

    static ItemStackRequestSlot cursorSlot(final InventoryTracker inventoryTracker) {
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CursorContainer, null), (byte) 0, netIdOf(inventoryTracker.getHudContainer().getItem(0)));
    }

    static ItemStackRequestSlot hotbarRequestSlot(final int hotbarSlot, final BedrockItem item) {
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) hotbarSlot, netIdOf(item));
    }

    static int netIdOf(final BedrockItem item) {
        return item == null || item.isEmpty() || item.netId() == null ? 0 : item.netId();
    }

    record TrackedSlot(Container container, int slot) {
    }

    static TrackedSlot resolveRequestSlot(final InventoryTracker inventoryTracker, final ItemStackRequestSlot slot) {
        if (slot == null || slot.containerName() == null) {
            return null;
        }
        final int index = slot.slot() & 0xFF;
        final Container container = switch (slot.containerName().name()) {
            case InventoryContainer, HotbarContainer, CombinedHotbarAndInventoryContainer -> inventoryTracker.getInventoryContainer();
            case CursorContainer -> inventoryTracker.getHudContainer();
            case ArmorContainer -> inventoryTracker.getArmorContainer();
            case OffhandContainer -> inventoryTracker.getOffhandContainer();
            case CraftingInputContainer -> index >= UI_CRAFTING_3X3_FIRST && inventoryTracker.getCurrentContainer() instanceof CraftingTableContainer table ? table : inventoryTracker.getHudContainer();
            default -> inventoryTracker.getCurrentContainer();
        };
        if (container == null) {
            return null;
        }
        final int resolvedIndex = slot.containerName().name() == ContainerEnumName.OffhandContainer ? 0 : container instanceof CraftingTableContainer ? index - UI_CRAFTING_3X3_FIRST : index;
        if (resolvedIndex < 0 || resolvedIndex >= container.size()) {
            return null;
        }
        return new TrackedSlot(container, resolvedIndex);
    }

    static ItemStackRequestSlot playerInventorySlot(final InventoryTracker inventoryTracker, final int bedrockIndex) {
        final BedrockItem item = inventoryTracker.getInventoryContainer().getItem(bedrockIndex);
        if (bedrockIndex < 9) {
            return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.HotbarContainer, null), (byte) bedrockIndex, netIdOf(item));
        }
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.InventoryContainer, null), (byte) bedrockIndex, netIdOf(item));
    }

    static boolean isFurnaceType(final ContainerType type) {
        return type == ContainerType.FURNACE || type == ContainerType.BLAST_FURNACE || type == ContainerType.SMOKER;
    }

    static ContainerEnumName bedrockContainerName(final ContainerType type, final int bedrockSlot) {
        if (isFurnaceType(type)) {
            return switch (bedrockSlot) {
                case 0 -> type == ContainerType.BLAST_FURNACE ? ContainerEnumName.BlastFurnaceIngredientContainer : type == ContainerType.SMOKER ? ContainerEnumName.SmokerIngredientContainer : ContainerEnumName.FurnaceIngredientContainer;
                case 1 -> ContainerEnumName.FurnaceFuelContainer;
                default -> ContainerEnumName.FurnaceResultContainer;
            };
        }
        return type == ContainerType.CRAFTER ? ContainerEnumName.CrafterLevelEntityContainer : ContainerEnumName.LevelEntityContainer;
    }

    static ItemStackRequestSlot createdOutputSlot(final InventoryTracker inventoryTracker) {
        return new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) UI_CREATED_OUTPUT, inventoryTracker.peekNextItemStackRequestId());
    }

}
