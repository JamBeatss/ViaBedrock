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
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;

/**
 * Bedrock orders the brewing stand as ingredient, three bottles, fuel. Java orders it as three bottles, ingredient, fuel.
 */
public class BrewingStandContainer extends SimpleContainer {

    public BrewingStandContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position, final String... validBlockTags) {
        super(user, containerId, ContainerType.BREWING_STAND, title, position, 5, validBlockTags);
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 0 -> 3;
            case 1, 2, 3 -> slot - 1;
            default -> slot;
        };
    }

    @Override
    public int bedrockSlot(final int javaSlot) {
        return switch (javaSlot) {
            case 3 -> 0;
            case 0, 1, 2 -> javaSlot + 1;
            default -> javaSlot;
        };
    }

    @Override
    public Item[] getJavaItems() {
        final Item[] bedrockOrder = super.getJavaItems();
        final Item[] javaOrder = new Item[bedrockOrder.length];
        for (int javaSlot = 0; javaSlot < javaOrder.length; javaSlot++) {
            javaOrder[javaSlot] = bedrockOrder[this.bedrockSlot(javaSlot)];
        }
        return javaOrder;
    }

}
