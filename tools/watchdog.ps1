<#
.SYNOPSIS
    Aether TV Automated Watchdog Script (PowerShell Entrypoint)
.DESCRIPTION
    Monitors engineering progress, verifies git status and diffs, executes test suites,
    updates watchdog-state.json and PROGRESS.md, and detects stalling.
.PARAMETER RunTests
    If set, executes Gradle unit tests in launcher directory.
.PARAMETER TvHost
    IP address and port of Android TV target (defaults to 10.199.103.54:5555).
#>
param(
    [switch]$RunTests,
    [string]$TvHost = "10.199.103.54:5555"
)

$ErrorActionPreference = "Continue"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$launcherDir = Join-Path $repoRoot "launcher"
$watchdogPy = Join-Path $scriptDir "watchdog.py"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    AETHER TV AUTONOMOUS WATCHDOG      " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Run Python Watchdog Core
Write-Host "[1/2] Executing Watchdog Checkpoint..." -ForegroundColor Yellow
python $watchdogPy

# 2. Optional Unit Test Execution
if ($RunTests) {
    Write-Host "[2/2] Running Gradle unit tests in $launcherDir..." -ForegroundColor Yellow
    Push-Location $launcherDir
    $testRun = .\gradlew.bat testDebugUnitTest 2>&1 | Out-String
    Pop-Location
    if ($testRun -match "BUILD SUCCESSFUL") {
        Write-Host "Unit tests PASSED successfully." -ForegroundColor Green
    } else {
        Write-Host "Unit tests FAILED." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[2/2] Unit test execution skipped (pass -RunTests to run full test suite)." -ForegroundColor Gray
}

Write-Host "Watchdog check completed successfully." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
