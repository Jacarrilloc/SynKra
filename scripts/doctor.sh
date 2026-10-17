#!/usr/bin/env bash
# Verifica que estén las herramientas que usa SynKra. Sale con 1 si falta alguna.
# Uso: ./scripts/doctor.sh
set -uo pipefail

missing=0
check() {
  local name="$1"; shift
  if command -v "$name" >/dev/null 2>&1; then
    printf '  [ok]    %-8s %s\n' "$name" "$("$@" 2>&1 | head -n1)"
  else
    printf '  [falta] %s\n' "$name"
    missing=1
  fi
}

echo "Herramientas:"
check git     git --version
check docker  docker --version
check kubectl kubectl version --client
check kind    kind version
check helm    helm version --short
check jq      jq --version
check make    make --version
check java    java -version
check mvn     mvn -v
check node    node --version
check claude  claude --version

echo "Docker daemon:"
if docker info >/dev/null 2>&1; then
  echo "  [ok]    responde"
else
  echo "  [falla] no responde (¿está iniciado? ¿tu usuario está en el grupo docker?)"
  missing=1
fi

case "$(pwd)" in
  /mnt/*) echo "Aviso: el repo está en /mnt/... (disco de Windows). Clónalo en ~/ para que Docker y git worktree vayan rápido." ;;
esac

exit "$missing"
