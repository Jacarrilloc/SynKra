#!/usr/bin/env bash
# Primera capa del limite (H-13, H-27): lo que la cuenta deployer puede y no puede hacer. Solo pregunta; no crea nada.
# Uso: KUBECONFIG_SYNKRA=<kubeconfig de kind-synkra> bash scripts/verificar-limite.sh
set -euo pipefail

KC="${KUBECONFIG_SYNKRA:-$PWD/.kube/kind-synkra.yaml}"

# pregunta | esperado (la unica lista de preguntas: el Makefile no la repite)
PREGUNTAS=(
  "create deployments.apps -n synkra-co|yes"
  "create deployments.apps -n synkra-pe|yes"
  "create deployments.apps -n default|no"
  "create deployments.apps -n kube-system|no"
  "create deployments.apps -n synkra|no"
  "create namespaces|no"
)

MAL=0
for entrada in "${PREGUNTAS[@]}"; do
  pregunta="${entrada%|*}"; esperado="${entrada##*|}"
  rc=0
  # shellcheck disable=SC2086  # la pregunta son varias palabras a proposito
  respuesta="$(kubectl --kubeconfig "$KC" auth can-i $pregunta --as=system:serviceaccount:synkra:deployer 2>/dev/null)" || rc=$?
  # can-i sale con 0 para yes y con 1 para no: se lee la salida, no el codigo.
  if [ "$rc" -gt 1 ] || { [ "$respuesta" != "yes" ] && [ "$respuesta" != "no" ]; }; then
    echo "FALLA $pregunta: kubectl auth can-i salió con $rc"
    MAL=$((MAL + 1))
  elif [ "$respuesta" = "$esperado" ]; then
    echo "ok $pregunta: $respuesta"
  else
    echo "FALLA $pregunta: $respuesta (esperado $esperado)"
    MAL=$((MAL + 1))
  fi
done

if [ "$MAL" -ne 0 ]; then
  echo "FALLA limite: $MAL respuestas no esperadas"
  exit 1
fi
echo "OK limite: ${#PREGUNTAS[@]} respuestas esperadas"
