param(
    [string] $Version = "0.1.2",
    [string] $OutputDirectory = "dist\modrinth"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$instancesRoot = Resolve-Path (Join-Path $repoRoot "..\..\..")
$outputPath = Join-Path $repoRoot $OutputDirectory

New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$sources = @(
    @{
        Minecraft = "1.21.11"
        Path = Join-Path $instancesRoot "Uncharted Odyssey - Test\minecraft\mods\uncharted-upgrades-$Version.jar"
    },
    @{
        Minecraft = "26.1.2"
        Path = Join-Path $instancesRoot "26.1.2\minecraft\mods\uncharted-upgrades-$Version.jar"
    },
    @{
        Minecraft = "26.2"
        Path = Join-Path $instancesRoot "26.2\minecraft\mods\uncharted-upgrades-$Version.jar"
    }
)

foreach ($source in $sources) {
    if (!(Test-Path -LiteralPath $source.Path)) {
        throw "Missing jar for Minecraft $($source.Minecraft): $($source.Path)"
    }

    $target = Join-Path $outputPath "uncharted-upgrades-$Version+$($source.Minecraft).jar"
    Copy-Item -LiteralPath $source.Path -Destination $target -Force
    Write-Host "Prepared $target"
}
