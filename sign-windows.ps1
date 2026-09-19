[CmdletBinding(DefaultParameterSetName = 'Audit')]
param(
    [string]$PackagePath = (Join-Path $PSScriptRoot '..\TarClient-Windows'),
    [Parameter(Mandatory, ParameterSetName = 'Sign')]
    [switch]$Sign,
    [Parameter(Mandatory, ParameterSetName = 'Sign')]
    [ValidatePattern('^[A-Fa-f0-9]{40}$')]
    [string]$CertificateThumbprint,
    [Parameter(Mandatory, ParameterSetName = 'Sign')]
    [string]$SignToolPath,
    [Parameter(Mandatory, ParameterSetName = 'Sign')]
    [uri]$TimestampUrl
)

# Audit is the default. Signing only runs with explicit -Sign and a caller-selected
# certificate. This script never creates certificates, modifies trust stores,
# changes Windows security settings, or searches for private credentials.
$ErrorActionPreference = 'Stop'
$tarPackage = (Resolve-Path -LiteralPath $PackagePath).Path
$tarExe = Join-Path $tarPackage 'Tar Client.exe'
if (-not (Test-Path -LiteralPath $tarExe -PathType Leaf)) {
    throw "Tar Client.exe was not found in $tarPackage"
}

if ($Sign) {
    if (-not (Test-Path -LiteralPath $SignToolPath -PathType Leaf)) {
        throw 'Install the Windows SDK signing tools and supply the exact signtool.exe path.'
    }
    if ($TimestampUrl.Scheme -notin @('https', 'http') -or -not $TimestampUrl.Host) {
        throw 'Supply the RFC 3161 timestamp endpoint recommended by your signing provider.'
    }
    $tarCertificate = Get-Item -LiteralPath "Cert:\CurrentUser\My\$CertificateThumbprint"
    if (-not $tarCertificate.HasPrivateKey) { throw 'The selected certificate has no available private key.' }
    if ($tarCertificate.PublicKey.Oid.Value -ne '1.2.840.113549.1.1.1') {
        throw 'Smart App Control signing requires an RSA certificate.'
    }
    if ('1.3.6.1.5.5.7.3.3' -notin @($tarCertificate.EnhancedKeyUsageList.ObjectId.Value)) {
        throw 'The selected certificate is not a code-signing certificate.'
    }
    $tarNow = Get-Date
    if ($tarNow -lt $tarCertificate.NotBefore -or $tarNow -gt $tarCertificate.NotAfter) {
        throw 'The selected certificate is outside its validity period.'
    }
    $tarChain = [System.Security.Cryptography.X509Certificates.X509Chain]::new()
    try {
        if (-not $tarChain.Build($tarCertificate)) {
            throw 'The certificate did not validate against the Windows trust store. Do not add a test certificate to a trust store as a workaround.'
        }
    } finally { $tarChain.Dispose() }

    & $SignToolPath sign /sha1 $CertificateThumbprint /s My /fd SHA256 /td SHA256 /tr $TimestampUrl.AbsoluteUri /d 'Tar Client' $tarExe
    if ($LASTEXITCODE -ne 0) { throw 'Signing or timestamping failed.' }
    & $SignToolPath verify /pa /all /tw $tarExe
    if ($LASTEXITCODE -ne 0) { throw 'Signature or timestamp verification failed.' }
    $tarSigned = Get-AuthenticodeSignature -LiteralPath $tarExe
    if ($tarSigned.Status -ne 'Valid' -or $null -eq $tarSigned.TimeStamperCertificate) {
        throw 'A valid Authenticode signature and timestamp are both required.'
    }
}

$tarFiles = Get-ChildItem -LiteralPath $tarPackage -Recurse -File |
    Where-Object Extension -In '.exe', '.dll'
$tarAudit = foreach ($tarFile in $tarFiles) {
    $tarSignature = Get-AuthenticodeSignature -LiteralPath $tarFile.FullName
    [PSCustomObject]@{
        Path = $tarFile.FullName.Substring($tarPackage.Length + 1)
        Status = $tarSignature.Status.ToString()
        Publisher = $tarSignature.SignerCertificate.Subject
    }
}
$tarAudit | ConvertTo-Json -Depth 3
$tarFailures = @($tarAudit | Where-Object Status -NE 'Valid')
if ($tarFailures.Count -gt 0) {
    Write-Warning "$($tarFailures.Count) native binaries do not have valid signatures. This is not a verified signed release."
    if ($Sign) { throw 'Package signature audit failed.' }
}
if ($Sign) {
    Write-Host 'Native signatures and launcher timestamp verified. Create a new archive and re-test on Smart App Control before claiming compatibility.'
}
