# Code signing policy

Status: unsigned preview; no signing provider approval or certificate exists.
Tarre Industries is the project's branding. It is not a registered company or
the name of a verified certificate holder.

We intend to request free community signing from
[SignPath Foundation](https://signpath.org/terms.html). Acceptance is discretionary;
publishing the source does not itself establish eligibility or Windows trust.
Do not describe a download as signed until its actual signature has been verified.

The initial maintainer, reviewer and proposed release approver is
[@prutprut2003-creator](https://github.com/prutprut2003-creator), owner of the
[source repository](https://github.com/prutprut2003-creator/tar-client).
No person has yet been assigned signing access. Before onboarding,
the maintainer must enable multi-factor authentication, satisfy the provider's
requirements, and approve each proposed release for signing. Changes from other
contributors must receive maintainer review.

Builds must be traceable to the published source commit. Only Tar's launcher
should be submitted for our project's signature; bundled Java files retain their
upstream signatures. The provider must review the jpackage-generated launcher
and bundled dependencies before configuring the signing artifact.

If accepted, update this page with the actual team accounts, signing process,
and provider attribution. Until then, no sponsorship or certification is claimed.

See [Privacy](PRIVACY.md) for network activity, [Third-party software](THIRD-PARTY.md)
for dependencies, and [Windows signing](WINDOWS-SIGNING.md) for verification.
