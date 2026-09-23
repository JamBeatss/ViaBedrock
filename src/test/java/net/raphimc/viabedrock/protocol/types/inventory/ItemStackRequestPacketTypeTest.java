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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequest;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Expected bytes follow the CloudburstMC reference encoder for protocol 2168/2193
 * (BedrockCodecHelper_v2168.writeItemStackRequest / writeRequestActionData / writeStackRequestSlotInfo),
 * not this project's writer.
 */
class ItemStackRequestPacketTypeTest {

    private static final ItemStackRequestPacketType TYPE = new ItemStackRequestPacketType();

    private static String write(final ItemStackRequest request) {
        final ByteBuf buffer = Unpooled.buffer();
        TYPE.write(buffer, request);
        return ByteBufUtil.hexDump(buffer);
    }

    private static ItemStackRequest read(final String hex) {
        return TYPE.read(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(hex)));
    }

    private static ItemStackRequestSlot slot(final ContainerEnumName name, final int slot, final int netId) {
        return new ItemStackRequestSlot(new FullContainerName(name, null), (byte) slot, netId);
    }

    @Test
    void takeFromInventoryToCursor() {
        final ItemStackRequest request = new ItemStackRequest(-1, List.of(
                ItemStackRequestAction.take(3, slot(ContainerEnumName.InventoryContainer, 15, 2832), slot(ContainerEnumName.CursorContainer, 0, 0))
        ), new ArrayList<>(), 0);

        final String expected = "01" // one request
                + "01" // request id -1 (zigzag varint)
                + "01" // one action
                + "00" + "00" // variant index 0, action type byte 0 (Take)
                + "03" // count
                + "1d00" + "0f" + "100b0000" // source: InventoryContainer (29), no dynamic id, slot 15, net id 2832
                + "3b00" + "00" + "00000000" // destination: CursorContainer (59), slot 0, net id 0
                + "00" // no filter strings
                + "00000000"; // filter origin: this project sends 0, the reference encoder sends -1 for "none"; the server accepts both
        assertEquals(expected, write(request));
    }

    @Test
    void craftActionsUseTheVariantIndexAsSelector() {
        // The selector is the position in the action variant list, which skips the removed container actions 7 and 8.
        // The byte inside the action data is the enum value. They differ for every craft action.
        final ItemStackRequest request = new ItemStackRequest(-3, List.of(
                ItemStackRequestAction.craftRecipe(300, 1),
                ItemStackRequestAction.craftResults(1),
                ItemStackRequestAction.consume(1, slot(ContainerEnumName.CraftingInputContainer, 28, 5))
        ), new ArrayList<>(), 0);

        final String expected = "01" + "05" + "03" // one request, id -3, three actions
                + "0a" + "0c" + "ac02" + "01" // CraftRecipe: variant 10, type 12, recipe net id 300, one craft
                + "11" + "13" + "00" + "01" // CraftResults: variant 17, type 19, no result items, one craft
                + "05" + "05" + "01" + "0d00" + "1c" + "05000000" // Consume: variant 5, type 5, count 1, CraftingInputContainer (13), slot 28, net id 5
                + "00" + "00000000";
        assertEquals(expected, write(request));
    }

    @Test
    void anvilRenameCarriesTheFilterString() {
        final ItemStackRequest request = new ItemStackRequest(-5, List.of(ItemStackRequestAction.craftRecipeOptional(0, 0)), List.of("Sword"), 3);

        final String expected = "01" + "09" + "01"
                + "0d" + "0f" + "00" + "00000000" // CraftRecipeOptional: variant 13, type 15, recipe net id 0, filtered string index 0
                + "01" + "05" + "53776f7264" // one filter string: "Sword"
                + "03000000"; // origin AnvilText (3)
        assertEquals(expected, write(request));
    }

    @Test
    void everyActionSurvivesAWriteReadRoundTrip() {
        final ItemStackRequestSlot cursor = slot(ContainerEnumName.CursorContainer, 0, 7);
        final ItemStackRequestSlot hotbar = slot(ContainerEnumName.HotbarContainer, 4, 9);
        final List<ItemStackRequestAction> actions = List.of(
                ItemStackRequestAction.take(2, hotbar, cursor),
                ItemStackRequestAction.place(2, cursor, hotbar),
                ItemStackRequestAction.swap(cursor, hotbar),
                ItemStackRequestAction.drop(1, hotbar, false),
                ItemStackRequestAction.destroy(1, hotbar),
                ItemStackRequestAction.consume(1, hotbar),
                ItemStackRequestAction.create(3),
                ItemStackRequestAction.screenBeaconPayment(1, 10),
                ItemStackRequestAction.craftRecipe(12, 1),
                ItemStackRequestAction.craftCreative(40, 1),
                ItemStackRequestAction.craftRecipeOptional(0, 0),
                ItemStackRequestAction.craftResults(1)
        );
        final ItemStackRequest request = new ItemStackRequest(-7, actions, List.of("name"), 3);

        final ItemStackRequest decoded = read(write(request));
        assertEquals(request.requestId(), decoded.requestId());
        assertEquals(request.stringsToFilter(), decoded.stringsToFilter());
        assertEquals(request.stringsToFilterOrigin(), decoded.stringsToFilterOrigin());
        assertEquals(actions, decoded.actions()); // every field of every action
    }

}
