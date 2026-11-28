#!/usr/bin/env bash
# CA-6: cada regla muerde, una mutacion por copia (git archive HEAD + git init). Desde la raiz del worktree, con JAVA_HOME exportado.
# Imprime, por mutacion, el comando de la mutacion y las lineas FALLA que salieron. Nada se hace en el worktree.
W=$PWD; MUT=mutacion.local:1
ID0=$(docker image inspect -f '{{.Id}}' localhost:5001/synkra-front:0.1.0); echo "id-antes=$ID0"
nueva() { D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q); }
manifiestos() { # nombre mutacion(comando en la copia) ; corre ca5.sh en la copia y muestra lo que cayo
  local n="$1"; shift; nueva
  echo "=== $n :: $*"
  (cd "$D" && eval "$@") || echo "(la mutacion no cambio nada)"
  (cd "$D" && KUBECONFIG=/dev/null bash "$W/bitacoras/U09-D-evidencia/ca5.sh" 2>&1 | grep -E 'FALLA|^(base|backend|versiones|front|front-base|solo-backend)=[0-9]+$' | grep -vE '=0$')
  rm -rf "$D"
}
imagen() { # nombre mutacion ; construye con prefijo mutacion.local:1 y verifica
  local n="$1"; shift; nueva
  echo "=== $n :: $*"
  (cd "$D" && eval "$@")
  (cd "$D" && SYNKRA_REGISTRO_IMAGENES=$MUT bash scripts/imagenes.sh >/dev/null 2>&1; echo "imagenes=$?"; SYNKRA_REGISTRO_IMAGENES=$MUT bash scripts/verificar-imagenes.sh 2>&1 | grep -E '^FALLA'; echo "verificar=${PIPESTATUS[0]}")
  rm -rf "$D"
}
SDK=mcr.microsoft.com/dotnet/sdk:10.0.404@sha256:8c6edfb26c85153f236d26efebd9ad349aec403583f343f201ee67838b23658d
imagen user-0       "sed -i 's/^USER .*/USER 0/' front/Dockerfile"
imagen sin-digest   "sed -i '/aspnet/ s/@sha256:[0-9a-f]*//' front/Dockerfile"
imagen una-etapa    "sed -i '/aspnet/ s#FROM .*#FROM $SDK#' front/Dockerfile"
manifiestos sin-root         "sed -i '/runAsNonRoot: true/d' deploy/base/front/deployment.yaml"
manifiestos escribible       "sed -i 's/readOnlyRootFilesystem: true/readOnlyRootFilesystem: false/' deploy/base/front/deployment.yaml"
manifiestos token            "sed -i 's/automountServiceAccountToken: false/automountServiceAccountToken: true/' deploy/base/front/deployment.yaml"
manifiestos url-api          "sed -i 's#http://synkra-backend.synkra.svc:8080/#https://synkra.local/#' deploy/base/front/deployment.yaml"
manifiestos ruta-raiz        "sed -i '/service:/{n;s/synkra-front/synkra-backend/}' deploy/overlays/kind/ingress-front.yaml"
manifiestos raiz-doble       "sed -i 's#- path: /api\$#- path: /#' deploy/overlays/kind/ingress.yaml"
manifiestos sin-tls          "sed -i '/^  tls:/,/secretName/d' deploy/overlays/kind/ingress-front.yaml"
manifiestos base-sin-front   "sed -i '/^  - front\$/d' deploy/base/kustomization.yaml"
manifiestos tag-distinto     "sed -i '/newName: localhost:5001\/synkra-front/{n;s/0.1.0/0.2.0/}' deploy/overlays/kind/kustomization.yaml"
manifiestos primero          "sed -i 's/c = por_nombre\[\"synkra-backend\"\]/c = por_nombre[\"synkra-front\"]/' scripts/verificar-backend.py"
echo "=== orden del render (Deployments)"; KUBECONFIG=/dev/null kubectl kustomize deploy/overlays/kind | python3 -I -c "import sys,yaml; print([o['metadata']['name'] for o in yaml.safe_load_all(sys.stdin) if o and o['kind']=='Deployment'])"
docker rmi $MUT/synkra-backend:0.1.0 $MUT/synkra-core-ejemplo:2.1.0 $MUT/synkra-init-descarga:1.0.0 $MUT/synkra-front:0.1.0 >/dev/null 2>&1
ID1=$(docker image inspect -f '{{.Id}}' localhost:5001/synkra-front:0.1.0); echo "id-despues=$ID1"; [ "$ID0" = "$ID1" ] && echo "imagen-buena-intacta"
