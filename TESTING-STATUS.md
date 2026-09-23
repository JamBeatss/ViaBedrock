# Testing status of the `bedrock-1.26.51` branch

Tested by hand on a live Bedrock 1.26.51 Realm (Java 26.3 client through ViaProxy), 2026-09-22:

- Joining a Realm, chunks, block and item data for 1.26.50/51
- Inventory: click moves, shift-click (inventory, chest, furnace, armor), armor equip/unequip
- Chests (single and double), furnaces, doors, beds, dropped item and XP orb name tags
- Hunger/health sync, falling blocks

**UNTESTED: crafting** (commit "Crafting: read recipes, match 2x2/3x3 grids, ..."). It compiles and
the proxy starts, but it has not been tried in game yet. Expect bugs in recipe reading, grid
matching and the craft request.

Also untested: sleeping pose, drag placement (QUICK_CRAFT), the join hurt-event suppression.

Known unresolved: sessions drop after 1-3 minutes at the WebRTC layer ("juice: Lost connectivity")
inside the NetherNet transport library, not in ViaBedrock code.

This branch includes four unreviewed upstream PRs (RaphiMC/ViaBedrock #404-#407) plus fixes on top.
