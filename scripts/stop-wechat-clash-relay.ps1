[CmdletBinding()]
param()

$pidFile = Join-Path $PSScriptRoot 'wechat-clash-relay.pid'
if (-not (Test-Path $pidFile)) {
    Write-Host 'WeChat Clash relay is not running.'
    exit 0
}

$relayPid = [int](Get-Content $pidFile -Raw)
Stop-Process -Id $relayPid -Force -ErrorAction SilentlyContinue
Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
Write-Host ("WeChat Clash relay stopped. PID: {0}" -f $relayPid) -ForegroundColor Green
