param([string]$Gradle = 'gradle')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& $Gradle --no-daemon :launcher:test :client-mod:build :launcher:fatJar
if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
$tarJdkBin = Split-Path (Get-Command javac.exe).Source
$tarDist = Join-Path $PSScriptRoot 'dist'
$tarRuntime = Join-Path $tarDist 'runtime'
if (Test-Path $tarRuntime) { throw "Existing runtime at $tarRuntime. Rename the dist folder before rebuilding." }
New-Item -ItemType Directory -Force $tarDist | Out-Null
& (Join-Path $tarJdkBin 'jlink.exe') --add-modules ALL-MODULE-PATH --output $tarRuntime --strip-debug --no-header-files --no-man-pages
if ($LASTEXITCODE -ne 0) { throw 'Runtime creation failed.' }
# Retain native Java commands: GameInstaller launches runtime/bin/java.exe.
& (Join-Path $tarJdkBin 'jpackage.exe') --type app-image --input launcher/build/libs --dest $tarDist --name 'Tar Client' --main-jar tar-launcher.jar --main-class dev.tarclient.launcher.TarLauncher --runtime-image $tarRuntime --app-version 0.2.0 --vendor 'Tarre Industries' --java-options '-Dfile.encoding=UTF-8'
if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed.' }
Copy-Item README.md,SIGN-IN-SETUP.md,WINDOWS-SIGNING.md,CODE-SIGNING.md,PRIVACY.md,CONTRIBUTING.md,LICENSE,THIRD-PARTY.md,TESTING.md,'Start Tar Client.cmd' (Join-Path $tarDist 'Tar Client')
Copy-Item licenses (Join-Path $tarDist 'Tar Client') -Recurse
Compress-Archive -LiteralPath (Join-Path $tarDist 'Tar Client') -DestinationPath (Join-Path $tarDist 'TarClient-0.2.0-Windows.zip')
$tarHash = Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $tarDist 'TarClient-0.2.0-Windows.zip')
Set-Content -LiteralPath (Join-Path $tarDist 'SHA256SUMS.txt') -Value ($tarHash.Hash.ToLowerInvariant() + '  TarClient-0.2.0-Windows.zip') -Encoding ascii
