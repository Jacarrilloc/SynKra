# Cabecera común de la evidencia de U09-C: fecha, sha del último commit que toca código, arquitectura y digest de la imagen.
cabecera() {
  echo "fecha=$(date -u +%FT%TZ)"
  echo "sha-codigo=$(git log -1 --format=%h -- front front.Tests) (HEAD=$(git rev-parse --short HEAD))"
  echo "uname -m=$(uname -m)"
  echo "RepoDigests=$(docker image inspect --format '{{index .RepoDigests 0}}' mcr.microsoft.com/dotnet/sdk:10.0.404)"
  echo
}
# Corre un comando mostrándolo antes. Uso: orden <comando...>
orden() { echo "\$ $*"; "$@"; echo "[salida=$?]"; echo; }
