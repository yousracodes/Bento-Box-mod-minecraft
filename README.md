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

Classic (6 compartments) and Deluxe (9 compartments) use the supplied Blockbench
geometry and embedded textures. No GeckoLib dependency is needed.
Find both items in the Bento Box creative tab (also available in Food & Drinks), or use:

```mcfunction
/give @s bentobox:classic_bento
/give @s bentobox:deluxe_bento
```

Hold a box and right-click to toggle its lid. The transition takes half a second,
and reversing it mid-animation continues from its current position. The server
stores the lid state on each individual item. Check first/third person views,
both hands, dropped items, and F3+T resource reloading when reviewing in-game.
Food storage, inventory screens, eating, and crafting are not implemented yet.

The import script `tools/import_models.py` converts the two original `.bbmodel`
files into render meshes and extracts their embedded PNGs. Originals are untouched.
To reimport, run `python tools/import_models.py <6-slot.bbmodel> <9-slot.bbmodel>`.

Future assets go in `src/main/resources/assets/bentobox/` and recipes/data go in
`src/main/resources/data/bentobox/`.
A development server requires accepting Minecraft's EULA in `run/eula.txt` after
its initial launch. Only accept it after reading and agreeing to it.

Official documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
