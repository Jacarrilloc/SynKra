# Prepara SynKra en Windows: todo se construye y prueba dentro de WSL2 (Ubuntu),
# así Windows y Linux usan exactamente los mismos scripts.
# Uso (PowerShell):  .\scripts\setup-windows.ps1
$ErrorActionPreference = 'Stop'
$Distro = 'Ubuntu'
$Repo   = 'https://github.com/Jacarrilloc/SynKra.git'

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    Write-Host 'WSL no está instalado. Ejecuta como administrador:  wsl --install -d Ubuntu-24.04  y reinicia.'
    exit 1
}

$distros = (wsl.exe -l -q) -replace "`0", '' | Where-Object { $_ -ne '' }
if ($distros -notcontains $Distro) {
    Write-Host "No existe la distro '$Distro'. Instálala con:  wsl --install -d Ubuntu-24.04"
    exit 1
}

# El repo vive en el disco de Linux (~/proyectos/SynKra), no en OneDrive: /mnt/c es lento y rompe worktrees.
$script = @"
set -e
mkdir -p ~/proyectos
if [ ! -d ~/proyectos/SynKra/.git ]; then git clone $Repo ~/proyectos/SynKra; fi
cd ~/proyectos/SynKra
git pull --ff-only || true
chmod +x scripts/*.sh
./scripts/setup-linux.sh
"@
wsl.exe -d $Distro -- bash -lc ($script -replace "`r", '')

Write-Host ''
Write-Host 'Para trabajar:  wsl -d Ubuntu   y luego   cd ~/proyectos/SynKra && make doctor'
Write-Host 'En VS Code:     code ~/proyectos/SynKra  (desde la terminal de Ubuntu, con la extensión WSL)'
