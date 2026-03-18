param(
    [string]$InputPath = "src/main/resources/data/luckybreak/structure/wishing_well.snbt",
    [string]$OutputPath = "src/main/resources/data/luckybreak/structure/wishing_well.nbt"
)

$ErrorActionPreference = "Stop"

function Resolve-Python {
    $localPython = Join-Path $env:LocalAppData "Programs\Python\Python312\python.exe"
    if (Test-Path $localPython) {
        & $localPython --version *> $null
        if ($LASTEXITCODE -eq 0) {
            return @{ Exe = $localPython; Prefix = @() }
        }
    }

    $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
    if ($pyLauncher) {
        & $pyLauncher.Source -3 --version *> $null
        if ($LASTEXITCODE -eq 0) {
            return @{ Exe = $pyLauncher.Source; Prefix = @("-3") }
        }
    }

    $pythonFromPath = Get-Command python -ErrorAction SilentlyContinue
    if ($pythonFromPath) {
        & $pythonFromPath.Source --version *> $null
        if ($LASTEXITCODE -eq 0) {
            return @{ Exe = $pythonFromPath.Source; Prefix = @() }
        }
    }

    throw "Python was not found. Install Python 3.12 first."
}

$python = Resolve-Python
$inputFull = Join-Path (Get-Location) $InputPath
$outputFull = Join-Path (Get-Location) $OutputPath

if (-not (Test-Path $inputFull)) {
    throw "Input file not found: $inputFull"
}

$args = @()
$args += $python.Prefix
$args += @("-m", "pip", "install", "nbtlib", "--quiet")
& $python.Exe @args
if ($LASTEXITCODE -ne 0) {
    throw "Failed to install or access nbtlib with Python."
}

$convertArgs = @()
$convertArgs += $python.Prefix
$convertArgs += @(
    "-c",
    "from pathlib import Path; import nbtlib; src=Path(r'$inputFull'); dst=Path(r'$outputFull'); root=nbtlib.parse_nbt(src.read_text(encoding='utf-8')); nbtlib.File(root).save(str(dst), gzipped=True); print(dst)"
)
& $python.Exe @convertArgs
if ($LASTEXITCODE -ne 0) {
    throw "Failed to convert SNBT to NBT."
}

Write-Host "Converted SNBT to NBT: $OutputPath"
