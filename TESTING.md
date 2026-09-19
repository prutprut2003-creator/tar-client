# Verification record — updated 19 September 2026

## Public hosted build

[GitHub Actions build 35425079764](https://github.com/prutprut2003-creator/tar-client/actions/runs/35425079764)
completed successfully on Windows Server 2022 with Temurin 21.0.10 and Gradle 9.3.0.
Source commit: `7360ecf14345ed9bb9a9c496846dead52b10487d`.

- The normal Gradle/Loom pipeline compiled the client and launcher, remapped the
  Fabric JAR, and created the Windows application image and ZIP.
- All 7 launcher tests passed; 0 failed and 0 were skipped.
- The downloaded artifact's SHA-256 matched the build-generated checksum:
  `f8b71ab8b51baa7c5417d43d19c5611229bcb210dff73abde95710a9e04a352b`.
- Required launcher/runtime files are present. The launcher EXE remains unsigned.

This hosted build resolves the local Gradle build limitation described below.
It does not test interactive game rendering, account login, or Smart App Control
acceptance. Those limitations remain.

The remaining sections record the earlier local verification of 18 September.

## Passed

- Java 21 compilation of the launcher and client sources against Minecraft Java
  1.21.11 (Yarn `1.21.11+build.6`) and Fabric API `0.141.6+1.21.11`.
- Production remapping of the client mod from named to intermediary mappings,
  including mixin annotations and shadow members.
- Bytecode validation of 11 mixin classes, 16 injection targets and 11 shadow
  fields. The checker verifies target names/descriptors, callback argument types,
  static/instance matching and the lightmap uniform invocation ordinal.
- Seven automated launcher tests: safe download paths; Mojang launch rules;
  configuration round-trip and bounds; Fabric version constraints; local mod
  import/version/duplicate checks; missing-dependency blocking; explicit demo
  sessions and token-safe session formatting.
- Live download/install integration against Mojang, Fabric and Modrinth:
  84 library/classpath entries and over 4,500 assets verified by checksum.
- Fabric API, Mod Menu, BetterF3, Cloth Config and Placeholder API installed for
  1.21.11, including nested-library dependency validation.
- Windows app image generated, containing an executable and OpenJDK 21.0.10
  runtime. The bundled `java.exe` reports the expected version.
- The seven tests also pass using that bundled runtime.
- The real Swing launcher UI was rendered to an image and visually inspected.

## Not verified

- Full Minecraft boot or world gameplay. The smoke test reaches Fabric Loader,
  which exits while canonicalizing a library path because this environment's
  Windows sandbox denies Java's parent-directory traversal. This occurs before
  Tar's mixins are loaded. It does **not** prove that the mods work in game.
- The packaged EXE did not expose a usable native window in this restricted
  environment. The UI image is an offscreen rendering of the actual Swing
  components, not evidence of a successful interactive desktop session.
- Microsoft/Xbox/Minecraft account login: no approved application client ID or
  account was supplied.
- Multiplayer latency against a live server, sound playback, full-screen monitor
  transitions, actual frame rendering and arbitrary extra-mod compatibility.

This is why the artifact is labeled a preview. No tests were disabled to claim a
successful gameplay run. The normal Gradle/Loom build also encountered the same
Windows canonical-path restriction; the delivered binaries were compiled and
remapped through a direct pipeline using the same official dependencies.

## Suggested real-machine acceptance check

1. Extract the ZIP and open Tar Client. Confirm navigation and settings persistence.
2. Run Install / verify, then Minecraft demo. Confirm the title screen and a world.
3. Open Right Shift. Toggle each feature, adjust its values, close and reopen the
   menu, then restart Minecraft to confirm persistence.
4. Drag HUD panels and resize the game window. Check FPS and potion countdowns.
5. In a suitable test world, wear nearly broken armor, verify warning timing and
   volume, then disable the sound and confirm it stops.
6. Check held, dropped, inventory, third-person and item-frame scales, including a
   filled map and a per-item override.
7. Press F3+B with outline customization enabled and disabled; verify collision
   behavior stays vanilla. Check the always-on option.
8. Test F11 on each monitor and return to windowed mode. Test shield, fire,
   fullbright, fog and glint in appropriate scenes.
9. Open Mod Menu and BetterF3 settings. Import a compatible local Fabric JAR,
   toggle it, and try a Modrinth install with dependencies.
10. Configure your registered Microsoft app, sign in yourself, launch the owned
    game and check your ping on a multiplayer server.
