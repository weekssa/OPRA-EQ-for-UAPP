$ErrorActionPreference = "Stop"

$alias = "opra-eq-for-uapp-release"
$outputDirectory = Join-Path $HOME "OPRA-EQ-release-signing"
$keystorePath = Join-Path $outputDirectory "opra-eq-for-uapp-release.p12"
$base64Path = Join-Path $outputDirectory "opra-eq-for-uapp-release.p12.base64.txt"

function Find-Keytool {
    $command = Get-Command keytool -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr\bin\keytool.exe"),
        (Join-Path $env:ProgramFiles "Android\Android Studio\jre\bin\keytool.exe"),
        (if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\keytool.exe" } else { $null })
    ) | Where-Object { $_ -and (Test-Path $_) }

    if ($candidates.Count -gt 0) {
        return $candidates[0]
    }

    throw "keytool was not found. Install Android Studio or a JDK 17+ and run this script again."
}

$keytool = Find-Keytool

if (Test-Path $keystorePath) {
    throw "Refusing to overwrite the existing release keystore at $keystorePath. Preserve that file as the permanent signing identity."
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

Write-Host ""
Write-Host "Creating the permanent OPRA EQ for UAPP GitHub release-signing key." -ForegroundColor Cyan
Write-Host "The password prompt comes from keytool and stays on this computer."
Write-Host "Choose a strong unique password and store it in your password manager."
Write-Host "Do not post the password, keystore, or Base64 file in GitHub Issues or chat."
Write-Host ""

& $keytool \
    -genkeypair \
    -v \
    -keystore $keystorePath \
    -storetype PKCS12 \
    -alias $alias \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -dname "CN=OPRA EQ for UAPP,O=weekssa"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed while generating the release keystore."
}

$bytes = [System.IO.File]::ReadAllBytes($keystorePath)
[System.Convert]::ToBase64String($bytes) | Set-Content -NoNewline -Encoding ascii $base64Path

Write-Host ""
Write-Host "Keystore created successfully." -ForegroundColor Green
Write-Host "Private keystore: $keystorePath"
Write-Host "Base64 copy for GitHub Actions secret: $base64Path"
Write-Host "Key alias: $alias"
Write-Host ""
Write-Host "Back up the .p12 file in at least two secure locations before publishing any APK." -ForegroundColor Yellow
Write-Host "The Base64 file is just another representation of the same private key and must also be treated as secret." -ForegroundColor Yellow
Write-Host ""
Write-Host "Next, keytool will ask for the keystore password again and display the certificate fingerprints."
Write-Host "The SHA-256 certificate fingerprint is PUBLIC information; copy only that fingerprint for the release record."
Write-Host ""

& $keytool -list -v -keystore $keystorePath -alias $alias

if ($LASTEXITCODE -ne 0) {
    throw "The keystore was created, but keytool could not display its certificate details. Keep the keystore and rerun keytool -list later."
}

Write-Host ""
Write-Host "Signing identity generation is complete." -ForegroundColor Green
