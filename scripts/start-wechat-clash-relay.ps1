[CmdletBinding()]
param(
    [string]$ListenAddress = '0.0.0.0',

    [ValidateRange(1, 65535)]
    [int]$ListenPort = 17897,

    [string]$ClashAddress = '127.0.0.1',

    [ValidateRange(1, 65535)]
    [int]$ClashPort = 7897,

    [switch]$ForceRestart
)

$ErrorActionPreference = 'Stop'
$proxyScript = Join-Path $PSScriptRoot 'miniapp-lan-proxy.js'
$pidFile = Join-Path $PSScriptRoot 'wechat-clash-relay.pid'

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMilliseconds = 1000
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne($TimeoutMilliseconds)) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

if (-not (Test-Path $proxyScript)) {
    throw "TCP relay script was not found: $proxyScript"
}

if (-not (Test-TcpPort -HostName $ClashAddress -Port $ClashPort)) {
    throw "Clash proxy is not listening at ${ClashAddress}:${ClashPort}. Check the Clash mixed-port setting."
}

if (Test-Path $pidFile) {
    $existingPid = [int](Get-Content $pidFile -Raw)
    $existingProcess = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($existingProcess -and -not $ForceRestart) {
        Write-Host ("WeChat Clash relay is already running. PID: {0}" -f $existingPid) -ForegroundColor Green
        exit 0
    }
    if ($existingProcess) {
        Stop-Process -Id $existingPid -Force
        Start-Sleep -Milliseconds 500
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

if (Test-TcpPort -HostName '127.0.0.1' -Port $ListenPort) {
    throw "Port $ListenPort is already in use and is not managed by this relay script."
}

$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $nodeCommand) {
    throw 'node.exe was not found. Install Node.js or add it to PATH.'
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $nodeCommand.Source
$startInfo.Arguments = ('"{0}" "{1}" {2} "{3}" {4}' -f `
        $proxyScript, $ListenAddress, $ListenPort, $ClashAddress, $ClashPort)
$startInfo.WorkingDirectory = $PSScriptRoot
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
$process = [System.Diagnostics.Process]::Start($startInfo)

Set-Content -Path $pidFile -Value $process.Id -Encoding ASCII
Start-Sleep -Seconds 2

if ($process.HasExited) {
    $errorText = $process.StandardError.ReadToEnd()
    if (-not $errorText) {
        $errorText = $process.StandardOutput.ReadToEnd()
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    throw ("WeChat Clash relay exited during startup. Exit code: {0}. {1}" -f `
            $process.ExitCode, $errorText)
}

if (-not (Test-TcpPort -HostName '127.0.0.1' -Port $ListenPort)) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    throw "WeChat Clash relay started but port $ListenPort is not reachable."
}

Write-Host ("WeChat Clash relay started. PID: {0}" -f $process.Id) -ForegroundColor Green
Write-Host ("Docker endpoint: host.docker.internal:{0}" -f $ListenPort)
Write-Host ("Forwarding to Clash: {0}:{1}" -f $ClashAddress, $ClashPort)
Write-Host 'Clash was not stopped or modified.' -ForegroundColor Green
