# Wraith — a Minecraft client *framework*

Fabric 1.21.x client core: a module/setting registry, event bus, profile-based config,
keybinds, an animated drag-and-drop click GUI and a HUD editor.

**Scope.** This is the client's chassis and chrome. It deliberately contains no PvP
cheats (no kill aura, auto-clicker, reach, hitbox, velocity, fly) and no
anti-cheat-bypass logic. See [SCOPE.md](SCOPE.md) for why, and for what this codebase
is genuinely good for.

## What is here

```
wraith/
├── build.gradle                 fabric-loom, Java 21
└── src/main/
    ├── java/com/wraith/
    │   ├── WraithClient.java    bootstrap + the one per-frame entry point
    │   ├── event/               EventBus, @EventTarget, Tick/RenderHud/World events
    │   ├── module/              Module, ModuleManager, Setting, Category
    │   │   └── settings/        Boolean / Number / Mode / Color
    │   ├── config/              ConfigManager - JSON profiles + autosave
    │   ├── gui/                 ClickGui, Theme, Render2D (the DrawContext adapter)
    │   │   ├── component/       CategoryPanel, ModuleCard (widgets drawn + hit in one pass)
    │   │   └── hud/             Anchor, HudModule, HudManager (drag / snap / toggle)
    │   ├── modules/             render / hud / qol / misc
    │   ├── util/                MCUtil, InputUtil (GLFW poll), ColorUtil, VanillaOptions
    │   └── mixin/               MinecraftClient, InGameHud, GameRenderer, Mouse, Camera
    └── resources/               fabric.mod.json, wraith.mixins.json
```

## Modules shipped

| Category | Module | What it does |
| --- | --- | --- |
| Render | `FullBright` | Drives the gamma option (no light-level writes) |
| Render | `Zoom` | FOV scaling with eased in/out and a zoom vignette |
| Render | `Freelook` | Decouples camera rotation from the player |
| Render | `CustomCrosshair` | Vector crosshair: style, gap, weight, colour |
| Render | `NoFovShift` | Holds FOV steady instead of vanilla's speed kick |
| HUD | `Watermark`, `ArrayList`, `Stats`, `Coordinates`, `ArmorStatus`, `Keystrokes` | Draggable, anchor-based HUD panels |
| QoL | `ToggleSprint` | Same job as the vanilla sprint toggle, minus its quirks |
| Misc | `ClickGui`, `HudEditor` | Theme + layout chrome (RShift opens the menu) |

## Build

Needs JDK 21, Gradle 8.14+ (or a wrapper), and internet access to Maven Central,
`maven.fabricmc.net` and Mojang's piston servers — Loom resolves the Minecraft jar,
Yarn mappings and Mixin from there.

```bash
cd wraith
gradle build          # or: gradle wrapper --gradle-version 8.14 && ./gradlew build
```

Drop `build/libs/wraith-0.1.0.jar` into `.minecraft/mods` with Fabric loader.
`gradle runClient` works too but wants ~4 GB of heap free; `runClient` on a small box
is what OOM-killed the previous attempt in this repo's history — the `build.gradle`
here keeps `org.gradle.jvmargs` at 2G for that reason.

Then check [docs/BUILDING.md](docs/BUILDING.md): it lists the four spots where Yarn
renames things between 1.21.x versions, and all of them are deliberately isolated.
