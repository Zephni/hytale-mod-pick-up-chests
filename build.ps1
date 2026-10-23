param(
    [string]$ServerJar = "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar",
    [string]$AssetsZip,
    [switch]$Test
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $ServerJar -PathType Leaf)) {
    throw "HytaleServer.jar not found. Supply -ServerJar with the path to your Hytale 0.7.4 server."
}
$ServerJar = (Resolve-Path -LiteralPath $ServerJar).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($ServerJar)
try {
    $entry = $archive.GetEntry("META-INF/MANIFEST.MF")
    if (-not $entry) { throw "The server JAR has no version manifest." }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { $serverManifest = $reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($serverManifest -notmatch '(?m)^Implementation-Version: 0\.7\.4\r?$') {
        throw "This mod targets Hytale 0.7.4. Supply a 0.7.4 HytaleServer.jar."
    }
} finally {
    $archive.Dispose()
}
foreach ($tool in "java", "javac", "jar") {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Install a JDK 25 and put its bin directory on PATH (missing: $tool)."
    }
}

$build = Join-Path $PSScriptRoot "build"
$classes = Join-Path $build "classes"
$null = New-Item -ItemType Directory -Force -Path $classes
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot "src\main\java") -Recurse -Filter "*.java" |
    ForEach-Object FullName)
& javac --release 25 -Xlint:all -classpath $ServerJar -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }
Copy-Item (Join-Path $PSScriptRoot "src\main\resources\*") $classes -Recurse -Force
& java (Join-Path $PSScriptRoot "tools\GenerateFilledChestIcon.java") `
    (Join-Path $classes "Common\Icons\PickUpChests\FilledChest.png")
if ($LASTEXITCODE -ne 0) { throw "Filled chest icon generation failed." }
$output = Join-Path $build "PickUpChests-1.0.0.jar"
& jar --create --file $output -C $classes .
if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed." }

if ($Test) {
    $testClasses = Join-Path $build "test-classes"
    $null = New-Item -ItemType Directory -Force -Path $testClasses
    $tests = @(Get-ChildItem (Join-Path $PSScriptRoot "src\test\java") -Recurse -Filter "*.java" |
        ForEach-Object FullName)
    & javac --release 25 -classpath "$ServerJar;$classes" -d $testClasses @tests
    if ($LASTEXITCODE -ne 0) { throw "Test compilation failed." }
    if (-not $AssetsZip) {
        $AssetsZip = Join-Path (Split-Path (Split-Path $ServerJar -Parent) -Parent) "Assets.zip"
    }
    if (-not (Test-Path -LiteralPath $AssetsZip -PathType Leaf)) {
        throw "Assets.zip not found. Supply -AssetsZip with the path to your installed game assets."
    }
    $AssetsZip = (Resolve-Path -LiteralPath $AssetsZip).Path
    $testServer = Join-Path $build "test-server"
    $testMods = Join-Path $testServer "mods"
    $null = New-Item -ItemType Directory -Force -Path $testMods
    Copy-Item "$classes\*" $testClasses -Recurse -Force
    Copy-Item (Join-Path $PSScriptRoot "src\test\resources\manifest.json") "$testClasses\manifest.json" -Force
    & jar --create --file "$testMods\PickUpChests-Test.jar" -C $testClasses .
    if ($LASTEXITCODE -ne 0) { throw "Test JAR packaging failed." }
    $result = Join-Path $testServer "result.txt"
    if (Test-Path -LiteralPath $result) { Remove-Item -LiteralPath $result }
    Push-Location $testServer
    try {
        & java --enable-native-access=ALL-UNNAMED -Xmx2G -ea "-Dpickupchests.testResult=$result" -jar $ServerJar `
            --assets $AssetsZip --bind 127.0.0.1:0 --auth-mode offline --disable-sentry --disable-file-watcher `
            --disable-cpb-build --boot-command stop *> "test-server.log"
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $result)) {
            throw "Native server tests failed. See $testServer\test-server.log."
        }
        Get-Content -LiteralPath $result
    } finally {
        Pop-Location
    }
}

Write-Host "Built $output"

if (-not $env:APPDATA) {
    throw "APPDATA is not set; cannot locate the local Hytale Mods directory."
}
$mods = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$null = New-Item -ItemType Directory -Force -Path $mods
$installed = Join-Path $mods (Split-Path $output -Leaf)
Copy-Item -LiteralPath $output -Destination $installed -Force
Write-Host "Installed $installed"
