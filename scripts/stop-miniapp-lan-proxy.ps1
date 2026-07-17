[CmdletBinding()]
param()

$pidFile = Join-Path $PSScriptRoot 'miniapp-lan-proxy.pid'
if (-not (Test-Path $pidFile)) {
    Write-Host 'LAN proxy is not running.'
    exit 0
}

$proxyPid = [int](Get-Content $pidFile -Raw)
Stop-Process -Id $proxyPid -Force -ErrorAction SilentlyContinue
Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
Write-Host ('LAN proxy stopped. PID: {0}' -f $proxyPid) -ForegroundColor Green
