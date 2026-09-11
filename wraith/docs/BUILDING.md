# Building and porting notes

## Version pinning

`gradle.properties` targets Minecraft `1.21.11` with Yarn `1.21.11+build.6` and
loader `0.19.5`. Minecraft, mappings and Fabric API versions move together — change
them as a set, and check `https://fabricmc.net/use/installer/` for the matching loader.

The code was written against the **Yarn 1.21.11 javadoc**, verified for:

- `DrawContext#getMatrices()` returning `org.joml.Matrix3x2fStack` (2-arg translate/scale)
- `InGameHud#render(DrawContext, RenderTickCounter)` — a single overload, so the mixin
  selector cannot multi-match
- `SimpleOption#getValue()` / `setValue(T)`
- `Camera` private `yaw`/`pitch` fields, shadowed by `MixinCamera`
- `Identifier` (Yarn still does not use Mojang's `ResourceLocation` here)

## The four things to fix first if the build complains

Everything version-sensitive is in one of these places. Nothing else should need edits.

| Symptom | File | Fix |
| --- | --- | --- |
| `cannot find symbol matrices()` / `getMatrices()` | `gui/Render2D.java` | Use whichever accessor exists, and `pushMatrix()`/`popMatrix()` if the stack is a `MatrixStack` |
| `no suitable method found for drawText/drawTextWithShadow` | `gui/Render2D.java` | Text methods moved between `DrawableHelper` and `DrawContext` across 1.20→1.21 |
| Ambiguous `render` match in mixin | `mixin/MixinInGameHud.java` | Add a descriptor, e.g. `method = "render(Lnet/minecraft/client/gui/DrawContext;F)V"` for 1.21.1 |
| `getGamma`/`getPerspective` setter error | `util/VanillaOptions.java` | `setValue` vs `set`; every option read in the project goes through this file |

Loom's mixin error messages name the target and the version's real signature; when a
`@At` fails to resolve, prefer re-running with `-Dmixin.debug.export=true` and reading
`build/mods` output over guessing.

## Verifying without a compiler

This repo was authored in a sandbox with no JDK and no access to Maven Central or
Mojang's servers, so `javac` was not available. Instead, `tools/check_java.py` parses
every source with tree-sitter's Java grammar and verifies:

- zero `ERROR`/`MISSING` nodes (syntax)
- every `com.wraith.*` import resolves to a declared type
- every `new OwnType(...)` and every call on a Wraith-typed receiver matches a declared
  arity
- every `@Override` matches a method that actually exists in the Wraith supertype chain
  (this is what caught `onEnable`/`onDisable` being called by `Module.setEnabled` without
  ever being declared on `Module`)

Run it from the repo root:

```bash
pip install tree-sitter tree-sitter-java
python3 tools/check_java.py wraith/src/main/java
```

That cannot check Minecraft's own API — only `gradle build` can — so treat the first
compile as the porting step in the table above.

## Manual test checklist

1. `gradle build` succeeds, jar in `.minecraft/mods`, join a singleplayer world.
2. RShift opens the menu; the game keeps running behind it (no pause) and the mouse
   does not rotate the camera while over a panel.
3. Left click toggles a module, right click expands it, middle click starts a bind
   capture; Del on a bound module clears it.
4. Drag a panel header: it leaves the auto row; drag back into the top strip to rejoin.
5. Enable `HudEditor`: HUD boxes get outlines, drag one near a screen edge and release
   — it should stick to that corner and survive a window resize.
6. Set a few values, quit, relaunch: values, binds and HUD positions reload.
7. Edit `config/wraith/profiles/default.json`, save as `pvp.json`, switch profile — no
   file corruption should be able to prevent joining (bad entries fall back to defaults).
