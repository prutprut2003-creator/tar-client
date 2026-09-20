# Third-party software

Tar's own source is MIT licensed. Dependencies keep their original licenses.

Bundled in the launcher JAR:
- Gson 2.13.2 — Apache 2.0 — https://github.com/google/gson
- FlatLaf 3.7 — Apache 2.0 — https://github.com/JFormDesigner/FlatLaf
- Fabric Loader API/implementation 0.19.5 (version predicate parsing) — Apache 2.0 — https://github.com/FabricMC/fabric-loader
- TwelveMonkeys ImageIO WebP and supporting modules 3.15.2 — BSD 3-Clause — https://github.com/haraldk/TwelveMonkeys

The Windows package contains a Temurin OpenJDK 21.0.10 runtime, GPLv2 with
Classpath Exception and third-party notices. Its license files are retained
under `runtime/legal`. Corresponding upstream source is available at
https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.10%2B7 and
https://github.com/openjdk/jdk21u/tree/jdk-21.0.10%2B7 .

Downloaded on first installation, not bundled in this distribution:
- Minecraft 1.21.11 and its libraries/assets — Mojang/Microsoft and their respective authors
- Fabric Loader and Fabric API — FabricMC contributors
- Mod Menu — https://modrinth.com/mod/modmenu
- BetterF3 — https://modrinth.com/mod/betterf3
- Cloth Config API — https://modrinth.com/mod/cloth-config
- Placeholder API — https://modrinth.com/mod/placeholder-api

These projects and additional user-installed mods remain under their own licenses.
Tar does not claim authorship of BetterF3 or any third-party mod.

Optional 1.21.11 integrations, downloaded from Modrinth when enabled (not bundled):
- TierTagger 2.4.1 — MPL-2.0 — https://modrinth.com/mod/tiertagger — https://github.com/mctiers-dev/TierTagger (requires ukulib).
- Smooth Motion Blur 1.0.0 — LGPL-3.0-only — https://modrinth.com/mod/smooth-motion-blur — https://github.com/realjahleel/smooth-motion-blur.
- 3D Skin Layers 1.11.3 — tr7zw Protective License — https://modrinth.com/mod/3dskinlayers — https://github.com/tr7zw/3d-skin-layers.
- Resource Tree 1.2 — MIT — https://modrinth.com/mod/resource-tree-mod — https://github.com/Naw7k/resource-tree.

Versions above were verified during 0.3.0 development. The launcher resolves a
current compatible Fabric 1.21.11 release and verifies Modrinth's file checksum.
Required dependencies retain their upstream licenses.
