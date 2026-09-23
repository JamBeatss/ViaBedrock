# Testing status of the `official-merged` branch

This branch is the official Bedrock 1.26.51 update (RaphiMC/ViaBedrock#415) plus nekohacker591's open inventory pull requests (#404-#407), ported to it, plus fixes for playing on a Bedrock Realm. Each commit's title says whether it was tested in game.

Setup used for testing: a Bedrock 1.26.51 Realm, a Java 26.3 client and ViaProxy 3.4.14-SNAPSHOT (`1546174`), on 2026-09-22. The in-game tests ran on the earlier `bedrock-1.26.51` branch, which used its own protocol update. The commits marked tested carry the same behaviour, since restructured into smaller classes, but this combined version on the official update has not been played yet.

## Seen working in game

- Joining the Realm; terrain and textures load in the Overworld and the Nether
- Moving items inside the inventory, inside chests, and between a chest and the inventory
- Shift-click: inventory to hotbar and back, chest to inventory and back, furnace to inventory and back, armor on and off
- Putting armor on and taking it off by clicking
- Single and double chests
- Furnaces
- Doors, including clicking the top half
- Breaking a bed removes both halves
- Falling gravel
- Dropped items and XP orbs no longer show name tags
- No red hurt flash on joining

## Not tested in game

- Crafting (the recipe reader has never run against a live server), drag placement, per-item stack sizes
- Villager trading, enchanting table, anvil, beacon, book editing, brewing stand, barrels and shulker boxes, and moving items into grindstones, looms, stonecutters, cartography and smithing tables
- Time of day sync, item cooldowns, End credits, toasts, boats, sleeping pose, the sideways tilt on joining
- Creative mode placement

## Automated tests

`./gradlew test` runs 18 JUnit tests. They check the item stack request and response formats against bytes from the CloudburstMC reference encoder, plus the slot mapping and how accepted requests update the tracked inventory.

## Known problems, not fixed

- Sessions drop after 1 to 3 minutes (`juice: Lost connectivity`, in the NetherNet transport, not in ViaBedrock)
- Walking through doors sometimes pulls you back
- Fired arrows render dark

## Other branches on this fork

- `tested-only`: only what was seen working in game, on the earlier `bedrock-1.26.51` protocol update
- `bedrock-1.26.51`: the earlier branch with everything, before the official update was merged
