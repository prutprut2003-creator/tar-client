# Tar Client 0.3.0 — Windows preview

A standalone Windows launcher for **Minecraft Java 1.21.11 / Fabric 0.19.5**,
plus its bundled Tar Client cosmetic and HUD mod. This is a real desktop app,
not a website or a mod-only download.

Tar Client is a free, MIT-licensed project branded **Tarre Industries**. This is
a project name, not a claim of incorporation or a verified Windows publisher.
Minecraft itself remains a separate product and requires its own entitlement
for full-game play.

[Public releases](https://github.com/prutprut2003-creator/tar-client/releases) include checksums. The current source is 0.3.0; check the download version before installing. Windows signing remains subject to the limitation below.

## Code signing policy

The current preview is unsigned. Free community signing is being explored;
no provider has accepted this project yet. See [Code signing policy](CODE-SIGNING.md),
[Privacy](PRIVACY.md), and [Contributing](CONTRIBUTING.md).

**Windows signing remains unfinished.** Smart App Control may block this unsigned
EXE without a Run anyway option. See [WINDOWS-SIGNING.md](WINDOWS-SIGNING.md).
Do not disable Windows protection or use the CMD launcher to evade that block.

## Start

1. Extract the entire `TarClient-0.3.0-Windows.zip` archive.
2. Open `Tar Client/Tar Client.exe`. Keep `app` and `runtime` beside the EXE.
   For ordinary startup errors (not a Windows security block), `Start Tar Client.cmd`
   starts the same launcher using the bundled Java runtime and shows startup errors.
3. Click **Install / verify files**. The first download includes Minecraft,
   Fabric, assets, Fabric API, Mod Menu, BetterF3 and their required dependencies.
4. To try it without an account, choose **Try Minecraft demo**. This launches
   Minecraft's own restricted demo, not an offline full-game account.
5. Open **Accounts** and choose **Sign in with Microsoft**. Tar Client's public
   application ID is built in; you do not need to register or paste an ID.
   **Minecraft API approval is pending, so full sign-in is not yet verified.**
   See [SIGN-IN-SETUP.md](SIGN-IN-SETUP.md).

Java 21 is bundled. The download targets Windows x64. The launcher defaults to
4 GB game memory; change it under **Settings**.

## What's new in 0.3.0

- 33 module cards, with a compact armor HUD and individual background controls for every Tar HUD panel.
- Added shulker previews, Spotify song/artwork overlay, zoom, freelook, clock, inventory, saturation, hit color, coordinates, reach measurement and server information.
- Added module profiles with Bedwars/SMP presets, permission-respecting time changes, disconnect confirmation and an unfocused FPS cap.
- Integrated compatible TierTagger, Smooth Motion Blur, 3D Skin Layers and Resource Tree downloads. Their toggles apply when restarting Minecraft; each exposes its upstream settings.
- Read [MODULES.md](MODULES.md) for controls, settings and verification limits.

## Earlier improvements in 0.2.1

- Built in Tar Client's registered Microsoft application ID for every download, including upgrades with empty settings. Explicit custom IDs remain supported.
- Back up either previous bundled core (0.1.0 or 0.2.0) when upgrading, keeping worlds and other mods.
- Minecraft API approval is still pending; registration alone does not confirm full login works.

## Earlier improvements in 0.2.0

- Fixed borderless F11 resizing: native window callbacks no longer overwrite the target monitor dimensions.
- Right Shift opens/closes the new searchable, categorized in-game module menu; it respects text input and key-binding screens.
- Redesigned launcher with a dark sidebar, original block artwork, module cards and an Accounts page.
- Modrinth discovery now has category filters, sorting, pagination and asynchronous mod icons.
- Start Microsoft sign-in or account creation from Accounts. The official browser completes the account flow; the registered Tar application ID is now included.
- Existing worlds and settings are reused. The previous bundled core is backed up during upgrade.

**Verification status:** see [TESTING.md](TESTING.md) for the current launcher checks and earlier verification. Eighteen remapped mixins were checked against Minecraft 1.21.11 bytecode. A real GLFW window passed three fullscreen/windowed cycles with geometry callbacks at 3840x2160. The Swing launcher and live Modrinth catalog were rendered and inspected. Full gameplay, multi-monitor transitions and Microsoft account login remain unverified. See [TESTING.md](TESTING.md).

## Included features

| Feature | Controls |
| --- | --- |
| Fullbright | Enable and brightness strength; visual lightmap only |
| Crosshair | Color, arm length, gap, thickness, dot, outline, third-person display |
| Item size | First-person, inventory, dropped, third-person and fixed/display scales; per-item overrides; held maps included |
| No fog | Terrain fog; optional fluid-fog removal |
| FPS | Live game FPS; position, scale and colors |
| Ping | Your multiplayer latency in ms; singleplayer is labeled separately |
| Armor status | Every armor slot, remaining points or percent; configurable warning threshold, volume and cooldown |
| Borderless fullscreen | F11 uses the current monitor's borderless desktop-sized window |
| Keystrokes | Actual bound movement/jump keys, mouse buttons, left/right CPS, pressed color |
| BetterF3 | Actual BetterF3 mod, installed from Modrinth for 1.21.11 |
| Potion status | Effect name, amplifier and duration; beneficial/harmful filters |
| Hitbox outlines | F3+B color, eye-height box and direction; optional always-on display |
| Low / side shield | Downward and outward offsets |
| Low fire | Fire-overlay vertical offset |
| Glint | Hide enchantment glint or set its vanilla strength and speed |

**Right Shift** opens Tar settings in game; it can be rebound in Minecraft's key
settings. The title and pause screens also have a **Tar settings** button.
Choose **Edit HUD** in a world to drag panels. Numeric controls, colors
and toggles save automatically. HUD coordinates are percentages so layouts adapt
to resolution changes. Disable a module to restore its normal rendering path.

Glint customization here means strength and speed, not replacement textures or
arbitrary glint colors. Hitbox customization affects debug drawing only; it never
changes collision boxes, attack reach or server behavior. Fullbright, no fog and
item scaling start disabled. Armor alerts use the game's pling sound, so the
Minecraft Master volume also affects their audibility.

The first-person map renderer is covered separately by item scaling. Per-item
overrides apply across contexts, for example:

`minecraft:diamond_sword=0.65;minecraft:shield=0.8`

Change borderless mode while windowed, then press F11. BetterF3 and other
third-party mods expose their own settings in the in-game **Mods** menu. Their
schemas differ, so they are not copied into Tar's built-in settings form.

## Add and manage mods

- **Discover mods:** search Modrinth, filtered to Fabric and 1.21.11. Installation
  resolves required dependencies and checks SHA-512 hashes before committing files.
- **Installed mods → Import Fabric JARs:** select one or more local `.jar` files.
  Forge-only, server-only and declared incompatible versions are rejected.
- Use **Check dependencies** after importing local JARs. Missing requirements and
  declared conflicts block launching and are reported by name.
- Disabling renames a mod to `.jar.disabled`. Removing moves it into
  `removed-mods`, allowing recovery. Tar's core mod is managed by the launcher.
- Close Minecraft before changing installed mods. Use the in-game menu for Tar
  settings while playing, so the launcher cannot overwrite live settings.

Version declarations and hashes cannot guarantee that arbitrary third-party mods
work together. Renderer replacements and other mods that modify the same game
code still need a real launch test. Required Modrinth dependencies install
automatically; optional dependencies do not. Search supports pagination, sorting and category filters.

## Files and data

The default instance is `%LOCALAPPDATA%\TarClient\instance-1.21.11` (on standard
Windows profiles). It is separate from the official `.minecraft` folder.

- `config/tarclient.json`: shared launcher/in-game settings.
- `mods/`: installed JARs and disabled JARs.
- `tar-mods.json`: Modrinth version and hash records.
- `saves/`, `screenshots/`, `resourcepacks/`: normal Minecraft content.
- `logs/latest.log`: Minecraft/Fabric log.
- `%LOCALAPPDATA%\TarClient\game-output.log`: last game's console output.
- `%LOCALAPPDATA%\TarClient\launcher.json`: memory and public application ID.

The first launch needs Internet access and several gigabytes of free disk space.
Minecraft files and third-party mods are downloaded from their providers; they
are not redistributed in this ZIP. This preview rechecks official metadata online
at launch, so it is not an offline launcher.

## Build and extend

Requires a JDK 21 and Gradle 9.3.0. Run `build-windows.ps1`, or:

```powershell
gradle :launcher:test :client-mod:build :launcher:fatJar
```

The GitHub Actions workflow builds an **unsigned preview** on a standard Windows
runner, only in a public repository. [Build 35425079764](https://github.com/prutprut2003-creator/tar-client/actions/runs/35425079764)
passed on 19 September 2026, including all seven launcher tests and Windows packaging.
It does not purchase services, sign executables, or publish releases automatically.

## Uninstall

Close Minecraft and Tar Client, then delete the extracted launcher folder.
To also remove downloaded files and settings, first back up worlds under
`%LOCALAPPDATA%\TarClient\instance-1.21.11\saves`, then delete
`%LOCALAPPDATA%\TarClient`. This second step deletes saved worlds, screenshots,
mods and settings. If you chose custom data or instance locations, use those
locations instead. No Windows service or registry installation is created.

## Extend the source

`launcher` is the desktop app; `client-mod` is the Fabric component; `common` holds
the shared declarative settings schema. Add a module to `ClientConfig.MODULES`
and implement its HUD/event/mixin behavior in `client-mod`; both settings menus
then discover its controls. Third-party mods need no source changes.

The release JAR must contain the **remapped** client mod, not a development JAR.
The build task embeds it under `bundled/tar-client.jar`. The Windows packaging
script deliberately retains `runtime/bin/java.exe` to launch the game.

Development diagnostics:

```powershell
java -jar tar-launcher.jar --smoke C:\path\to\isolated-test-instance
java -jar tar-launcher.jar --smoke C:\path\to\isolated-test-instance --launch
```

The second command starts Minecraft in demo mode. Never point a smoke test at an
existing personal instance. `-Dtar.data=...` and `-Dtar.instance=...` can override
the launcher data and game directories for isolated testing.

## References

- [Minecraft 1.21.11 release](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- [Fabric 1.21.11 developer notes](https://fabricmc.net/2025/12/05/12111.html)
- [Modrinth version API](https://docs.modrinth.com/api/operations/getprojectversions/)
- [Microsoft device-code flow](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
