[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$ListenPort = 18080,

    [ValidateRange(1, 65535)]
    [int]$UpstreamPort = 8080,

    [string]$RuleName = 'Badminton MES MiniApp LAN 18080',

    [string]$WlanAddress = '172.25.96.19'
)

$ErrorActionPreference = 'Stop'

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-IsAdministrator)) {
    $argumentList = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"{0}"' -f $PSCommandPath),
        '-ListenPort', $ListenPort,
        '-UpstreamPort', $UpstreamPort,
        '-RuleName', ('"{0}"' -f $RuleName),
        '-WlanAddress', $WlanAddress
    )
    Start-Process -FilePath powershell.exe -ArgumentList $argumentList -Verb RunAs -Wait
    exit $LASTEXITCODE
}

Write-Host 'Removing obsolete Windows portproxy entry...' -ForegroundColor Cyan
& netsh interface portproxy delete v4tov4 listenaddress=$WlanAddress listenport=$UpstreamPort protocol=tcp | Out-Null
& netsh interface portproxy delete v4tov4 listenaddress=$WlanAddress listenport=$ListenPort protocol=tcp | Out-Null

Write-Host 'Recreating the scoped firewall rule...' -ForegroundColor Cyan
Get-NetFirewallRule -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName.Trim('"') -eq $RuleName } |
    Remove-NetFirewallRule

New-NetFirewallRule `
    -DisplayName $RuleName `
    -Description 'Allow local-subnet access to the Badminton MES LAN proxy.' `
    -Direction Inbound `
    -Action Allow `
    -Enabled True `
    -Profile Private,Public `
    -Protocol TCP `
    -LocalPort $ListenPort `
    -RemoteAddress LocalSubnet | Out-Null

Write-Host 'Starting the Clash-compatible user-space LAN proxy...' -ForegroundColor Cyan
$startScript = Join-Path $PSScriptRoot 'start-miniapp-lan-proxy.ps1'
& $startScript -WlanAddress $WlanAddress -ListenPort $ListenPort -UpstreamPort $UpstreamPort -ForceRestart

$response = Invoke-WebRequest -UseBasicParsing -Uri ('http://{0}:{1}/api/system/auth/registration_roles' -f $WlanAddress, $ListenPort) -TimeoutSec 10
Write-Host ('HTTP status: {0}' -f $response.StatusCode) -ForegroundColor Green
Write-Host 'Clash was not stopped or modified.' -ForegroundColor Green
