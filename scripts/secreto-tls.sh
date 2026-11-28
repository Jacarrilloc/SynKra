#!/usr/bin/env bash
# Crea (o actualiza) el secreto TLS synkra/synkra-local-tls con el certificado de .tls/ (lo genera si no existe).
# Uso: KUBECONFIG_SYNKRA=<kubeconfig de kind-synkra> bash scripts/secreto-tls.sh   (idempotente; la clave nunca se imprime)
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
KC="${KUBECONFIG_SYNKRA:-$RAIZ/.kube/kind-synkra.yaml}"

TMP=""
limpiar() { [ -z "$TMP" ] || rm -rf "$TMP"; }
trap limpiar EXIT                  # registrado antes de crear el temporal
TMP="$(mktemp -d)"

mkdir -p -m 0700 .tls && chmod 0700 .tls
if [ -e .tls/tls.crt ] && [ -e .tls/tls.key ]; then
  echo "tls: se reutiliza .tls/tls.crt"
elif [ -e .tls/tls.crt ] || [ -e .tls/tls.key ]; then
  solo="tls.crt"; [ -e .tls/tls.key ] && solo="tls.key"
  echo "FALLA tls: .tls tiene solo $solo; no se regenera ni se borra" >&2
  exit 1
else
  bash scripts/certificado-tls.sh .tls
fi

rc=0
( umask 077; kubectl --kubeconfig "$KC" create secret tls synkra-local-tls -n synkra \
    --cert .tls/tls.crt --key .tls/tls.key --dry-run=client -o yaml > "$TMP/secreto.yaml" ) || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA tls: kubectl create secret tls --dry-run=client salió con $rc" >&2; exit 1; }
chmod 0600 "$TMP/secreto.yaml"
rc=0
kubectl --kubeconfig "$KC" apply -f "$TMP/secreto.yaml" || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA tls: kubectl apply del secreto salió con $rc" >&2; exit 1; }

# Lectura de vuelta.
rc=0
TIPO="$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.type}')" || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA tls: kubectl get secret salió con $rc" >&2; exit 1; }
[ "$TIPO" = "kubernetes.io/tls" ] || { echo "FALLA tls: el tipo es '$TIPO' y se esperaba kubernetes.io/tls" >&2; exit 1; }
rc=0
EN_CLUSTER="$(kubectl --kubeconfig "$KC" -n synkra get secret synkra-local-tls -o jsonpath='{.data.tls\.crt}' | base64 -d | sha256sum | cut -d' ' -f1)" || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA tls: no se pudo leer tls.crt del secreto (código $rc)" >&2; exit 1; }
LOCAL="$(sha256sum < .tls/tls.crt | cut -d' ' -f1)"
[ "$EN_CLUSTER" = "$LOCAL" ] || { echo "FALLA tls: el certificado del secreto ($EN_CLUSTER) no es el de .tls/tls.crt ($LOCAL)" >&2; exit 1; }
echo "OK tls: synkra/synkra-local-tls $LOCAL"
