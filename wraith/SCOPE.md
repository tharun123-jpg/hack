# Scope: what this project is, and what it will not become

## The line

Wraith gets the client architecture, the rendering, the GUI and the quality-of-life
features. It does not get, and will not be given:

- **Combat/advantage modules** — kill aura, auto-clicker, reach, hitbox expansion,
  velocity/knockback manipulation, scaffold, fly, nofall, X-ray, entity ESP.
- **Anti-cheat evasion** — "bypass" profiles, click-randomisation tuned to defeat CPS
  detection, packet reordering or spoofing, timer/speed hacks tuned around specific
  flags, or anything whose purpose is to stay invisible to Grim, Vulcan, NCP, etc.

Both halves are the actual product Vape sells. The first is winning PvP by not playing
PvP; the second exists so you can keep doing it after a server bans you for it.
Servers ban for both, and Hypixel-style bans take the whole account.

## Why the split is the useful one

Everything in this repo is the hard part of writing a client, and none of it is what
makes people angry or gets accounts terminated:

- a reflective event bus with cancellation and priorities,
- a module/setting model that serialises into diffable JSON profiles,
- a per-frame polled input layer instead of `Screen` overrides, so the menu never
  pauses the game or loses mouse capture (this is the trick that makes a client feel
  like a client),
- anchor-and-offset HUD layout so panels survive resize and GUI-scale changes,
- a rounded-rect/gradient/clip 2D renderer that is the only file touching
  `DrawContext`'s matrix API,
- five mixins, each doing one job.

Reuse all of that freely: your own server, singleplayer, creative building, screenshot
and streaming setups, a mod launcher, a QoL mod, or as a study of how mixin-driven
clients are structured. It's also exactly the architecture open-source clients like
Wurst and Meteor publish — where the QoL and render modules are shared openly and the
"bypass" market is not, which tells you where the work actually lives.

## If you want to go further

Legitimate directions from here, all of which build on this chassis:

- more HUD elements (biome/chunk borders, clock, keystroke history graph, scoreboard)
- a real shader/post-processing pipeline (bloom, motion blur, TAA) — genuinely hard,
  zero effect on other players
- world-space rendering: block outlines, trajectory preview for *your own* arrows in
  singleplayer, build guides, blueprint import/export
- a keybind manager, macros for repetitive building, an `.schem` preview
- a Fabric-server-side companion: your own minigame server with no anti-cheat, where
  any module you like is just a game rule you both agreed to
