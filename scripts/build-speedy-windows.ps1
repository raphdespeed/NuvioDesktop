param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdk = $env:ANDROID_HOME,
    [string]$WebView2Sdk,
    [string]$WixDirectory
)
$ErrorActionPreference = 'Stop'
$desktopRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $JavaHome 'bin/jpackage.exe'))) {
    throw 'Set -JavaHome to a JDK 17 or newer containing jpackage.'
}
if ($WixDirectory) { $env:PATH = "$WixDirectory;$env:PATH" }
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdk
$arguments = @('-p', $desktopRoot, ':composeApp:createDistributable', ':composeApp:packageExe', '--no-daemon', '--max-workers=2', '--no-configuration-cache', '-Pkotlin.compiler.execution.strategy=in-process', '-Dorg.gradle.jvmargs=-Xmx10g -Dfile.encoding=UTF-8 -XX:MaxMetaspaceSize=2g')
if ($WebView2Sdk) { $arguments += "-Pnuvio.webview2.dir=$WebView2Sdk" }
& (Join-Path $desktopRoot 'gradlew.bat') @arguments
if ($LASTEXITCODE -ne 0) { throw "Windows build failed ($LASTEXITCODE)." }
$installer = Get-ChildItem (Join-Path $desktopRoot 'composeApp/build/compose/binaries/main/exe') -Filter '*.exe' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $installer) { throw 'The build did not produce an EXE installer.' }
$outputDirectory = Join-Path $desktopRoot 'output_windows'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$installerPath = Join-Path $outputDirectory 'Nuvio-Speedy-Windows-x64.exe'
Copy-Item -LiteralPath $installer.FullName -Destination $installerPath -Force
$checksum = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath (Join-Path $outputDirectory 'SHA256SUMS.txt') -Value "$checksum  Nuvio-Speedy-Windows-x64.exe" -Encoding ascii
Write-Output $installerPath
