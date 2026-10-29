<#
    Builds the API and (re)starts it from backend\run.

    Why: on Windows an executing .jar is locked, so `mvn clean install`
    fails with "The process cannot access the file ... being used by
    another process" whenever the app is started from target\. Starting
    the app from run\ keeps backend\target free so Maven can rebuild at
    any time, even while the API is running.

    Usage:
        .\run-backend.ps1              # build, copy, stop old instance, start
        .\run-backend.ps1 -NoBuild     # restart the last built jar
        .\run-backend.ps1 -Port 8081   # stop/verify a different port
#>
[CmdletBinding()]
param(
    [switch]$NoBuild,
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$runDir = Join-Path $root 'run'
$builtJar = Join-Path $root 'target\nursery-api-0.0.1-SNAPSHOT.jar'
$runJar = Join-Path $runDir 'nursery-api.jar'

Push-Location $root
try {
    if (-not $NoBuild) {
        Write-Host 'Building backend...' -ForegroundColor Cyan
        & .\mvnw.cmd clean install
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed (exit code $LASTEXITCODE). Nothing was restarted."
        }
    }

    if (-not (Test-Path -LiteralPath $builtJar)) {
        throw "Jar not found: $builtJar. Run without -NoBuild to build it first."
    }

    New-Item -ItemType Directory -Force -Path $runDir | Out-Null
    Copy-Item -LiteralPath $builtJar -Destination $runJar -Force

    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        $runningId = $listener.OwningProcess
        Write-Host "Stopping process $runningId on port $Port..." -ForegroundColor Yellow
        Stop-Process -Id $runningId -Force
        Start-Sleep -Seconds 2
    }
}
finally {
    Pop-Location
}

Write-Host "Starting $runJar (Ctrl+C to stop)..." -ForegroundColor Cyan
& java -jar $runJar
exit $LASTEXITCODE
