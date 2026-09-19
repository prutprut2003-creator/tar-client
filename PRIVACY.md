# Privacy

Tar Client has no Tar-operated analytics, advertising, account server, or automatic
crash-report upload in this preview. Opening the launcher reads and writes local
settings; installation, account sign-in, searches and game launching are initiated
by the user.

## Network activity

- Installation and launching fetch Minecraft metadata, libraries and assets from
  Mojang/Microsoft, and Fabric metadata/libraries from FabricMC. A launch checks
  installation metadata online and installs the default mods if needed.
- Mod discovery sends your search text, game version and loader filter to Modrinth.
  Installation fetches mod metadata and files, including required dependencies.
- Microsoft sign-in opens the provider's page in your browser. The launcher uses
  Microsoft, Xbox and Minecraft services to obtain a game session and check your
  entitlement. It does not ask for your Microsoft password.
- Opening a project's web page sends a normal browser request to that site.
  Providers receive normal connection data such as your IP address and request
  headers. Metadata can designate upstream file hosts for downloads.

Provider policies:
[Microsoft](https://privacy.microsoft.com/privacystatement) and
[Modrinth](https://modrinth.com/legal/privacy).
Fabric's service is operated by [FabricMC](https://fabricmc.net/); a separate
published policy covering its metadata/download services has not been verified.
Minecraft, multiplayer servers and any additional mods can have their own network
behavior and policies; Tar does not control or disable that behavior.

## Local data

The default data directory is `%LOCALAPPDATA%\TarClient`. It holds settings,
downloaded game files, mods, saves, screenshots and game output. Microsoft session
tokens are held in memory. To start Java, a temporary argument file contains the
game access token; deletion is attempted shortly after starting and when the game
exits. A crash or failed cleanup can leave that file behind. Do not share raw
instance folders or launch argument files. Review logs before posting them publicly.

Custom data paths are supported. See [Uninstall](README.md#uninstall) to remove
local data after backing up your worlds. No data is uploaded to Tar maintainers
unless you choose to share it yourself.
