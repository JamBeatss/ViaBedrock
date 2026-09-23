# Testing status of the `bedrock-1.26.51` branch

Tested by hand on a live Bedrock 1.26.51 Realm (Java 26.3 client through ViaProxy), 2026-09-22:

- Joining a Realm, chunks, block and item data for 1.26.50/51
- Inventory: click moves, shift-click (inventory, chest, furnace, armor), armor equip/unequip
- Chests (single and double), furnaces, doors, beds, dropped item and XP orb name tags
- Hunger/health sync, falling blocks

**UNTESTED: crafting** (commit "Crafting: read recipes, match 2x2/3x3 grids, ..."). It compiles and
the proxy starts, but it has not been tried in game yet. Expect bugs in recipe reading, grid
matching and the craft request.

Also untested: sleeping pose, drag placement (QUICK_CRAFT), the join hurt-event suppression, boat movement (MOVE_VEHICLE), per-item stack sizes.

Also untested (added 2026-09-22, late):

- World clock sync (time of day, sleeping through the night, `/time set`) and item cooldowns
- Villager trading (UPDATE_TRADE opens the screen, SELECT_TRADE autofills one stack per input, result slot trades once per click)
- Enchanting table (options as Java hints, button click to enchant request)
- Anvil rename and combine (the Java result is a local prediction; combined enchantments only show after a resync; repair material use is estimated, at most four)
- Beacon effect selection (the screen always reports a level 4 pyramid; the server checks the real one)
- Book and quill editing and signing
- End credits after the dragon, and the finished reply that returns you to the Overworld
- Toasts, shown on the action bar
- Item moves into anvils, grindstones, looms, stonecutters, cartography and smithing tables (their results are not predicted, so only anvil, trade and enchanting produce anything)
- Brewing stand slot order, barrel and shulker box container names
- Review fixes: late item stack responses still apply, queued clicks are dropped on container change, shaped recipes only mirror when the recipe allows it

Not implemented: recipe book "place recipe", grindstone/loom/stonecutter/cartography/smithing results, achievements.

Known unresolved: sessions drop after 1-3 minutes at the WebRTC layer ("juice: Lost connectivity")
inside the NetherNet transport library, not in ViaBedrock code.

This branch includes four unreviewed upstream PRs (RaphiMC/ViaBedrock #404-#407) plus fixes on top.
