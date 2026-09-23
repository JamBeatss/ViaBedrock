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
package net.raphimc.viabedrock.protocol.model.inventory;

import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackRequestActionType;

/**
 * One action inside an item stack request, one record per Bedrock action with exactly its fields.
 * Wire layouts follow the CloudburstMC BedrockCodecHelper_v2168 encoder (protocol 2168/2193).
 */
public sealed interface ItemStackRequestAction {

    ItemStackRequestActionType type();

    /**
     * An action that takes items out of a slot.
     */
    sealed interface SourceAction extends ItemStackRequestAction {

        ItemStackRequestSlot source();

    }

    /**
     * Moves an amount of items from one slot to another.
     */
    sealed interface Transfer extends SourceAction {

        int amount();

        ItemStackRequestSlot destination();

    }

    /**
     * Removes an amount of items from a slot.
     */
    sealed interface Removal extends SourceAction {

        int amount();

    }

    record Take(int amount, ItemStackRequestSlot source, ItemStackRequestSlot destination) implements Transfer {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Take;
        }
    }

    record Place(int amount, ItemStackRequestSlot source, ItemStackRequestSlot destination) implements Transfer {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Place;
        }
    }

    record Swap(ItemStackRequestSlot source, ItemStackRequestSlot destination) implements SourceAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Swap;
        }
    }

    record Drop(int amount, ItemStackRequestSlot source, boolean throwRandomly) implements Removal {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Drop;
        }
    }

    record Destroy(int amount, ItemStackRequestSlot source) implements Removal {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Destroy;
        }
    }

    record Consume(int amount, ItemStackRequestSlot source) implements Removal {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Consume;
        }
    }

    record Create(int slot) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.Create;
        }
    }

    record LabTableCombine() implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.ScreenLabTableCombine;
        }
    }

    record BeaconPayment(int primaryEffect, int secondaryEffect) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.ScreenBeaconPayment;
        }
    }

    record MineBlock(int hotbarSlot, int predictedDurability, int stackNetworkId) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.ScreenHUDMineBlock;
        }
    }

    record CraftRecipe(int recipeNetId, int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftRecipe;
        }
    }

    record CraftCreative(int creativeItemNetId, int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftCreative;
        }
    }

    /**
     * @param filteredStringIndex index into the request's filter strings (the anvil rename text)
     */
    record CraftRecipeOptional(int recipeNetId, int filteredStringIndex) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftRecipeOptional;
        }
    }

    record CraftRepairAndDisenchant(int recipeNetId, int timesCrafted, int repairCost) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftRepairAndDisenchant;
        }
    }

    record CraftLoom(String patternId, int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftLoom;
        }
    }

    record CraftNonImplemented() implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftNonImplemented;
        }
    }

    /**
     * Deprecated by Bedrock but still sent by the vanilla client. The result item list is not modelled; an empty list is accepted.
     */
    record CraftResults(int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType type() {
            return ItemStackRequestActionType.CraftResults;
        }
    }

    static ItemStackRequestAction take(final int amount, final ItemStackRequestSlot source, final ItemStackRequestSlot destination) {
        return new Take(amount, source, destination);
    }

    static ItemStackRequestAction place(final int amount, final ItemStackRequestSlot source, final ItemStackRequestSlot destination) {
        return new Place(amount, source, destination);
    }

    static ItemStackRequestAction swap(final ItemStackRequestSlot source, final ItemStackRequestSlot destination) {
        return new Swap(source, destination);
    }

    static ItemStackRequestAction drop(final int amount, final ItemStackRequestSlot source, final boolean throwRandomly) {
        return new Drop(amount, source, throwRandomly);
    }

    static ItemStackRequestAction destroy(final int amount, final ItemStackRequestSlot source) {
        return new Destroy(amount, source);
    }

    static ItemStackRequestAction consume(final int amount, final ItemStackRequestSlot source) {
        return new Consume(amount, source);
    }

    static ItemStackRequestAction create(final int slot) {
        return new Create(slot);
    }

    static ItemStackRequestAction screenBeaconPayment(final int primaryEffect, final int secondaryEffect) {
        return new BeaconPayment(primaryEffect, secondaryEffect);
    }

    static ItemStackRequestAction craftRecipe(final int recipeNetId, final int timesCrafted) {
        return new CraftRecipe(recipeNetId, timesCrafted);
    }

    static ItemStackRequestAction craftCreative(final int creativeItemNetId, final int timesCrafted) {
        return new CraftCreative(creativeItemNetId, timesCrafted);
    }

    static ItemStackRequestAction craftRecipeOptional(final int recipeNetId, final int filteredStringIndex) {
        return new CraftRecipeOptional(recipeNetId, filteredStringIndex);
    }

    static ItemStackRequestAction craftResults(final int timesCrafted) {
        return new CraftResults(timesCrafted);
    }

}
