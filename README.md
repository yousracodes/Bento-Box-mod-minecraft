# Bento Box

Minecraft Java Edition 1.20.1 mod starter using Forge 47.4.28 and Java 17.
Mod ID: `bentobox`. Java package: `com.bentobox`. Version: `0.1.0`.

## Windows commands

Run these from this folder in PowerShell:

```powershell
.\bento.bat build
.\bento.bat runClient
.\bento.bat genIntellijRuns
.\bento.bat runServer
```

`bento.bat` uses the locally installed JDK 17 at `C:\Program Files\Java\jdk-17`.
On another computer set `BENTO_JAVA_HOME` to a JDK 17 directory, or set
`JAVA_HOME` if that default installation path does not exist. For direct Gradle
commands, set `JAVA_HOME` to JDK 17 first; the system default Java 25 is not suitable.
The first build downloads Gradle, Forge, Minecraft dependencies and mappings.
The built mod is `build/libs/bentobox-0.1.0.jar`.

## IDE setup

Open this folder as a Gradle project in IntelliJ IDEA or Eclipse. Select JDK 17
as both the project SDK and Gradle JVM, then import/sync Gradle.
For Eclipse, run `.\bento.bat genEclipseRuns`.
For Linux/macOS use `sh gradlew build` or `sh gradlew runClient` with JDK 17.

## Project files

- `src/main/java/com/bentobox/BentoBoxMod.java`: mod entry point.
- `src/main/resources/META-INF/mods.toml`: Forge mod metadata.
- `src/main/resources/pack.mcmeta`: resource pack metadata.
- `gradle.properties`: mod name, author, version and Minecraft/Forge versions.

## Animated cherry bento models

Compact (4 compartments), Classic (6 compartments) and Deluxe (9 compartments) use the supplied Blockbench
geometry and embedded textures. No GeckoLib dependency is needed.
Find all three items in the Bento Box creative tab (also available in Food & Drinks), or use:

```mcfunction
/give @s bentobox:compact_bento
/give @s bentobox:classic_bento
/give @s bentobox:deluxe_bento
```

Hold a box and right-click to toggle its lid. The transition takes half a second,
and reversing it mid-animation continues from its current position. The server
stores the lid state on each individual item. Check first/third person views,
both hands, dropped items, and F3+T resource reloading when reviewing in-game.
Right-click a block face with a bento to place it. Placed boxes face the player.

- **Shift + right-click a placed box:** toggle its animated lid, even with an item held.
- **Right-click a placed box:** open its food inventory (Compact 2x2, Classic 3x2, Deluxe 3x3).
- **Left-click a placed box:** drop it as a single item, keeping its food and appearance.

The cherry-themed GUI includes the player's inventory and hotbar. Standard clicking,
dragging and shift-clicking transfer food. Non-food items and nested bentos are
rejected; normal stack limits and item NBT are preserved. Storage persists through
world saves, dropping, picking up and placing again. Removing a box invalidates
its open menus. Lid state is independent of whether the inventory screen is open.
Open placed boxes display the stored food icons inside their matching compartments.
Larger stacks appear as taller piles (up to six visual layers); these layers represent
the stack quantity and do not create extra items. Food uses its own item textures,
including resource-pack overrides and standard modded food models. Closing the lid
hides the food. Changes made through the GUI synchronize the world display.

When a compartment contains **exactly one** standard `BlockItem` food, it displays
the associated placed block model instead of the flat icon. The model is centered
and scaled uniformly to fit. Two or more items use the existing texture pile.
Items without a supported static block model keep their normal item appearance;
block-entity-only renderers are not used. This does not place real blocks or change
which foods the bento accepts.

Custom placement foods can be mapped through a resource pack. Add one JSON file
per mapping under `assets/<namespace>/bento_food_displays/<name>.json`, for example:

```json
{
  "item": "example_food_mod:rice_meal",
  "block": "example_food_mod:placed_rice_meal",
  "properties": { "facing": "north" }
}
```

Replace the example IDs with the food mod's real IDs. `properties` is optional.
Mappings override automatic block detection and are client resources, reloadable
with F3+T. Missing optional mods are skipped. No extra food mod is required.

### Display Delight integration

When Display Delight 1.4.x is installed, Bento Box reads its own live food-to-block
associations automatically, including supported add-on foods. No individual JSON
files are needed. Explicit JSON mappings take priority; otherwise the integration
prefers a direct food display, then a small plate, then a regular plate. Standard
BlockItem detection remains available as a fallback. Foods still have to pass the
bento's edible-item check, and only single items use block models.
Display Delight models with a `stacks` property are rendered with `stacks=1`,
regardless of their default serving count. Unsupported
renderers fall back to the food icon. Display Delight is optional and is not bundled.

For development testing, put `displaydelight-1.4.0.jar` and
`FarmersDelight-1.20.1-1.2.6.jar` into `libs/` (not `run/mods/`), then run:

```powershell
.\bento.bat -PdisplayDelightCompat runGameTestServer
.\bento.bat -PdisplayDelightCompat runClient
```

This profile remaps the production jars for the development environment. Test one
`farmersdelight:beef_stew`, `farmersdelight:cake_slice`, or `farmersdelight:hamburger`
inside either placed bento. Open its lid to see the block, then increase the count
to two to check the texture-pile fallback. The catalog is in
`docs/displaydelight-food-catalog.md`; installed mod versions control the actual
available associations. Other Display Delight versions may require bridge updates.
Bento box crafting is not implemented yet.

Run `.\bento.bat runGameTestServer` for the server integration tests covering all three
capacities, food validation, stack limits, NBT persistence, shift-click transfers,
pickup drops, and menu invalidation. Test sources are excluded from the release JAR.

The import script `tools/import_models.py` converts the two original `.bbmodel`
files into render meshes and extracts their embedded PNGs. Originals are untouched.
To reimport, run `python tools/import_models.py <6-slot.bbmodel> <9-slot.bbmodel>`.

The Compact mesh is derived from Classic by removing the middle column and shortening
the body and animated lid by six model units (about 27% narrower). It reuses the
cherry textures and keeps the same compartment depth and height. After reimporting
the originals, run `python tools/make_compact_model.py` to regenerate it.

Future assets go in `src/main/resources/assets/bentobox/` and recipes/data go in
`src/main/resources/data/bentobox/`.
A development server requires accepting Minecraft's EULA in `run/eula.txt` after
its initial launch. Only accept it after reading and agreeing to it.

Official documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/

## Wooden chopsticks

Craft two sticks together (shapeless) for reusable wooden chopsticks, find them
in the Bento Box or Food & Drinks creative tab, or use
`/give @s bentobox:wooden_chopsticks`.

Place a bento and shift + right-click to open its lid. With empty chopsticks,
right-click a food compartment once to transfer one serving onto the chopsticks.
The food appears between the tips and in the tooltip. Picking up food does not eat
it or require hunger. Right-click an empty compartment or a matching food stack with room to put the
serving back. Full or incompatible compartments leave it on the chopsticks.

Then hold right-click away from the bento with the loaded chopsticks to eat: you can walk
away, look away, or close/pick up the original bento. Eating uses the food's normal
duration, hunger, saturation, effects, and consumption behavior. Leftover bowls or
bottles go into your inventory (or drop if it is full). Releasing right-click cancels
eating and keeps the serving on the chopsticks. Full players can only eat foods
that normally allow it. The chopsticks remain reusable after eating.

Carried food and its NBT persist when switching items, dropping/picking up the
chopsticks, and saving/reloading. The serving leaves the bento when picked up, so
players sharing a box cannot take the same last serving. Creative pickup copies a
serving without reducing the box's contents. Empty hands still open the usual
inventory; shift + right-click still toggles the lid.

The chopsticks use the supplied `models/chopsticks-v1-blockbench.bbmodel`, including
its embedded texture, tapered geometry, and diagonal presentation. Each stick pivots
at its grip and opens over six ticks when food is picked up. The actual carried food
appears centered between the spreading tips, in inventory, hand, dropped-item, and
item-frame views. Eating empties the chopsticks and returns them to the resting pose.
The mesh and texture reload with F3+T. Regenerate these assets with
`python tools/import_chopsticks.py` after editing the source model.

Hand views anchor the thick grip ends inside the player's palm. In third person,
the sticks extend forward and slightly downward with the food between their tips;
left-hand transforms mirror the right-hand pose. First-person views place the grips
near the lower hand and angle the tips toward the center of the screen. The source
model's diagonal Display pose is used for inventory, ground, and frame presentation.

To remove carried food without a bento, Shift + right-click in the air. The serving
moves into your inventory, or drops beside you if it is full. This preserves food
NBT and does not require hunger. Shift + right-click on a bento still toggles its
lid. Normal eating still requires hunger (except always-edible foods); an action-bar
message explains when a full hunger bar prevents eating.
