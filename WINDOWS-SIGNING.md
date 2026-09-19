# Tar Client: the next step after a Smart App Control block

The current download is an unsigned development preview. It has not become a
signed or trusted release merely because a signing script is now included.

The selected route is now **free community signing**, with MIT-licensed public
source and an intended SignPath Foundation application. See [Code signing
policy](CODE-SIGNING.md). No purchase or business registration is planned.
Provider acceptance is still pending; the local signing script below is optional
tooling, not a requirement to buy a certificate.

The package audit found:

- `Tar Client.exe`: not signed.
- 127 bundled Java executables and DLLs: valid publisher signatures.

## What needs to happen

1. The publisher obtains an eligible **publicly trusted RSA code-signing identity**
   from a recognized certificate provider or signing service. This involves real
   identity verification and may require payment. A self-signed certificate or a
   made-up publisher name does not solve the public trust requirement.
2. Sign the final launcher EXE and timestamp it. Keep the private key in the
   provider's supported protected storage. Never send private keys, passwords or
   identity documents in chat.
3. Verify the signature and packaged native dependencies; create a fresh ZIP and
   checksums, then test it on the actual Windows installation with Smart App
   Control enabled. Do not redistribute the previous unsigned ZIP as signed.
4. Configure the separate Microsoft application ID for Minecraft account sign-in
   and test an actual game session. Code signing does not grant Minecraft API
   access, and an application ID does not sign Windows software.

Smart App Control can also evaluate application reputation. Signature validation
is necessary preparation, not proof of successful gameplay or a guarantee that
every policy on every computer will allow the application.

## Prepared tooling

`sign-windows.ps1` performs a read-only audit by default:

```powershell
.\sign-windows.ps1 -PackagePath '..\TarClient-Windows'
```

For a provider-managed RSA certificate accessible in your own Windows Personal
certificate store, it also supports an explicit signing operation. Supply your
certificate thumbprint, Windows SDK SignTool path and the provider's RFC 3161
timestamp URL. The script checks the code-signing certificate, runs SignTool,
verifies the resulting Authenticode signature and timestamp, then audits all
packaged EXE/DLL signatures. It does not change any Windows security setting or
trust store. It only accesses the specific certificate thumbprint you select.

Cloud-only services such as Microsoft's Artifact Signing require their own
provider integration instead of this certificate-store signing command. Choose a
service before configuring that integration.

Microsoft's Artifact Signing **Public Trust** option is eligibility-restricted:
the current documentation lists individual developers in the US and Canada, and
organizations in a broader set of countries. Do not buy or register a service
before checking its eligibility for your country and publisher type. Private Trust
and Public Trust Test profiles are not substitutes for public release signing.

This work has not created an account, purchased a certificate, performed identity
verification or signed any file.

## Corrected launch guidance

If Smart App Control has blocked Tar Client, the earlier **More info → Run anyway**
instructions do not apply. The optional `.cmd` launcher is for ordinary startup
troubleshooting; it is not a trust or security-block workaround. Do not disable
Smart App Control or install a test root certificate to use this preview.

## Official references

- [Smart App Control signing requirements](https://learn.microsoft.com/en-us/windows/apps/develop/smart-app-control/code-signing-for-smart-app-control)
- [Smart App Control FAQ](https://support.microsoft.com/en-us/windows/security/threat-malware-protection/smart-app-control-frequently-asked-questions)
- [Artifact Signing eligibility and setup](https://learn.microsoft.com/en-us/azure/artifact-signing/quickstart)
- [Public Trust and test profiles](https://learn.microsoft.com/en-us/azure/artifact-signing/concept-trust-models)
- [SignTool documentation](https://learn.microsoft.com/en-us/windows/win32/seccrypto/signtool)
