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
package net.raphimc.viabedrock.protocol.types.inventory;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackRequestActionType;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequest;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire layout per CloudburstMC BedrockCodecHelper_v2168 (ViaBedrock targets protocol 2169).
 */
public class ItemStackRequestPacketType extends Type<ItemStackRequest> {

    public ItemStackRequestPacketType() {
        super("ItemStackRequest", ItemStackRequest.class);
    }

    @Override
    public ItemStackRequest read(final ByteBuf buffer) {
        final int requestsCount = BedrockTypes.UNSIGNED_VAR_INT.read(buffer); // requests count
        if (requestsCount != 1) {
            throw new IllegalStateException("Expected exactly one item stack request, got " + requestsCount);
        }
        final int requestId = BedrockTypes.VAR_INT.read(buffer); // client request id (signed varint)
        final int actionsCount = BedrockTypes.UNSIGNED_VAR_INT.read(buffer); // actions count
        final List<ItemStackRequestAction> actions = new ArrayList<>(actionsCount);
        for (int i = 0; i < actionsCount; i++) {
            actions.add(readAction(buffer));
        }
        final int stringsToFilterCount = BedrockTypes.UNSIGNED_VAR_INT.read(buffer); // strings to filter count
        final List<String> stringsToFilter = new ArrayList<>(stringsToFilterCount);
        for (int i = 0; i < stringsToFilterCount; i++) {
            stringsToFilter.add(BedrockTypes.STRING.read(buffer)); // string to filter
        }
        final int stringsToFilterOrigin = buffer.readIntLE(); // strings to filter origin (little endian int, -1 = none)
        return new ItemStackRequest(requestId, actions, stringsToFilter, stringsToFilterOrigin);
    }

    @Override
    public void write(final ByteBuf buffer, final ItemStackRequest request) {
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, 1); // requests count
        BedrockTypes.VAR_INT.write(buffer, request.requestId()); // client request id (signed varint)
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, request.actions().size()); // actions count
        for (ItemStackRequestAction action : request.actions()) {
            writeAction(buffer, action);
        }
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, request.stringsToFilter() != null ? request.stringsToFilter().size() : 0); // strings to filter count
        if (request.stringsToFilter() != null) {
            for (String stringToFilter : request.stringsToFilter()) {
                BedrockTypes.STRING.write(buffer, stringToFilter); // string to filter
            }
        }
        buffer.writeIntLE(request.stringsToFilterOrigin()); // strings to filter origin (little endian int)
    }

    private ItemStackRequestAction readAction(final ByteBuf buffer) {
        final int actionType = variantToActionType(BedrockTypes.UNSIGNED_VAR_INT.read(buffer)); // action variant index
        final ItemStackRequestActionType type = ItemStackRequestActionType.getByValue(actionType);
        if (type == null) {
            throw new IllegalArgumentException("Unknown item stack request action type: " + actionType);
        }
        Types.BYTE.read(buffer); // action type (repeated as a byte constant inside the action data)

        return switch (type) {
            case Take -> new ItemStackRequestAction.Take(Types.BYTE.read(buffer), readSlot(buffer), readSlot(buffer)); // count, source, destination
            case Place -> new ItemStackRequestAction.Place(Types.BYTE.read(buffer), readSlot(buffer), readSlot(buffer)); // count, source, destination
            case Swap -> new ItemStackRequestAction.Swap(readSlot(buffer), readSlot(buffer)); // source, destination
            case Drop -> new ItemStackRequestAction.Drop(Types.BYTE.read(buffer), readSlot(buffer), Types.BOOLEAN.read(buffer)); // count, source, throw randomly
            case Destroy -> new ItemStackRequestAction.Destroy(Types.BYTE.read(buffer), readSlot(buffer)); // count, source
            case Consume -> new ItemStackRequestAction.Consume(Types.BYTE.read(buffer), readSlot(buffer)); // count, source
            case Create -> new ItemStackRequestAction.Create(Types.UNSIGNED_BYTE.read(buffer)); // slot
            case ScreenLabTableCombine -> new ItemStackRequestAction.LabTableCombine();
            case ScreenBeaconPayment -> new ItemStackRequestAction.BeaconPayment(BedrockTypes.VAR_INT.read(buffer), BedrockTypes.VAR_INT.read(buffer)); // primary, secondary effect
            case ScreenHUDMineBlock -> new ItemStackRequestAction.MineBlock(BedrockTypes.VAR_INT.read(buffer), BedrockTypes.VAR_INT.read(buffer), buffer.readIntLE()); // hotbar slot, predicted durability, stack net id
            case CraftRecipe -> new ItemStackRequestAction.CraftRecipe(BedrockTypes.UNSIGNED_VAR_INT.read(buffer), Types.BYTE.read(buffer)); // recipe net id, crafts
            case CraftCreative -> new ItemStackRequestAction.CraftCreative(BedrockTypes.UNSIGNED_VAR_INT.read(buffer), Types.BYTE.read(buffer)); // creative item net id, crafts
            case CraftRecipeOptional -> new ItemStackRequestAction.CraftRecipeOptional(BedrockTypes.UNSIGNED_VAR_INT.read(buffer), buffer.readIntLE()); // recipe net id, filtered string index
            case CraftRepairAndDisenchant -> new ItemStackRequestAction.CraftRepairAndDisenchant(buffer.readIntLE(), Types.BYTE.read(buffer), BedrockTypes.VAR_INT.read(buffer)); // recipe net id, crafts, repair cost
            case CraftLoom -> new ItemStackRequestAction.CraftLoom(BedrockTypes.STRING.read(buffer), Types.UNSIGNED_BYTE.read(buffer)); // pattern id, times crafted
            case CraftNonImplemented -> new ItemStackRequestAction.CraftNonImplemented();
            case CraftResults -> {
                // The result item instances come first; reading them requires the item definition registry
                if (BedrockTypes.UNSIGNED_VAR_INT.read(buffer) > 0) {
                    throw new UnsupportedOperationException("Reading craft result item instances is not supported");
                }
                yield new ItemStackRequestAction.CraftResults(Types.BYTE.read(buffer)); // times crafted
            }
            // Ingredient descriptors follow the recipe; requests built by this project never contain it
            case CraftRecipeAuto -> throw new UnsupportedOperationException("Reading CraftRecipeAuto actions is not supported");
        };
    }

    private void writeAction(final ByteBuf buffer, final ItemStackRequestAction action) {
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, actionTypeToVariant(action.type().getValue())); // action variant index
        Types.BYTE.write(buffer, (byte) action.type().getValue()); // action type (the enum value, repeated as a byte constant inside the action data)

        if (action instanceof ItemStackRequestAction.Transfer transfer) {
            Types.BYTE.write(buffer, (byte) transfer.amount()); // count
            writeSlot(buffer, transfer.source());
            writeSlot(buffer, transfer.destination());
        } else if (action instanceof ItemStackRequestAction.Swap swap) {
            writeSlot(buffer, swap.source());
            writeSlot(buffer, swap.destination());
        } else if (action instanceof ItemStackRequestAction.Drop drop) {
            Types.BYTE.write(buffer, (byte) drop.amount()); // count
            writeSlot(buffer, drop.source());
            Types.BOOLEAN.write(buffer, drop.throwRandomly()); // throw randomly
        } else if (action instanceof ItemStackRequestAction.Removal removal) { // Destroy, Consume
            Types.BYTE.write(buffer, (byte) removal.amount()); // count
            writeSlot(buffer, removal.source());
        } else if (action instanceof ItemStackRequestAction.Create create) {
            Types.UNSIGNED_BYTE.write(buffer, (short) create.slot()); // slot
        } else if (action instanceof ItemStackRequestAction.BeaconPayment payment) {
            BedrockTypes.VAR_INT.write(buffer, payment.primaryEffect()); // primary effect
            BedrockTypes.VAR_INT.write(buffer, payment.secondaryEffect()); // secondary effect
        } else if (action instanceof ItemStackRequestAction.MineBlock mineBlock) {
            BedrockTypes.VAR_INT.write(buffer, mineBlock.hotbarSlot()); // hotbar slot
            BedrockTypes.VAR_INT.write(buffer, mineBlock.predictedDurability()); // predicted durability
            buffer.writeIntLE(mineBlock.stackNetworkId()); // stack network id
        } else if (action instanceof ItemStackRequestAction.CraftRecipe craft) {
            BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craft.recipeNetId()); // recipe net id
            Types.BYTE.write(buffer, (byte) craft.timesCrafted()); // number of requested crafts
        } else if (action instanceof ItemStackRequestAction.CraftCreative craft) {
            BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craft.creativeItemNetId()); // creative item network id
            Types.BYTE.write(buffer, (byte) craft.timesCrafted()); // number of requested crafts
        } else if (action instanceof ItemStackRequestAction.CraftRecipeOptional craft) {
            BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craft.recipeNetId()); // recipe net id
            buffer.writeIntLE(craft.filteredStringIndex()); // filtered string index
        } else if (action instanceof ItemStackRequestAction.CraftRepairAndDisenchant craft) {
            buffer.writeIntLE(craft.recipeNetId()); // recipe net id
            Types.BYTE.write(buffer, (byte) craft.timesCrafted()); // number of requested crafts
            BedrockTypes.VAR_INT.write(buffer, craft.repairCost()); // repair cost
        } else if (action instanceof ItemStackRequestAction.CraftLoom craft) {
            BedrockTypes.STRING.write(buffer, craft.patternId()); // pattern id
            Types.UNSIGNED_BYTE.write(buffer, (short) craft.timesCrafted()); // times crafted
        } else if (action instanceof ItemStackRequestAction.CraftResults results) {
            BedrockTypes.UNSIGNED_VAR_INT.write(buffer, 0); // result items: an empty list is accepted
            Types.BYTE.write(buffer, (byte) results.timesCrafted()); // times crafted
        }
        // LabTableCombine and CraftNonImplemented carry no payload
    }

    /**
     * The action is a variant: its selector is the position in the variant list, which skips the two
     * removed container actions (values 7 and 8). The byte inside the action data is the enum value.
     * Both agree for the move actions (0-6) but not for beacon, mining and every craft action.
     */
    private static int actionTypeToVariant(final int actionType) {
        return actionType <= 6 ? actionType : actionType - 2;
    }

    private static int variantToActionType(final int variant) {
        return variant <= 6 ? variant : variant + 2;
    }

    private ItemStackRequestSlot readSlot(final ByteBuf buffer) {
        final FullContainerName containerName = BedrockTypes.FULL_CONTAINER_NAME.read(buffer); // full container name
        final byte slot = Types.BYTE.read(buffer); // slot
        final int netId = buffer.readIntLE(); // stack network id (little endian int)
        return new ItemStackRequestSlot(containerName, slot, netId);
    }

    private void writeSlot(final ByteBuf buffer, final ItemStackRequestSlot slot) {
        BedrockTypes.FULL_CONTAINER_NAME.write(buffer, slot.containerName()); // full container name
        Types.BYTE.write(buffer, slot.slot()); // slot
        buffer.writeIntLE(slot.netId()); // stack network id (little endian int)
    }

}
