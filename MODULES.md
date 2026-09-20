# Tar Client 0.3.0 module guide

Open **Client modules** in the launcher or press **Right Shift** in Minecraft.
Search by name, toggle a module and open its settings. **Edit HUD** lets you drag
visible HUDs. All Tar HUD panels have a **Show background** switch, background
color/opacity, roundness, text color, position and scale. Backgrounds are off by
default, including when upgrading old settings. Visual and utility modules that
do not draw a HUD have no panel background to configure.

## New built-in modules

| Module | Use and customization |
| --- | --- |
| Armor status | Compact horizontal item strip, colored durability bars, optional vertical layout, percentage or remaining points, empty-slot display and existing sound alert controls. |
| Shulker box tooltips | 9-column contents grid on hover, optional Shift requirement and grid background. Minecraft still draws its outer tooltip frame. Only item contents supplied by the game are shown. |
| Spotify overlay | Windows desktop Spotify song, artist and album art. Adjustable position, scale, width, square/rounded/pill corners, artwork visibility and idle visibility. Uses local Windows media sessions; no Spotify password or OAuth setup in Tar. Browser playback may not identify as Spotify. |
| Zoom | Enable, then hold **C**. Configure multiplier and smooth transition; rebind in Minecraft Controls. |
| Freelook | Enable, then hold **Left Alt**. The camera enters third person and rotates independently while the player's aim stays unchanged. Releasing restores your prior perspective. Configure sensitivity and rebind in Controls. |
| Time changer | Enable, set the tick value, then press **Apply time** in the in-game module settings. Sends the normal `/time set` command. Requires server OP/command permission or singleplayer cheats; never changes time automatically. |
| Clock | Local computer time in AM/PM format, with optional seconds. |
| Inventory HUD | Main inventory grid, optional hotbar row and item count/durability overlays. |
| Saturation | Exact singleplayer saturation. Multiplayer is labeled as an estimate because vanilla does not continuously synchronize the actual value. |
| Hit color | Change the damage tint on player models. |
| Coordinates | Block X/Y/Z and optional dimension label. Enable Show background for a coordinate box. |
| Reach display | Distance from your eyes to the target hitbox at the last local attack, with display timeout and player-only filter. Does not change reach or confirm that the server accepted damage. |
| Server address | Connected server address, optional server name and server-list image. A placeholder is used if Minecraft has no icon. |
| Profiles | Save and load all Tar module settings. Bedwars and SMP presets are created once and never overwrite your saved versions. Available both in the launcher sidebar and the in-game Profiles module. |
| Smart disconnect | A Leave/Cancel confirmation before disconnecting through the pause menu. Preserves Minecraft's draft-report flow. Does not prevent server kicks or closing the operating-system window. |
| Limit unfocused FPS | Set a limit from 5 to 120 FPS while the game is unfocused. Respects any lower vanilla limit. |

## Integrated mods

These four module cards use compatible upstream mods. Enable the card before
launching; Tar downloads it and its required dependencies from Modrinth. If you
change enabled state in game, **close and relaunch Minecraft through Tar** to
apply it. Profiles store their enabled states; their detailed upstream settings
remain global and are edited through each mod's own settings screen.

| Module | Provider and settings |
| --- | --- |
| TierTagger | [Official TierTagger](https://modrinth.com/mod/tiertagger), with ukulib. Displays published PvP tiers; use Mod settings / Mods for tier-list and display settings. |
| Motion blur | [Smooth Motion Blur](https://modrinth.com/mod/smooth-motion-blur). Use Mod settings or `/motionblur 0` through `/motionblur 300` to tune strength. |
| 3D skins | [3D Skin Layers](https://modrinth.com/mod/3dskinlayers). Use Mod settings for the outer skin layer rendering options. |
| Pack organizer | [Resource Tree](https://modrinth.com/mod/resource-tree-mod). The Resource Packs screen gains subfolder navigation and folder-management controls. Use the Resource Packs button in its integration screen. |

The four integrations start disabled and are not copied into the Tar ZIP.
Installed-mod toggles for these four are reconciled with their Tar module setting
at the next launch. Disable them through **Client modules** when using Tar.

## Verification limits

Compilation, all 15 launcher/configuration tests, remapped mixin target checks,
and all four integration download/dependency/enable-disable checks passed. The
local test environment prevents Fabric startup during filesystem path resolution,
before Tar code runs; in-world rendering has not yet been verified. Its Windows
media-session service is also unavailable, so live Spotify metadata/artwork still
needs a check on a normal Windows desktop. See [TESTING.md](TESTING.md).

Minecraft account login still depends on the pending Mojang application review.
