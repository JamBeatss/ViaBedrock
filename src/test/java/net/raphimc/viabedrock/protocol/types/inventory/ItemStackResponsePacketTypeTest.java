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

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Input bytes are laid out as the CloudburstMC reference encoder writes them for protocol 2193
 * (ItemStackResponseSerializer_v2193.serialize and BedrockCodecHelper_v2193.writeItemEntry).
 */
class ItemStackResponsePacketTypeTest {

    private static final ItemStackResponsePacketType TYPE = new ItemStackResponsePacketType();

    private static ItemStackResponse read(final String hex) {
        return TYPE.read(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(hex)));
    }

    @Test
    void acceptedMoveReportsTheNewCursorStack() {
        final ItemStackResponse response = read("01" // one response
                + "00" // result OK
                + "01" // request id -1 (zigzag varint)
                + "01" // has containers
                + "01" // one container
                + "3b00" // CursorContainer (59), no dynamic id
                + "01" // one slot
                + "00" + "00" + "03" // requested slot 0, slot 0, amount 3
                + "01" + "a02c" // net id present: 2832 (zigzag varint)
                + "00" // custom name ""
                + "00" // no filtered custom name
                + "00"); // durability correction 0

        assertEquals(ItemStackResponse.RESULT_OK, response.result());
        assertEquals(-1, response.requestId());
        assertEquals(1, response.containers().size());
        final ItemStackResponse.Container container = response.containers().get(0);
        assertEquals(ContainerEnumName.CursorContainer, container.containerName().name());
        final ItemStackResponse.Slot slot = container.slots().get(0);
        assertEquals(0, slot.slot());
        assertEquals(3, slot.amount());
        assertEquals(2832, slot.serverNetId());
        assertEquals("", slot.customName());
        assertNull(slot.filteredCustomName());
    }

    @Test
    void anvilRenameReportsTheCustomName() {
        final ItemStackResponse response = read("01" + "00" + "03" + "01" + "01"
                + "3b00" + "01"
                + "00" + "00" + "01"
                + "01" + "0e" // net id 7
                + "05" + "53776f7264" // custom name "Sword"
                + "00" + "00");

        final ItemStackResponse.Slot slot = response.containers().get(0).slots().get(0);
        assertEquals(7, slot.serverNetId());
        assertEquals("Sword", slot.customName());
    }

    @Test
    void rejectedRequestHasNoContainers() {
        final ItemStackResponse response = read("01" + "31" + "05" + "00"); // result 49 (source validation failed), request id -3, no containers

        assertEquals(49, response.result());
        assertEquals(-3, response.requestId());
        assertNull(response.containers());
    }

}
