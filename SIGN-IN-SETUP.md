# Microsoft sign-in setup

Tar Client implements Microsoft's device-code flow, Xbox Live authentication,
XSTS, Minecraft token exchange and a Minecraft profile check. It does not collect
your password, borrow another launcher's identity, or provide cracked accounts.

**Tar Client 0.2.1 includes its registered Microsoft application ID automatically:**
`c8d8f6e2-12dc-4499-911c-1c7294e91f44`.
This is a public identifier, not a password. Users do not need an Azure account,
an app registration, or any manual application ID setup.

**Minecraft API approval is still pending.** The publisher submitted the review
request on 20 September 2026 and the form confirmed receipt. Submission does not
mean approval. Full account sign-in has not been verified. Entra
registration and public-client configuration alone do not grant Minecraft access.

1. Open **Accounts** and click **Sign in with Microsoft**.
2. Complete the official Microsoft device-code flow in your own browser.
3. A successful Minecraft profile lookup will show your player name. Full-game
   launch requires that successful lookup and a valid entitlement.

Missing, empty or whitespace-only application ID settings use the built-in ID,
including when upgrading an older preview. An explicit custom ID remains in use.
The advanced override is under **Settings > Microsoft connection**; clearing it
and saving restores the built-in ID.

For source forks that use their own identity: register an application supporting
personal Microsoft accounts and enable public-client flows for device-code sign-in.
Do not create or embed a client secret. Request Minecraft API access through
the official [AppID review form](https://aka.ms/mce-reviewappid).

Use an account with Minecraft Java ownership or a qualifying active subscription.
Family restrictions or missing Xbox profiles may need to be resolved in Microsoft's
own account interface. Tar Client only reports the service error; it cannot remove
account restrictions.

Sessions are held in memory and never saved to the preferences JSON. Sign in after
each launcher restart. Signing out clears the launcher's in-memory session. The
game necessarily receives a token when launched; its temporary Java argument file
is deleted after startup or process exit. If the launcher is forcibly killed during
startup, a `.launch-*.args` file could remain in the game folder; do not share it.

Microsoft references:
- https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app
- https://learn.microsoft.com/en-us/gaming/gdk/docs/services/fundamentals/xbox-services-overview

No live Microsoft account login was performed during development of this build.
