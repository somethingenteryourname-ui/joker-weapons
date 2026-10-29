# Joker Weapons (Paper 1.21.11 plugin)

Idea by Thejoker56. Server-side only: players join with a normal vanilla client, no mods or resource pack needed.

## Storm Mace
- Normal mace: same damage, attack speed and smash attack as vanilla.
- **Right-click** fires a wind charge (same 0.5s cooldown as a real wind charge).
- **Max entity reach** while held in the main hand (64 blocks, the highest the game allows).

## Orbital Spear
- Netherite spear with **Sharpness VI**. Normal right-click still does the spear charge.
- **Sneak + right-click** while looking at the ground (up to 160 blocks away): a red beam comes down for 2 seconds, then 43 TNT get flung out in rings around the spot. 30 second cooldown.

## Commands (op)
- `/jokerweapons give mace [player]`
- `/jokerweapons give spear [player]`
- `/jokerweapons reload` - reloads `plugins/JokerWeapons/config.yml`

Short alias: `/jw`.

## Settings
Everything (cooldowns, reach, TNT rings, range, whether the TNT breaks blocks) is in `config.yml`.

## Building
Push to GitHub; the Actions workflow builds the jar and posts it on the Releases page.
The older Fabric mod version is still on the `build-2` release.
