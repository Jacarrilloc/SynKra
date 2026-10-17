#!/usr/bin/env bash
# Instala las herramientas que usa SynKra en Ubuntu/Debian (nativo o WSL2).
# Idempotente: si una herramienta ya está en la versión pedida, la salta.
# Uso: ./scripts/setup-linux.sh
set -euo pipefail

KUBECTL_VERSION="${KUBECTL_VERSION:-v1.37.0}"
KIND_VERSION="${KIND_VERSION:-v0.33.0}"
NODE_MAJOR="${NODE_MAJOR:-22}"

case "$(uname -m)" in
  x86_64)        ARCH=amd64 ;;
  aarch64|arm64) ARCH=arm64 ;;
  *) echo "Arquitectura no soportada: $(uname -m)" >&2; exit 1 ;;
esac

if ! command -v apt-get >/dev/null; then
  echo "Este script requiere Ubuntu o Debian (apt-get)." >&2
  exit 1
fi

log() { printf '\n==> %s\n' "$*"; }
have() { command -v "$1" >/dev/null 2>&1; }

log "Paquetes base"
sudo apt-get update -y
sudo apt-get install -y git curl jq unzip make ca-certificates openjdk-21-jdk maven

if have docker; then
  log "Docker ya instalado: $(docker --version)"
else
  log "Docker Engine"
  curl -fsSL https://get.docker.com | sudo sh
fi
if ! id -nG "$USER" | grep -qw docker; then
  sudo usermod -aG docker "$USER"
  echo "Agregado $USER al grupo docker: cierra y abre la sesión (en WSL: 'wsl --shutdown')."
fi

if have kubectl && kubectl version --client 2>/dev/null | grep -q "$KUBECTL_VERSION"; then
  log "kubectl $KUBECTL_VERSION ya instalado"
else
  log "kubectl $KUBECTL_VERSION"
  tmp="$(mktemp -d)"
  curl -fsSLo "$tmp/kubectl" "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${ARCH}/kubectl"
  sudo install -m 0755 "$tmp/kubectl" /usr/local/bin/kubectl
  rm -rf "$tmp"
fi

if have kind && kind version | grep -q "$KIND_VERSION"; then
  log "kind $KIND_VERSION ya instalado"
else
  log "kind $KIND_VERSION"
  tmp="$(mktemp -d)"
  curl -fsSLo "$tmp/kind" "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-linux-${ARCH}"
  sudo install -m 0755 "$tmp/kind" /usr/local/bin/kind
  rm -rf "$tmp"
fi

if have helm; then
  log "Helm ya instalado: $(helm version --short)"
else
  log "Helm"
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi

if have node && [ "$(node -p 'process.versions.node.split(".")[0]')" -ge 20 ]; then
  log "Node ya instalado: $(node --version)"
else
  log "Node $NODE_MAJOR"
  curl -fsSL "https://deb.nodesource.com/setup_${NODE_MAJOR}.x" | sudo -E bash -
  sudo apt-get install -y nodejs
fi

if have claude; then
  log "Claude Code ya instalado"
else
  log "Claude Code (para el loop de agentes del módulo 6)"
  sudo npm install -g @anthropic-ai/claude-code
fi

log "Listo. Verificando:"
"$(dirname "$0")/doctor.sh"
