# Aether TV — Mi Box 4 System State Backup Script
# Automatically pulls active launcher, installed package lists, and system settings for full restore safety.
param(
    [string]$DeviceIp = "10.199.103.54:5555",
    [string]$BackupDir = "$PSScriptRoot\..\launcher\backups\device_state"
)

Write-Host "=== Aether TV: Mi Box 4 Safety Backup Utility ===" -ForegroundColor Cyan
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

Write-Host "[1/5] Checking ADB connection to $DeviceIp..."
adb connect $DeviceIp

$devicesOutput = (adb devices) | Out-String
if ($devicesOutput -notmatch "10\.199\.103\.54:5555\s+device") {
    Write-Warning "Device $DeviceIp is not connected in 'device' mode. Current devices: $devicesOutput"
    exit 0
}

Write-Host "[2/5] Querying current default HOME launcher..." -ForegroundColor Green
$homePackage = adb shell "cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME" | Out-String
$homePackage | Out-File -FilePath "$BackupDir\active_home_launcher.txt" -Encoding utf8
Write-Host "Saved current active HOME resolver."

Write-Host "[3/5] Backing up package lists..." -ForegroundColor Green
adb shell "pm list packages" | Out-File -FilePath "$BackupDir\all_packages.txt" -Encoding utf8
adb shell "pm list packages -d" | Out-File -FilePath "$BackupDir\disabled_packages.txt" -Encoding utf8
adb shell "pm list packages -e" | Out-File -FilePath "$BackupDir\enabled_packages.txt" -Encoding utf8
adb shell "pm list packages -3" | Out-File -FilePath "$BackupDir\third_party_packages.txt" -Encoding utf8

Write-Host "[4/5] Backing up system settings & accessibility services..." -ForegroundColor Green
adb shell "settings get secure enabled_accessibility_services" | Out-File -FilePath "$BackupDir\enabled_accessibility_services.txt" -Encoding utf8
adb shell "settings get global private_dns_mode" | Out-File -FilePath "$BackupDir\private_dns_mode.txt" -Encoding utf8
adb shell "settings get global private_dns_specifier" | Out-File -FilePath "$BackupDir\private_dns_specifier.txt" -Encoding utf8

Write-Host "[5/5] Backing up device properties (build fingerprint, display size, ABI)..." -ForegroundColor Green
adb shell "getprop" | Out-File -FilePath "$BackupDir\getprop_dump.txt" -Encoding utf8

Write-Host "=== Backup successfully completed to $BackupDir ===" -ForegroundColor Green
