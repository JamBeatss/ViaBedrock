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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import net.lenni0451.mcstructs_bedrock.forms.elements.*;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.UiContainer;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.Enchant_Type;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.protocol.model.inventory.ItemStackRequestSlot;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.rewriter.item.ItemDataRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.storage.AuthData;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static net.raphimc.viabedrock.protocol.packet.ContainerClicks.*;
import static net.raphimc.viabedrock.protocol.packet.CraftingTranslator.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackRequestSlots.*;
import static net.raphimc.viabedrock.protocol.packet.ItemStackResponses.*;

/**
 * Villager trading, enchanting table, anvil, beacon and book editing.
 */
public final class SpecialScreenPackets {

    private SpecialScreenPackets() {
    }

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.UPDATE_TRADE, ClientboundPackets26_3.MERCHANT_OFFERS, wrapper -> {
            // On Bedrock this packet opens the villager trade screen and carries its offers
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final byte containerId;
            final int tier;
            final String displayName;
            final boolean newTradeScreen;
            final Tag data;
            try {
                containerId = wrapper.read(Types.BYTE); // container id
                wrapper.read(Types.BYTE); // container type
                wrapper.read(BedrockTypes.VAR_INT); // size
                tier = wrapper.read(BedrockTypes.VAR_INT); // trader tier
                wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
                wrapper.read(BedrockTypes.VAR_LONG); // last trading player unique id
                displayName = wrapper.read(BedrockTypes.STRING); // display name
                newTradeScreen = wrapper.read(Types.BOOLEAN); // use new trade screen
                wrapper.read(Types.BOOLEAN); // using economy trade
                data = wrapper.read(BedrockTypes.NETWORK_TAG); // offers
            } catch (Throwable e) { // A layout mismatch must not disconnect the player, the trade screen just stays closed
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to read UPDATE_TRADE", e);
                wrapper.cancel();
                return;
            }

            final List<InventoryTracker.TradeOffer> offers = new ArrayList<>();
            try {
                if (data instanceof CompoundTag dataTag && dataTag.get("Recipes") instanceof ListTag<?> recipes) {
                    for (Tag recipeTag : recipes) {
                        if (!(recipeTag instanceof CompoundTag recipe)) continue;
                        if (recipe.getInt("tier") > tier) continue; // Locked until the villager levels up
                        final BedrockItem buyA = bedrockItemFromNbt(wrapper.user(), recipe.get("buyA"), recipe.getInt("buyCountA"));
                        final BedrockItem buyB = bedrockItemFromNbt(wrapper.user(), recipe.get("buyB"), recipe.getInt("buyCountB"));
                        final BedrockItem sell = bedrockItemFromNbt(wrapper.user(), recipe.get("sell"), 0);
                        if (buyA.isEmpty() || sell.isEmpty()) continue;
                        offers.add(new InventoryTracker.TradeOffer(buyA, buyB, sell, recipe.getInt("netId"), recipe.getInt("maxUses") > 0 && recipe.getInt("uses") >= recipe.getInt("maxUses")));
                    }
                }
            } catch (Throwable e) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to read villager trades", e);
            }
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "UPDATE_TRADE: id=" + containerId + " tier=" + tier + " offers=" + offers.size() + " newTradeScreen=" + newTradeScreen);

            UiContainer screen = inventoryTracker.getCurrentContainer() instanceof UiContainer open && open.type() == ContainerType.TRADE && open.containerId() == containerId ? open : null;
            if (screen == null) {
                if (inventoryTracker.isAnyScreenOpen()) {
                    wrapper.cancel();
                    return;
                }
                final TextComponent title = TextUtil.stringToTextComponent(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(displayName));
                screen = tradeContainer(wrapper.user(), containerId, title, null, newTradeScreen);
                inventoryTracker.setCurrentContainer(screen);
                final PacketWrapper openScreen = PacketWrapper.create(ClientboundPackets26_3.OPEN_SCREEN, wrapper.user());
                openScreen.write(Types.VAR_INT, (int) containerId); // container id
                openScreen.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getBedrockToJavaContainers().get(ContainerType.TRADE)); // type
                openScreen.write(Types.TAG, TextUtil.textComponentToNbt(title)); // title
                openScreen.send(BedrockProtocol.class);
            }
            inventoryTracker.tradeOffers().clear();
            inventoryTracker.tradeOffers().addAll(offers);
            writeMerchantOffers(wrapper, screen, inventoryTracker, Math.max(1, Math.min(5, tier + 1)));
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_ENCHANT_OPTIONS, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final List<InventoryTracker.EnchantOption> options = new ArrayList<>();
            try {
                final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // options count
                for (int i = 0; i < count; i++) {
                    final int cost = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // level cost
                    wrapper.read(BedrockTypes.INT_LE); // equipment slot mask
                    final List<int[]> enchants = new ArrayList<>();
                    for (int list = 0; list < 3; list++) { // enchants that activate on equip, on held use and on self use
                        final int enchantCount = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                        for (int j = 0; j < enchantCount; j++) {
                            final int type = wrapper.read(Types.UNSIGNED_BYTE); // enchant type
                            final int level = wrapper.read(Types.UNSIGNED_BYTE); // enchant level
                            enchants.add(new int[]{type, level});
                        }
                    }
                    wrapper.read(BedrockTypes.STRING); // enchant name (random glyph text)
                    final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant net id
                    options.add(new InventoryTracker.EnchantOption(cost, netId, enchants));
                }
            } catch (Throwable e) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to read enchanting options (" + options.size() + " read so far)", e);
            }
            ViaBedrock.getPlatform().getLogger().log(Level.FINE, "PLAYER_ENCHANT_OPTIONS: " + options.size() + " options, costs " + options.stream().map(InventoryTracker.EnchantOption::cost).toList());
            inventoryTracker.enchantOptions().clear();
            inventoryTracker.enchantOptions().addAll(options);
            sendEnchantOptions(wrapper.user(), inventoryTracker);
        });
        protocol.registerServerbound(ServerboundPackets26_3.RENAME_ITEM, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            inventoryTracker.setAnvilName(wrapper.read(Types.STRING)); // item name
            updateUiScreenResult(wrapper.user(), inventoryTracker);
        });
        protocol.registerServerbound(ServerboundPackets26_3.SELECT_TRADE, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int index = wrapper.read(Types.VAR_INT); // selected offer
            if (!(inventoryTracker.getCurrentContainer() instanceof UiContainer screen) || screen.type() != ContainerType.TRADE) return;
            inventoryTracker.setSelectedTrade(index);
            final InventoryTracker.TradeOffer offer = selectedOffer(inventoryTracker);
            if (offer != null && !inventoryTracker.hasPendingItemStackRequests()) {
                sendItemStackRequest(wrapper.user(), inventoryTracker, buildTradeAutofillActions(inventoryTracker, screen, offer));
            }
            updateUiScreenResult(wrapper.user(), inventoryTracker);
        });
        protocol.registerServerbound(ServerboundPackets26_3.CONTAINER_BUTTON_CLICK, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int containerId = wrapper.read(Types.VAR_INT); // container id
            final int buttonId = wrapper.read(Types.VAR_INT); // button id
            // Like trade selection, wait for an unanswered move: the enchant request is built on the tracked input and lapis.
            // The Java client predicts nothing for the button, so the player can simply click again
            if (inventoryTracker.getCurrentContainer() instanceof UiContainer screen && screen.javaContainerId() == containerId && screen.type() == ContainerType.ENCHANTMENT
                    && !inventoryTracker.hasPendingItemStackRequests()) {
                sendItemStackRequest(wrapper.user(), inventoryTracker, buildEnchantActions(inventoryTracker, screen, buttonId));
            }
        });
        protocol.registerServerbound(ServerboundPackets26_3.SET_BEACON, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int primary = wrapper.read(Types.BOOLEAN) ? wrapper.read(Types.VAR_INT) : -1; // primary effect
            final int secondary = wrapper.read(Types.BOOLEAN) ? wrapper.read(Types.VAR_INT) : -1; // secondary effect
            if (!(inventoryTracker.getCurrentContainer() instanceof UiContainer screen) || screen.type() != ContainerType.BEACON) return;
            final ItemStackRequestSlot payment = requestSlotInfo(inventoryTracker, screen, 0);
            if (payment == null || screen.getItem(0).isEmpty()) return;
            // Sent even while another move is unanswered: the Java client closes the beacon screen right after confirming,
            // so waiting would drop the payment. A request built on stale state is rejected and resynced by the server
            sendItemStackRequest(wrapper.user(), inventoryTracker, List.of(
                    ItemStackRequestAction.screenBeaconPayment(primary == -1 ? 0 : bedrockEffectId(primary), secondary == -1 ? 0 : bedrockEffectId(secondary)),
                    ItemStackRequestAction.consume(1, payment)
            ));
        });
        protocol.registerServerbound(ServerboundPackets26_3.EDIT_BOOK, null, wrapper -> {
            wrapper.cancel();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final int slot = wrapper.read(Types.VAR_INT); // slot
            final int pageCount = wrapper.read(Types.VAR_INT); // pages count
            final List<String> pages = new ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                pages.add(wrapper.read(Types.STRING)); // page
            }
            final String title = wrapper.read(Types.OPTIONAL_STRING); // title (present when signing)
            if (slot < 0 || slot > 8) return; // Bedrock edits books by hotbar slot
            final BedrockItem book = inventoryTracker.getInventoryContainer().getItem(slot);
            if (book == null || book.isEmpty()) return;
            int existingPages = 0;
            if (book.tag() != null && book.tag().get("pages") instanceof ListTag<?> existing) {
                existingPages = existing.size();
            }
            for (int i = 0; i < pages.size(); i++) {
                final PacketWrapper bookEdit = PacketWrapper.create(ServerboundBedrockPackets.BOOK_EDIT, wrapper.user());
                bookEdit.write(BedrockTypes.VAR_INT, slot); // book slot
                bookEdit.write(BedrockTypes.UNSIGNED_VAR_INT, i < existingPages ? 0 : 1); // operation (replace page / add page)
                bookEdit.write(BedrockTypes.VAR_INT, i); // page index
                bookEdit.write(BedrockTypes.STRING, pages.get(i)); // page text
                bookEdit.write(BedrockTypes.STRING, ""); // photo name
                bookEdit.sendToServer(BedrockProtocol.class);
            }
            for (int i = existingPages - 1; i >= pages.size(); i--) {
                final PacketWrapper bookEdit = PacketWrapper.create(ServerboundBedrockPackets.BOOK_EDIT, wrapper.user());
                bookEdit.write(BedrockTypes.VAR_INT, slot); // book slot
                bookEdit.write(BedrockTypes.UNSIGNED_VAR_INT, 2); // operation (delete page)
                bookEdit.write(BedrockTypes.VAR_INT, i); // page index
                bookEdit.sendToServer(BedrockProtocol.class);
            }
            if (title != null) {
                final AuthData authData = wrapper.user().get(AuthData.class);
                final PacketWrapper bookEdit = PacketWrapper.create(ServerboundBedrockPackets.BOOK_EDIT, wrapper.user());
                bookEdit.write(BedrockTypes.VAR_INT, slot); // book slot
                bookEdit.write(BedrockTypes.UNSIGNED_VAR_INT, 4); // operation (finalize / sign)
                bookEdit.write(BedrockTypes.STRING, title); // title
                bookEdit.write(BedrockTypes.STRING, authData != null && authData.getDisplayName() != null ? authData.getDisplayName() : ""); // author
                bookEdit.write(BedrockTypes.STRING, authData != null && authData.getXuid() != null ? authData.getXuid() : ""); // xuid
                bookEdit.sendToServer(BedrockProtocol.class);
            }
            // Keep the tracked book in step so the next edit knows how many pages exist
            final BedrockItem updated = book.copy();
            if (updated.tag() == null) updated.setTag(new CompoundTag());
            final ListTag<CompoundTag> pageList = new ListTag<>(CompoundTag.class);
            for (String page : pages) {
                final CompoundTag pageTag = new CompoundTag();
                pageTag.putString("text", page);
                pageTag.putString("photoname", "");
                pageList.add(pageTag);
            }
            updated.tag().put("pages", pageList);
            inventoryTracker.getInventoryContainer().setItem(slot, updated);
        });
    }

    static UiContainer tradeContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position, final boolean newTradeScreen) {
        if (newTradeScreen) {
            return new UiContainer(user, containerId, ContainerType.TRADE, title, position, new int[]{UI_TRADE2_INGREDIENT_1, UI_TRADE2_INGREDIENT_2, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.Trade2Ingredient1Container, ContainerEnumName.Trade2Ingredient2Container, ContainerEnumName.Trade2ResultPreviewContainer});
        }
        return new UiContainer(user, containerId, ContainerType.TRADE, title, position, new int[]{UI_TRADE_INGREDIENT_1, UI_TRADE_INGREDIENT_2, UiContainer.RESULT_PREVIEW}, new ContainerEnumName[]{ContainerEnumName.TradeIngredient1Container, ContainerEnumName.TradeIngredient2Container, ContainerEnumName.TradeResultPreviewContainer});
    }

    // ---- Special screens: anvil, villager trading, enchanting table, beacon ----


    static String itemIdentifier(final InventoryTracker inventoryTracker, final BedrockItem item) {
        return item == null || item.isEmpty() ? null : inventoryTracker.user().get(ItemRewriter.class).getItems().inverse().get(item.identifier());
    }

    static String customNameOf(final BedrockItem item) {
        if (item == null || item.tag() == null) return null;
        final CompoundTag display = item.tag().getCompoundTag("display");
        return display != null && display.get("Name") instanceof StringTag name ? name.getValue() : null;
    }

    static int repairCostOf(final BedrockItem item) {
        return item == null || item.tag() == null ? 0 : item.tag().getInt("RepairCost");
    }

    static InventoryTracker.TradeOffer selectedOffer(final InventoryTracker inventoryTracker) {
        final int index = inventoryTracker.selectedTrade();
        return index >= 0 && index < inventoryTracker.tradeOffers().size() ? inventoryTracker.tradeOffers().get(index) : null;
    }

    static boolean satisfiesCost(final BedrockItem slotItem, final BedrockItem cost) {
        if (cost == null || cost.isEmpty()) return true;
        return slotItem != null && !slotItem.isEmpty() && slotItem.identifier() == cost.identifier() && slotItem.amount() >= cost.amount();
    }

    static ItemStackRequestSlot createdOutputDestination(final InventoryTracker inventoryTracker, final BedrockItem result, final ContainerInput action) {
        if (action == ContainerInput.QUICK_MOVE) {
            for (int i : INVENTORY_FILL_ORDER) {
                final BedrockItem existing = inventoryTracker.getInventoryContainer().getItem(i);
                if (existing == null || existing.isEmpty()) {
                    return playerInventorySlot(inventoryTracker, i);
                }
            }
            return null;
        }
        final BedrockItem held = inventoryTracker.getHudContainer().getItem(0);
        if (held != null && !held.isEmpty() && (held.isDifferent(result) || held.amount() + result.amount() > maxStackOf(inventoryTracker, result))) {
            return null; // Can't pick the result up onto a different or full cursor stack
        }
        return cursorSlot(inventoryTracker);
    }

    /**
     * Recomputes the Java result slot of the open anvil or trade screen. Bedrock computes these results on the client, so the server never sends them.
     */
    static void updateUiScreenResult(final UserConnection user, final InventoryTracker inventoryTracker) {
        if (!(inventoryTracker.getCurrentContainer() instanceof UiContainer screen)) return;
        final int resultSlot = screen.resultSlot();
        if (resultSlot == -1) return;
        BedrockItem result = BedrockItem.empty();
        int cost = 0;
        switch (screen.type()) {
            case ANVIL -> {
                final BedrockItem input = screen.getItem(0);
                final BedrockItem material = screen.getItem(1);
                final String name = inventoryTracker.anvilName();
                final boolean rename = name != null && !name.isEmpty() && !name.equals(customNameOf(input));
                if (!input.isEmpty() && (rename || !material.isEmpty())) {
                    result = input.copy();
                    if (rename) setCustomName(result, name);
                    // Estimate only: the Java client needs a cost above zero to allow taking the result, the server does the real check
                    cost = Math.max(1, repairCostOf(input) + repairCostOf(material) + (rename ? 1 : 0) + (material.isEmpty() ? 0 : 2));
                }
            }
            case TRADE -> {
                final InventoryTracker.TradeOffer offer = selectedOffer(inventoryTracker);
                if (offer != null && !offer.outOfStock() && satisfiesCost(screen.getItem(0), offer.buyA()) && satisfiesCost(screen.getItem(1), offer.buyB())) {
                    result = offer.sell().copy();
                }
            }
            default -> {
                return; // Grindstone, loom, stonecutter, cartography and smithing results are not predicted
            }
        }
        screen.setItem(resultSlot, result);
        final PacketWrapper setSlot = PacketWrapper.create(ClientboundPackets26_3.CONTAINER_SET_SLOT, user);
        setSlot.write(Types.VAR_INT, (int) screen.javaContainerId()); // container id
        setSlot.write(Types.VAR_INT, 0); // revision
        setSlot.write(Types.SHORT, (short) screen.javaSlot(resultSlot)); // slot
        setSlot.write(VersionedTypes.V26_3.item, screen.getJavaItem(resultSlot)); // item
        setSlot.send(BedrockProtocol.class);
        if (screen.type() == ContainerType.ANVIL) {
            sendJavaContainerData(user, screen, 0, cost); // repair cost
        }
    }

    static void sendJavaContainerData(final UserConnection user, final Container container, final int property, final int value) {
        final PacketWrapper containerData = PacketWrapper.create(ClientboundPackets26_3.CONTAINER_SET_DATA, user);
        containerData.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
        containerData.write(Types.SHORT, (short) property); // property id
        containerData.write(Types.SHORT, (short) value); // value
        containerData.send(BedrockProtocol.class);
    }

    static List<ItemStackRequestAction> buildScreenResultActions(final InventoryTracker inventoryTracker, final UiContainer screen, final ContainerInput action) {
        final BedrockItem result = screen.getItem(screen.resultSlot());
        if (result == null || result.isEmpty()) return new ArrayList<>();
        final ItemStackRequestSlot destination = createdOutputDestination(inventoryTracker, result, action);
        if (destination == null) return new ArrayList<>();
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        switch (screen.type()) {
            case ANVIL -> {
                final BedrockItem input = screen.getItem(0);
                final BedrockItem material = screen.getItem(1);
                final ItemStackRequestSlot inputSlot = requestSlotInfo(inventoryTracker, screen, 0);
                if (input.isEmpty() || inputSlot == null) return new ArrayList<>();
                inventoryTracker.setPendingFilterStrings(List.of(inventoryTracker.anvilName() != null ? inventoryTracker.anvilName() : ""));
                actions.add(ItemStackRequestAction.craftRecipeOptional(0, 0)); // The rename text is the request's first filter string
                actions.add(ItemStackRequestAction.craftResults(1));
                actions.add(ItemStackRequestAction.consume(input.amount(), inputSlot));
                if (!material.isEmpty()) {
                    final String inputId = itemIdentifier(inventoryTracker, input);
                    final String materialId = itemIdentifier(inventoryTracker, material);
                    // Combining with the same item or a book uses one; repair materials use up to four (each repairs a quarter)
                    final int used = "minecraft:enchanted_book".equals(materialId) || (materialId != null && materialId.equals(inputId)) ? 1 : Math.min(material.amount(), 4);
                    actions.add(ItemStackRequestAction.consume(used, requestSlotInfo(inventoryTracker, screen, 1)));
                }
            }
            case TRADE -> {
                final InventoryTracker.TradeOffer offer = selectedOffer(inventoryTracker);
                final ItemStackRequestSlot firstSlot = requestSlotInfo(inventoryTracker, screen, 0);
                if (offer == null || firstSlot == null) return new ArrayList<>();
                actions.add(ItemStackRequestAction.craftRecipe(offer.netId(), 1));
                actions.add(ItemStackRequestAction.craftResults(1));
                actions.add(ItemStackRequestAction.consume(offer.buyA().amount(), firstSlot));
                if (!offer.buyB().isEmpty()) {
                    actions.add(ItemStackRequestAction.consume(offer.buyB().amount(), requestSlotInfo(inventoryTracker, screen, 1)));
                }
            }
            default -> {
                return new ArrayList<>();
            }
        }
        final ItemStackRequestSlot output = createdOutputSlot(inventoryTracker);
        actions.add(action == ContainerInput.QUICK_MOVE ? ItemStackRequestAction.place(result.amount(), output, destination) : ItemStackRequestAction.take(result.amount(), output, destination));
        inventoryTracker.setCreatedOutputPreview(result.copy());
        return actions;
    }

    /**
     * Java fills the trade inputs from the inventory when a trade is selected. One stack per input keeps every action's net id valid.
     */
    static List<ItemStackRequestAction> buildTradeAutofillActions(final InventoryTracker inventoryTracker, final UiContainer screen, final InventoryTracker.TradeOffer offer) {
        final List<ItemStackRequestAction> actions = new ArrayList<>();
        final BedrockItem[] costs = {offer.buyA(), offer.buyB()};
        final Container inventory = inventoryTracker.getInventoryContainer();
        final List<Integer> usedSources = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            final BedrockItem cost = costs[slot];
            if (cost == null || cost.isEmpty()) continue;
            final BedrockItem current = screen.getItem(slot);
            if (!current.isEmpty() && current.identifier() != cost.identifier()) continue; // Leave other items for the player to move
            final int maxStack = maxStackOf(inventoryTracker, cost);
            final int space = maxStack - (current.isEmpty() ? 0 : current.amount());
            if (space <= 0) continue;
            int bestSource = -1;
            for (int i = 0; i < 36; i++) {
                final BedrockItem candidate = inventory.getItem(i);
                if (candidate == null || candidate.isEmpty() || candidate.identifier() != cost.identifier() || usedSources.contains(i)) continue;
                if (!current.isEmpty() && candidate.isDifferent(current)) continue;
                if (bestSource == -1 || candidate.amount() > inventory.getItem(bestSource).amount()) bestSource = i;
            }
            if (bestSource == -1) continue;
            usedSources.add(bestSource);
            final int amount = Math.min(space, inventory.getItem(bestSource).amount());
            actions.add(ItemStackRequestAction.place(amount, playerInventorySlot(inventoryTracker, bestSource), requestSlotInfo(inventoryTracker, screen, slot)));
        }
        return actions;
    }

    static List<ItemStackRequestAction> buildEnchantActions(final InventoryTracker inventoryTracker, final UiContainer screen, final int optionIndex) {
        if (optionIndex < 0 || optionIndex >= inventoryTracker.enchantOptions().size()) return new ArrayList<>();
        final InventoryTracker.EnchantOption option = inventoryTracker.enchantOptions().get(optionIndex);
        final BedrockItem input = screen.getItem(0);
        final BedrockItem lapis = screen.getItem(1);
        final ItemStackRequestSlot inputSlot = requestSlotInfo(inventoryTracker, screen, 0);
        final ItemStackRequestSlot lapisSlot = requestSlotInfo(inventoryTracker, screen, 1);
        if (input.isEmpty() || lapis.isEmpty() || lapis.amount() < optionIndex + 1 || inputSlot == null || lapisSlot == null) return new ArrayList<>();

        // Predict the enchanted item so the Java view shows it before the next full sync
        final BedrockItem enchanted = input.copy();
        enchanted.setAmount(1);
        if ("minecraft:book".equals(itemIdentifier(inventoryTracker, input))) {
            final Integer enchantedBookId = inventoryTracker.user().get(ItemRewriter.class).getItems().get("minecraft:enchanted_book");
            if (enchantedBookId != null) enchanted.setIdentifier(enchantedBookId);
        }
        if (enchanted.tag() == null) enchanted.setTag(new CompoundTag());
        final ListTag<CompoundTag> enchantments = new ListTag<>(CompoundTag.class);
        for (int[] enchant : option.enchants()) {
            final CompoundTag enchantment = new CompoundTag();
            enchantment.putShort("id", (short) enchant[0]);
            enchantment.putShort("lvl", (short) enchant[1]);
            enchantments.add(enchantment);
        }
        enchanted.tag().put("ench", enchantments);

        final List<ItemStackRequestAction> actions = new ArrayList<>();
        actions.add(ItemStackRequestAction.craftRecipe(option.netId(), 1));
        actions.add(ItemStackRequestAction.craftResults(1));
        actions.add(ItemStackRequestAction.consume(1, inputSlot));
        actions.add(ItemStackRequestAction.consume(optionIndex + 1, lapisSlot));
        // The enchanted item goes back into the (now empty) input slot
        actions.add(ItemStackRequestAction.place(1, createdOutputSlot(inventoryTracker), new ItemStackRequestSlot(inputSlot.containerName(), inputSlot.slot(), 0)));
        inventoryTracker.setCreatedOutputPreview(enchanted);
        return actions;
    }

    static BedrockItem bedrockItemFromNbt(final UserConnection user, final Tag tag, final int countOverride) {
        if (!(tag instanceof CompoundTag itemTag)) return BedrockItem.empty();
        final String name = itemTag.getString("Name");
        if (name == null || name.isEmpty()) return BedrockItem.empty();
        final Integer id = user.get(ItemRewriter.class).getItems().get(name.contains(":") ? name : "minecraft:" + name);
        if (id == null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown item in trade offer: " + name);
            return BedrockItem.empty();
        }
        final int count = countOverride > 0 ? countOverride : itemTag.getByte("Count");
        final BedrockItem item = new BedrockItem(id, itemTag.getShort("Damage"), (byte) count, itemTag.getCompoundTag("tag"));
        return count <= 0 ? BedrockItem.empty() : item;
    }

    static int javaEffectId(final int bedrockEffectId) {
        final String bedrockIdentifier = BedrockProtocol.MAPPINGS.getBedrockEffects().inverse().get(bedrockEffectId);
        final String javaIdentifier = bedrockIdentifier != null ? BedrockProtocol.MAPPINGS.getBedrockToJavaEffects().get(bedrockIdentifier) : null;
        final Integer javaId = javaIdentifier != null ? BedrockProtocol.MAPPINGS.getJavaEffects().get(javaIdentifier) : null;
        return javaId != null ? javaId : -1;
    }

    static int bedrockEffectId(final int javaEffectId) {
        final String javaIdentifier = BedrockProtocol.MAPPINGS.getJavaEffects().inverse().get(javaEffectId);
        if (javaIdentifier == null) return 0;
        for (Map.Entry<String, String> entry : BedrockProtocol.MAPPINGS.getBedrockToJavaEffects().entrySet()) {
            if (entry.getValue().equals(javaIdentifier)) {
                final Integer bedrockId = BedrockProtocol.MAPPINGS.getBedrockEffects().get(entry.getKey());
                return bedrockId != null ? bedrockId : 0;
            }
        }
        return 0;
    }

    static void sendEnchantOptions(final UserConnection user, final InventoryTracker inventoryTracker) {
        if (!(inventoryTracker.getCurrentContainer() instanceof UiContainer screen) || screen.type() != ContainerType.ENCHANTMENT) return;
        for (int i = 0; i < 3; i++) {
            final InventoryTracker.EnchantOption option = i < inventoryTracker.enchantOptions().size() ? inventoryTracker.enchantOptions().get(i) : null;
            int javaEnchantment = -1, level = -1;
            if (option != null && !option.enchants().isEmpty()) {
                final Enchant_Type type = Enchant_Type.getByValue(option.enchants().get(0)[0]);
                javaEnchantment = type != null ? ItemDataRewriter.getJavaEnchantmentIndex(type) : -1;
                level = javaEnchantment == -1 ? -1 : option.enchants().get(0)[1];
            }
            sendJavaContainerData(user, screen, i, option != null ? option.cost() : 0); // level cost
            sendJavaContainerData(user, screen, 4 + i, javaEnchantment); // enchantment hint
            sendJavaContainerData(user, screen, 7 + i, level); // enchantment hint level
        }
    }

    static void writeMerchantOffers(final PacketWrapper wrapper, final UiContainer screen, final InventoryTracker inventoryTracker, final int villagerLevel) {
        final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
        wrapper.write(Types.VAR_INT, (int) screen.javaContainerId()); // container id
        wrapper.write(Types.VAR_INT, inventoryTracker.tradeOffers().size()); // offers count
        for (InventoryTracker.TradeOffer offer : inventoryTracker.tradeOffers()) {
            wrapper.write(VersionedTypes.V26_3.itemCost, itemRewriter.javaItem(offer.buyA())); // first cost
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(offer.sell())); // result
            wrapper.write(VersionedTypes.V26_3.optionalItemCost, offer.buyB().isEmpty() ? null : itemRewriter.javaItem(offer.buyB())); // second cost
            wrapper.write(Types.BOOLEAN, offer.outOfStock()); // out of stock
            wrapper.write(Types.INT, 0); // uses
            wrapper.write(Types.INT, 1); // max uses
            wrapper.write(Types.INT, 0); // xp
            wrapper.write(Types.INT, 0); // special price (the Bedrock counts already include it)
            wrapper.write(Types.FLOAT, 0F); // price multiplier
            wrapper.write(Types.INT, 0); // demand
        }
        wrapper.write(Types.VAR_INT, villagerLevel); // villager level
        wrapper.write(Types.VAR_INT, 0); // villager xp
        wrapper.write(Types.BOOLEAN, false); // show progress
        wrapper.write(Types.BOOLEAN, false); // can restock
    }

}
