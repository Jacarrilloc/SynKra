#!/usr/bin/env bash
# Despliega el overlay de kind en kind-synkra y comprueba que el pod corre el digest que hay en el registro.
# Uso: KUBECONFIG_SYNKRA=<kubeconfig de kind-synkra> bash scripts/desplegar.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"
KC="${KUBECONFIG_SYNKRA:-$RAIZ/.kube/kind-synkra.yaml}"
ACEPTA='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
k() { kubectl --kubeconfig "$KC" "$@"; }

# 1. El webhook de admision de Ingress NGINX tiene que tener un endpoint listo (sin reintentos de apply).
LISTO=0
LIMITE=$((SECONDS + 60))
while [ "$SECONDS" -lt "$LIMITE" ]; do
  listos="$(k get endpointslices -n ingress-nginx -l kubernetes.io/service-name=ingress-nginx-controller-admission -o jsonpath='{.items[*].endpoints[*].conditions.ready}' 2>/dev/null || true)"
  case "$listos" in *true*) LISTO=1; break ;; esac
  sleep 2
done
[ "$LISTO" -eq 1 ] || { echo "FALLA desplegar: el webhook de Ingress NGINX no tiene endpoint" >&2; exit 1; }

# 2. Validacion en el servidor (sustituye a kubeconform) y apply.
rc=0; k apply --dry-run=server -k deploy/overlays/kind || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA desplegar: kubectl apply --dry-run=server salió con $rc" >&2; exit 1; }
rc=0; k apply -k deploy/overlays/kind || rc=$?
[ "$rc" -eq 0 ] || { echo "FALLA desplegar: kubectl apply -k salió con $rc" >&2; exit 1; }

# 3 y 4, para cada Deployment de SynKra en synkra (la lista sale del render del overlay, no se escribe aqui).
RENDER="$(k kustomize deploy/overlays/kind)" || { echo "FALLA desplegar: kubectl kustomize salió con $?" >&2; exit 1; }
DEPLOYMENTS="$(printf '%s' "$RENDER" | python3 -I -c "import sys,yaml; print(' '.join(sorted(o['metadata']['name'] for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment' and o['metadata'].get('namespace')=='synkra')))")" \
  || { echo "FALLA desplegar: no se pudo leer la lista de Deployments del overlay" >&2; exit 1; }
[ -n "$DEPLOYMENTS" ] || { echo "FALLA desplegar: el overlay no tiene Deployments en synkra" >&2; exit 1; }

# 3. Espera (300 s: el backend por su startupProbe; el front, el mismo tope).
esperar() { # deployment
  local rc=0
  k -n synkra rollout status "deployment/$1" --timeout=300s || rc=$?
  [ "$rc" -eq 0 ] || { echo "FALLA desplegar: rollout status de $1 salió con $rc" >&2; exit 1; }
}

# 4. Lo que corre es lo que se subio. La imagen sale del render del overlay; nada se escribe a mano.
for D in $DEPLOYMENTS; do
  esperar "$D"
  rc=0
  IMAGEN="$(printf '%s' "$RENDER" | DEPLOY="$D" python3 -I -c "import os,sys,yaml; print([o for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment' and o['metadata']['name']==os.environ['DEPLOY']][0]['spec']['template']['spec']['containers'][0]['image'])")" || rc=$?
  [ "$rc" -eq 0 ] && [ -n "$IMAGEN" ] || { echo "FALLA desplegar: no se pudo leer la imagen de $D en el overlay (código $rc)" >&2; exit 1; }
  PREFIJO="${IMAGEN%%/*}"; RESTO="${IMAGEN#*/}"; REPO="${RESTO%%:*}"; ETIQUETA="${RESTO##*:}"
  REG_HOST="${PREFIJO/#localhost/127.0.0.1}"

  registro() {
    local cab rc=0
    cab="$(curl -sfI --max-time 10 -H "Accept: $ACEPTA" "http://$REG_HOST/v2/$REPO/manifests/$ETIQUETA")" || rc=$?
    [ "$rc" -eq 0 ] || { echo "FALLA desplegar: curl al registro salió con $rc" >&2; exit 1; }
    printf '%s\n' "$cab" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}'
  }
  corre() {
    k -n synkra get pods -l "app.kubernetes.io/name=$D" -o jsonpath="{.items[0].status.containerStatuses[?(@.name==\"$D\")].imageID}" | sed 's/.*@//'
  }
  EN_REGISTRO="$(registro)"
  EN_POD="$(corre)"
  if [ "$EN_REGISTRO" != "$EN_POD" ]; then
    echo "desplegar: $D corre $EN_POD y el registro tiene $EN_REGISTRO; rollout restart"
    rc=0; k -n synkra rollout restart "deployment/$D" || rc=$?
    [ "$rc" -eq 0 ] || { echo "FALLA desplegar: rollout restart de $D salió con $rc" >&2; exit 1; }
    esperar "$D"
    EN_REGISTRO="$(registro)"
    EN_POD="$(corre)"
  fi
  [ -n "$EN_REGISTRO" ] && [ "$EN_REGISTRO" = "$EN_POD" ] || { echo "FALLA desplegar: $D corre ${EN_POD:-nada} y el registro tiene ${EN_REGISTRO:-nada}" >&2; exit 1; }
  echo "OK desplegar: $IMAGEN $EN_REGISTRO"
done
