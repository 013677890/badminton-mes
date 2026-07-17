<#
.SYNOPSIS
  通过前端 Nginx 代理检查管理端全部只读 API 路径。

.DESCRIPTION
  从 frontend-web/src/api 自动提取 get(...) 调用，登录后逐条请求。
  路径变量以 1 代替、分页参数使用 pageNo=1&pageSize=10；4xx 或业务错误码表示
  参数/数据校验已生效，不计为路由故障，只有网络错误或 5xx 才判定失败。

  写接口不在本脚本中调用，避免烟测产生业务数据；由 Gradle 完整测试套件负责回归。
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost',
    [string]$UserNo = 'admin',
    [string]$Password = 'admin123',
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$apiRoot = Join-Path $projectRoot 'frontend-web/src/api'
$credentialsJson = @{ userNo = $UserNo; password = $Password } | ConvertTo-Json -Compress
$loginJson = & curl.exe -sS --max-time $TimeoutSeconds -X POST "$BaseUrl/api/system/auth/login" `
    -H 'Content-Type: application/json' --data $credentialsJson
$login = $loginJson | ConvertFrom-Json
if ($login.code -ne '00000' -or [string]::IsNullOrWhiteSpace($login.data.token)) {
    throw "登录失败：$($login.message)"
}

$getCallPattern = '(?s)\bget(?:<[^>]+>)?\(\s*([`"''])(?<path>.*?)\1'
$paths = foreach ($file in Get-ChildItem -Path $apiRoot -Recurse -Filter '*.ts') {
    $content = Get-Content -Raw $file.FullName
    foreach ($match in [regex]::Matches($content, $getCallPattern)) {
        $path = $match.Groups['path'].Value
        if ($path.StartsWith('/')) {
            [regex]::Replace($path, '\$\{[^}]+\}', '1')
        }
    }
}
$paths = $paths | Sort-Object -Unique
$authorization = "Authorization: Bearer $($login.data.token)"
$results = foreach ($path in $paths) {
    $uri = "$BaseUrl/api$path"
    $separator = if ($uri.Contains('?')) { '&' } else { '?' }
    $uri = "$uri${separator}pageNo=1&pageSize=10"
    $status = (& curl.exe -sS --max-time $TimeoutSeconds -o NUL -w '%{http_code}' -H $authorization $uri).Trim()
    [pscustomobject]@{
        Path = $path
        HttpStatus = if ($status -match '^\d{3}$') { [int]$status } else { 0 }
        Passed = $status -match '^[234]\d\d$'
    }
}

$failed = @($results | Where-Object { -not $_.Passed })
[pscustomobject]@{
    Total = @($results).Count
    Passed = @($results | Where-Object Passed).Count
    Failed = $failed.Count
    FailedPaths = @($failed | Select-Object -ExpandProperty Path)
} | ConvertTo-Json -Depth 3

if ($failed.Count -gt 0) {
    exit 1
}
