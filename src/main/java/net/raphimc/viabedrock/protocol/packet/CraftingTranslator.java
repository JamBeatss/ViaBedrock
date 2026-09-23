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
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.util.Key;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.CraftingTableContainer;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static net.raphimc.viabedrock.protocol.packet.ContainerClicks.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackRequestSlots.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackResponses.*;
import static net.raphimc.viabedrock.protocol.packet.SpecialScreenPackets.*;

/**
 * Reads Bedrock crafting recipes, matches the crafting grid and builds craft requests.
 */
final class CraftingTranslator {

    private CraftingTranslator() {
    }

    static void handleCraftingData(final PacketWrapper wrapper) {
        wrapper.cancel();
        final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
        final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
        final List<InventoryTracker.Recipe> recipes = new ArrayList<>();
        try {
            for (int list = 0; list < 2; list++) { // Shaped recipes first, then shapeless; the remaining lists are not needed
                final boolean shaped = list == 0;
                final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                for (int i = 0; i < count; i++) {
                    wrapper.read(BedrockTypes.STRING); // recipe id
                    int width = 0, height = 0;
                    if (shaped) {
                        width = wrapper.read(BedrockTypes.VAR_INT); // width
                        height = wrapper.read(BedrockTypes.VAR_INT); // height
                    }
                    final int ingredientCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                    final List<InventoryTracker.Ingredient> ingredients = new ArrayList<>(ingredientCount);
                    for (int j = 0; j < ingredientCount; j++) {
                        ingredients.add(readRecipeIngredient(wrapper));
                    }
                    final int resultCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                    BedrockItem result = BedrockItem.empty();
                    for (int j = 0; j < resultCount; j++) {
                        final BedrockItem output = wrapper.read(itemRewriter.itemInstanceType()); // result
                        if (j == 0) result = output;
                    }
                    wrapper.read(BedrockTypes.UUID); // uuid
                    final String tag = wrapper.read(BedrockTypes.STRING); // crafting tag
                    wrapper.read(BedrockTypes.VAR_INT); // priority
                    boolean assumeSymmetry = false;
                    if (shaped) {
                        assumeSymmetry = wrapper.read(Types.BOOLEAN); // assume symmetry (the mirrored arrangement also crafts)
                    }
                    if (wrapper.read(Types.BOOLEAN)) { // has unlocking requirement
                        wrapper.read(BedrockTypes.VAR_INT); // unlocking context
                        if (wrapper.read(Types.BOOLEAN)) {
                            final int requirementCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                            for (int j = 0; j < requirementCount; j++) {
                                readRecipeIngredient(wrapper);
                            }
                        }
                    }
                    final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // recipe net id
                    if (!result.isEmpty() && (tag.equals("crafting_table") || tag.equals("deprecated"))) {
                        recipes.add(new InventoryTracker.Recipe(netId, tag, shaped, width, height, ingredients, result, assumeSymmetry));
                    }
                }
            }
        } catch (Throwable e) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to read crafting recipes (" + recipes.size() + " read so far" + (recipes.isEmpty() ? "" : ", last: " + recipes.get(recipes.size() - 1)) + ")", e);
        }
        inventoryTracker.recipes().clear();
        inventoryTracker.recipes().addAll(recipes);
        ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Loaded " + recipes.size() + " crafting recipes");
        for (int i = 0; i < Math.min(3, recipes.size()); i++) { // Sample for verifying the ingredient reader against real data
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "Recipe sample: " + recipes.get(i));
        }
    }

    static InventoryTracker.Ingredient readRecipeIngredient(final PacketWrapper wrapper) {
        final int typeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // descriptor type
        InventoryTracker.Ingredient ingredient;
        if (typeId == 0) {
            wrapper.read(BedrockTypes.VAR_INT); // aux
            ingredient = new InventoryTracker.Ingredient(0, null, 0, 0);
        } else {
            final String kind = wrapper.read(BedrockTypes.STRING); // descriptor name
            switch (kind) {
                case "default" -> {
                    final String id = wrapper.read(BedrockTypes.STRING);
                    final int aux = wrapper.read(BedrockTypes.VAR_INT);
                    ingredient = new InventoryTracker.Ingredient(1, id, aux, 0);
                }
                case "item_tag" -> {
                    final String tag = wrapper.read(BedrockTypes.STRING);
                    wrapper.read(BedrockTypes.VAR_INT); // aux
                    ingredient = new InventoryTracker.Ingredient(2, tag, 0, 0);
                }
                case "molang" -> {
                    final String expression = wrapper.read(BedrockTypes.STRING);
                    wrapper.read(BedrockTypes.SHORT_LE); // molang version
                    ingredient = new InventoryTracker.Ingredient(3, expression, 0, 0);
                }
                default -> throw new IllegalStateException("Unknown recipe item descriptor: " + kind);
            }
        }
        final int count = wrapper.read(BedrockTypes.VAR_INT); // count
        return new InventoryTracker.Ingredient(ingredient.kind(), ingredient.value(), ingredient.aux(), count);
    }

    static boolean ingredientMatches(final UserConnection user, final InventoryTracker.Ingredient ingredient, final BedrockItem item) {
        final boolean empty = item == null || item.isEmpty();
        if (ingredient.kind() == 0) return empty;
        if (empty) return false;
        final String identifier = user.get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (identifier == null) return false;
        return switch (ingredient.kind()) {
            case 1 -> Key.namespaced(ingredient.value()).equals(identifier) && (ingredient.aux() == 32767 || ingredient.aux() == -1 || ingredient.aux() == item.data());
            case 2 -> {
                final Set<String> tags = BedrockProtocol.MAPPINGS.getBedrockItemTags().get(identifier);
                yield tags != null && tags.contains(Key.namespaced(ingredient.value())) || tags != null && tags.contains(ingredient.value());
            }
            default -> false;
        };
    }

    static BedrockItem[] craftingGrid(final InventoryTracker inventoryTracker, final Container viewContainer) {
        if (viewContainer instanceof CraftingTableContainer table) {
            return table.getItems();
        }
        final BedrockItem[] grid = new BedrockItem[4];
        for (int i = 0; i < 4; i++) grid[i] = inventoryTracker.getHudContainer().getItem(UI_CRAFTING_2X2_FIRST + i);
        return grid;
    }

    static InventoryTracker.Recipe matchRecipe(final UserConnection user, final InventoryTracker inventoryTracker, final BedrockItem[] grid) {
        final int size = grid.length == 9 ? 3 : 2;
        int minX = size, minY = size, maxX = -1, maxY = -1;
        final List<BedrockItem> nonEmpty = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null && !grid[i].isEmpty()) {
                final int x = i % size, y = i / size;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                nonEmpty.add(grid[i]);
            }
        }
        if (nonEmpty.isEmpty()) return null;
        final int boxWidth = maxX - minX + 1, boxHeight = maxY - minY + 1;
        for (InventoryTracker.Recipe recipe : inventoryTracker.recipes()) {
            if (recipe.shaped()) {
                if (recipe.width() != boxWidth || recipe.height() != boxHeight) continue;
                for (int mirror = 0; mirror < (recipe.assumeSymmetry() ? 2 : 1); mirror++) {
                    boolean ok = true;
                    for (int y = 0; y < boxHeight && ok; y++) {
                        for (int x = 0; x < boxWidth && ok; x++) {
                            final int rx = mirror == 0 ? x : boxWidth - 1 - x;
                            final InventoryTracker.Ingredient ingredient = recipe.ingredients().get(y * recipe.width() + rx);
                            ok = ingredientMatches(user, ingredient, grid[(minY + y) * size + minX + x]);
                        }
                    }
                    if (ok) return recipe;
                }
            } else {
                final List<InventoryTracker.Ingredient> needed = new ArrayList<>();
                for (InventoryTracker.Ingredient ingredient : recipe.ingredients()) {
                    if (ingredient.kind() == 0) continue;
                    for (int c = 0; c < Math.max(1, ingredient.count()); c++) needed.add(ingredient);
                }
                if (needed.size() != nonEmpty.size() || needed.size() > size * size) continue;
                if (assignShapeless(user, needed, nonEmpty, new boolean[nonEmpty.size()], 0)) return recipe;
            }
        }
        return null;
    }

    static boolean assignShapeless(final UserConnection user, final List<InventoryTracker.Ingredient> needed, final List<BedrockItem> items, final boolean[] used, final int index) {
        if (index == needed.size()) return true;
        for (int i = 0; i < items.size(); i++) {
            if (!used[i] && ingredientMatches(user, needed.get(index), items.get(i))) {
                used[i] = true;
                if (assignShapeless(user, needed, items, used, index + 1)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    /**
     * Recomputes the crafting result for the open crafting grid and shows it in Java slot 0.
     */
    static void updateCraftingResult(final UserConnection user, final InventoryTracker inventoryTracker) {
        final Container current = inventoryTracker.getCurrentContainer();
        if (!(current instanceof CraftingTableContainer) && (current == null || current.type() != ContainerType.INVENTORY)) {
            inventoryTracker.setMatchedRecipe(null);
            return;
        }
        final InventoryTracker.Recipe recipe = matchRecipe(user, inventoryTracker, craftingGrid(inventoryTracker, current));
        inventoryTracker.setMatchedRecipe(recipe);
        final BedrockItem result = recipe != null ? recipe.result() : BedrockItem.empty();
        if (current instanceof CraftingTableContainer table) {
            table.setResult(result);
        }
        final PacketWrapper setSlot = PacketWrapper.create(ClientboundPackets26_3.CONTAINER_SET_SLOT, user);
        setSlot.write(Types.VAR_INT, current instanceof CraftingTableContainer ? (int) current.javaContainerId() : (int) inventoryTracker.getInventoryContainer().javaContainerId()); // container id
        setSlot.write(Types.VAR_INT, 0); // revision
        setSlot.write(Types.SHORT, (short) 0); // slot
        setSlot.write(VersionedTypes.V26_3.item, user.get(ItemRewriter.class).javaItem(result)); // item
        setSlot.send(BedrockProtocol.class);
    }

    static List<ItemStackRequestAction> buildCraftActions(final InventoryTracker inventoryTracker, final Container viewContainer, final ContainerInput action) {
        final InventoryTracker.Recipe recipe = inventoryTracker.matchedRecipe();
        if (recipe == null) return new ArrayList<>();
        final BedrockItem result = recipe.result();
        final BedrockItem held = inventoryTracker.getHudContainer().getItem(0);
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        final int requestId = inventoryTracker.peekNextItemStackRequestId();
        final ItemStackRequestSlot output = new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) UI_CREATED_OUTPUT, requestId);
        ItemStackRequestSlot destination;
        if (action == ContainerInput.QUICK_MOVE) {
            destination = null;
            for (int i : INVENTORY_FILL_ORDER) {
                final BedrockItem existing = inventoryTracker.getInventoryContainer().getItem(i);
                if (existing == null || existing.isEmpty()) {
                    destination = playerInventorySlot(inventoryTracker, i);
                    break;
                }
            }
            if (destination == null) return new ArrayList<>();
        } else {
            if (held != null && !held.isEmpty() && (held.isDifferent(result) || held.amount() + result.amount() > maxStackOf(inventoryTracker, result))) {
                return new ArrayList<>(); // Can't pick the result up onto a different or full cursor stack
            }
            destination = cursorSlot(inventoryTracker);
        }
        actions.add(ItemStackRequestAction.craftRecipe(recipe.netId(), 1));
        actions.add(ItemStackRequestAction.craftResults(1));
        final BedrockItem[] grid = craftingGrid(inventoryTracker, viewContainer);
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == null || grid[i].isEmpty()) continue;
            final int uiSlot = grid.length == 9 ? UI_CRAFTING_3X3_FIRST + i : UI_CRAFTING_2X2_FIRST + i;
            actions.add(ItemStackRequestAction.consume(1, new ItemStackRequestSlot(new FullContainerName(ContainerEnumName.CraftingInputContainer, null), (byte) uiSlot, netIdOf(grid[i]))));
        }
        if (action == ContainerInput.QUICK_MOVE) {
            actions.add(ItemStackRequestAction.place(result.amount(), output, destination));
        } else {
            actions.add(ItemStackRequestAction.take(result.amount(), output, destination));
        }
        return actions;
    }

}
