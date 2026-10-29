# Joker Weapons (Fabric 1.21.11)

Idea by Thejoker56. Needs Fabric Loader + Fabric API on Minecraft 1.21.11.

## Storm Mace
- Normal mace: same attack speed and smash attack as vanilla.
- **Right-click** fires a wind charge (same 0.5s cooldown as a real wind charge).
- **Max entity reach** while held in the main hand (64 blocks, the highest the game allows).

## Orbital Spear
- Netherite spear with **Sharpness VI**. Normal right-click still does the spear charge.
- **Sneak + right-click** while looking at the ground (up to 160 blocks away): a red beam comes down for 2 seconds, then 43 TNT get flung out in rings around the spot. 30 second cooldown.

## Getting them
- Creative: end of the **Combat** tab.
- Command (op): `/jokerweapons give mace` or `/jokerweapons give spear` (add a player name to give it to someone else).

## Tweaking
Ring sizes, TNT count, cooldown and range are constants at the top of `OrbitalStrike.java`. Reach is `MACE_EXTRA_REACH` in `Weapons.java`.

## Building
Push to GitHub; the Actions "build" workflow produces the jar under **Artifacts**.
