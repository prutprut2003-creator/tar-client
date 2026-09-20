$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType=WindowsRuntime]
function Await-TarOperation($Operation, $ResultType) {
    $Method = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
    $Task = $Method.MakeGenericMethod($ResultType).Invoke($null, @($Operation))
    if (-not $Task.Wait(5000)) { throw 'Windows media session timed out' }
    return $Task.Result
}
try {
    $Manager = Await-TarOperation ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
    $Session = $Manager.GetSessions() | Where-Object { $_.SourceAppUserModelId -match '(?i)spotify' } | Select-Object -First 1
    if ($null -eq $Session) { '{"available":false,"status":"Open Spotify and play a song"}'; exit 0 }
    $Props = Await-TarOperation ($Session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
    $Art = ''
    if ($null -ne $Props.Thumbnail) {
        $Stream = Await-TarOperation ($Props.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
        try {
            if ($Stream.Size -gt 0 -and $Stream.Size -le 1048576) {
                $Reader = [Windows.Storage.Streams.DataReader]::new($Stream)
                try { $null = Await-TarOperation ($Reader.LoadAsync([uint32]$Stream.Size)) ([uint32]); $Bytes = New-Object byte[] ([int]$Stream.Size); $Reader.ReadBytes($Bytes); $Art = [Convert]::ToBase64String($Bytes) } finally { $Reader.Dispose() }
            }
        } finally { $Stream.Dispose() }
    }
    @{ available=$true; title=$Props.Title; artist=$Props.Artist; playing=($Session.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing'); artwork=$Art } | ConvertTo-Json -Compress
} catch {
    '{"available":false,"status":"Windows media information unavailable"}'
}
