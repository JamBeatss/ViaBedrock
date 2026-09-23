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
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

/**
 * 3x3 crafting grid. Java slot 0 is the (proxy computed) result, slots 1-9 the grid.
 */
public class CraftingTableContainer extends SimpleContainer {

    private BedrockItem result = BedrockItem.empty();

    public CraftingTableContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position, final String... validBlockTags) {
        super(user, containerId, ContainerType.WORKBENCH, title, position, 9, 1, validBlockTags);
    }

    public BedrockItem result() {
        return this.result;
    }

    public void setResult(final BedrockItem result) {
        this.result = result == null ? BedrockItem.empty() : result;
    }

    @Override
    public Item[] getJavaItems() {
        final Item[] grid = super.getJavaItems();
        final Item[] items = new Item[grid.length + 1];
        items[0] = this.user.get(ItemRewriter.class).javaItem(this.result);
        System.arraycopy(grid, 0, items, 1, grid.length);
        return items;
    }

}
