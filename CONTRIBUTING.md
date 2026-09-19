# Contributing

Tar Client's source is available under the MIT license in LICENSE. Contributions
must be compatible with that license; dependencies retain their own licenses.
Tarre Industries is project branding, not a registered business.

Use JDK 21 and Gradle 9.3.0. Run `build-windows.ps1` for the Windows preview,
or `gradle :launcher:test :client-mod:build :launcher:fatJar` for code and tests.
See TESTING.md for the exact checks already completed and remaining limitations.

Submit focused pull requests with the problem, the change, and validation.
Cosmetic rendering and HUD improvements are in scope. Combat automation, reach
changes, authentication bypasses and server exploitation are outside this project.

Never commit credentials, identity documents, game accounts, private signing
material, local instances, downloaded Minecraft binaries, or personal logs.
Report ordinary bugs through repository issues once the public repository exists;
include game/mod versions and a minimal reproduction with sensitive data removed.
For a security issue, use GitHub's private vulnerability reporting if enabled.
If unavailable, ask for a private contact without publishing exploit details or
credentials in a public issue.

Maintainers review code and build-script changes before release. Release claims
must distinguish a successful build from actual gameplay testing, Microsoft
sign-in testing, and verified Windows signing.
