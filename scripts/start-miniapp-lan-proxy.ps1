[CmdletBinding()]
param(
    [string]$WlanAddress = '172.25.96.19',

    [ValidateRange(1, 65535)]
    [int]$ListenPort = 18080,

    [ValidateRange(1, 65535)]
    [int]$UpstreamPort = 8080,

    [switch]$ForceRestart
)

$ErrorActionPreference = 'Stop'
$scriptDirectory = $PSScriptRoot
$proxyScript = Join-Path $scriptDirectory 'miniapp-lan-proxy.js'
$pidFile = Join-Path $scriptDirectory 'miniapp-lan-proxy.pid'

if (Test-Path $pidFile) {
    $existingPid = [int](Get-Content $pidFile -Raw)
    $existingProcess = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($existingProcess -and -not $ForceRestart) {
        Write-Host ('LAN proxy is already running. PID: {0}' -f $existingPid) -ForegroundColor Green
        exit 0
    }
    if ($existingProcess) {
        Stop-Process -Id $existingPid -Force
        Start-Sleep -Milliseconds 500
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $nodeCommand) {
    throw 'node.exe was not found. Install Node.js or add it to PATH.'
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $nodeCommand.Source
$startInfo.Arguments = ('"{0}" "{1}" {2} "127.0.0.1" {3}' -f $proxyScript, $WlanAddress, $ListenPort, $UpstreamPort)
$startInfo.WorkingDirectory = $scriptDirectory
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
    if (-not $errorText) { $errorText = $process.StandardOutput.ReadToEnd() }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    throw ('LAN proxy exited during startup. Exit code: {0}. {1}' -f $process.ExitCode, $errorText)
}

$test = Test-NetConnection $WlanAddress -Port $ListenPort -WarningAction SilentlyContinue
if (-not $test.TcpTestSucceeded) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    throw ('LAN proxy started but {0}:{1} is not reachable.' -f $WlanAddress, $ListenPort)
}

Write-Host ('LAN proxy started. PID: {0}' -f $process.Id) -ForegroundColor Green
Write-Host ('Listening: {0}:{1}' -f $WlanAddress, $ListenPort)
Write-Host ('Forwarding to: 127.0.0.1:{0}' -f $UpstreamPort)
