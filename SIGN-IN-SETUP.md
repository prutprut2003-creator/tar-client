# Microsoft sign-in setup

Tar Client implements Microsoft's device-code flow, Xbox Live authentication,
XSTS, Minecraft token exchange and a Minecraft profile check. It does not collect
your password, borrow another launcher's identity, or provide cracked accounts.

**This build has no registered Microsoft application ID. Full account sign-in
cannot work until a valid application ID with the necessary Xbox/Minecraft API
access is supplied.** That registration is an external prerequisite, not something
the launcher can create automatically. Minecraft demo mode is available without it.

1. Register an application in Microsoft Entra with support for personal Microsoft
   accounts. Enable public-client flows for device-code authentication. Do not
   create or embed a client secret for this desktop app.
2. Follow Microsoft's current Xbox/Minecraft third-party application access
   requirements. A generic Entra registration alone may not grant Minecraft API
   access. A rejected application typically fails during Xbox or Minecraft login.
3. Copy the **Application (client) ID** into Tar Client â†’ Launcher settings â†’
   Microsoft sign-in, then save. An app ID is public configuration, not a password.
4. Click **Sign in with Microsoft**. In your own browser, visit the displayed
   Microsoft verification page and enter the temporary code. Sign in yourself.
5. Return to Tar Client. A successful Minecraft profile lookup shows your player
   name. Click **Play Minecraft**.

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
