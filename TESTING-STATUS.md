# Testing status of the `tested-only` branch

This branch holds only what was tried by hand and seen working in game. Setup: a Bedrock 1.26.51 Realm, a Java 26.3 client and ViaProxy 3.4.14-SNAPSHOT (`1546174`), on 2026-09-22.

It is the `bedrock-1.26.51` branch cut at the last tested fix, plus the falling block fix, with the untested sleeping pose removed. Everything added after that (crafting, drag placement, stack sizes, boats, world clocks, cooldowns, trading, enchanting, anvil, beacon, books, End credits, toasts, the hurt tilt suppression on joining, the code review fixes) lives only on `bedrock-1.26.51`.

## Seen working in game

- Joining the Realm; terrain and textures load in the Overworld and the Nether
- Moving items inside the inventory, inside chests, and between a chest and the inventory
- Shift-click: inventory to hotbar and back, chest to inventory and back, furnace to inventory and back, armor on and off
- Putting armor on and taking it off by clicking
- Single and double chests, including the double chest staying joined when opened
- Furnaces open, and items can be put in and taken out
- Doors open and close both halves, including clicking the top half
- Breaking a bed removes both halves
- Dropped items and XP orbs no longer show name tags
- No red hurt flash on joining
- Falling gravel shows and lands correctly

Also checked, but in the proxy log rather than by eye: hunger values arrive from the Realm.

## Known problems, not fixed

- Sessions drop after 1 to 3 minutes: `juice: Lost connectivity`, in the NetherNet transport, not in ViaBedrock
- Walking through doors sometimes pulls you back
- A small sideways hurt tilt on joining
- Fired arrows render dark

## Base

This branch uses my own 1.26.51 protocol update (`d7153a5`), not the official one merged upstream as RaphiMC/ViaBedrock#415. It also includes the open upstream PRs #404 to #407 by nekohacker591. My inventory fixes are built on those PRs, and they were tested together as one unit.
