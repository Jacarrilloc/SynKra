# CA-8: una copia de HEAD por mutacion. Manifiestos: verificar-backend.py (overlay y base). Imagenes: imagenes.sh + verificar-imagenes.sh con otro prefijo.
R=mutacion.local:1
RAIZ=$(pwd)
ids() { for i in synkra-backend:0.1.0 synkra-core-ejemplo:2.1.0 synkra-init-descarga:1.0.0; do docker image inspect -f '{{.Id}}' localhost:5001/$i; done; }
echo "== Id de las imagenes buenas ANTES"; ids

manifiesto() { # nombre ruta sed-expr [modo: overlay|base]
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  echo; echo "== mutacion $1: sed -i '$3' $2"
  sed -i "$3" "$D/$2"
  if (cd "$D" && git diff --no-index --quiet "$RAIZ/$2" "$2"); then echo "MUTACION NO APLICADA"; fi
  if [ "${4:-overlay}" = base ]; then
    echo '$ kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base'
    (cd "$D" && kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base); echo "salida=$?"
  else
    echo '$ kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py'
    (cd "$D" && kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py); echo "salida=$?"
  fi
  rm -rf "$D"
}
manifiesto recreate deploy/base/backend/deployment.yaml 's/type: Recreate/type: RollingUpdate/'
manifiesto sin-root deploy/base/backend/deployment.yaml '/runAsNonRoot: true/d'
manifiesto cuenta deploy/base/backend/deployment.yaml 's/serviceAccountName: deployer/serviceAccountName: default/'
manifiesto solo-lectura deploy/base/backend/deployment.yaml 's/readOnlyRootFilesystem: true/readOnlyRootFilesystem: false/'
manifiesto tls deploy/overlays/kind/ingress.yaml '/^  tls:/,/secretName/d'
# registro-base: la variable se mueve del overlay a la base (dos sed, uno por archivo); se mira la base
D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
echo; echo "== mutacion registro-base: (1) base gana SYNKRA_REGISTRO_PERMITIDO tras JAVA_TOOL_OPTIONS; (2) el overlay la pierde"
echo "sed -i '/value: -XX:MaxRAMPercentage=70/a\\            - name: SYNKRA_REGISTRO_PERMITIDO\\n              value: localhost:5001' deploy/base/backend/deployment.yaml"
sed -i '/value: -XX:MaxRAMPercentage=70/a\            - name: SYNKRA_REGISTRO_PERMITIDO\n              value: localhost:5001' "$D/deploy/base/backend/deployment.yaml"
echo "sed -i '/name: SYNKRA_REGISTRO_PERMITIDO/,+1d' deploy/overlays/kind/backend-kind.yaml"
sed -i '/name: SYNKRA_REGISTRO_PERMITIDO/,+1d' "$D/deploy/overlays/kind/backend-kind.yaml"
echo '$ (overlay) kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py'
(cd "$D" && kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py); echo "salida=$?"
echo '$ (base) kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base'
(cd "$D" && kubectl kustomize deploy/base/backend | python3 -I scripts/verificar-backend.py --base); echo "salida=$?"
rm -rf "$D"

imagen() { # nombre ruta sed-expr
  local D; D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  echo; echo "== mutacion $1: sed -i '$3' $2"
  sed -i "$3" "$D/$2"
  (cd "$D" && git diff --no-index --quiet "$RAIZ/$2" "$2") && echo "MUTACION NO APLICADA"
  echo '$ SYNKRA_REGISTRO_IMAGENES='$R' bash scripts/imagenes.sh && SYNKRA_REGISTRO_IMAGENES='$R' bash scripts/verificar-imagenes.sh'
  (cd "$D" && SYNKRA_REGISTRO_IMAGENES=$R bash scripts/imagenes.sh > "$D/imagenes.log" 2>&1); local rc=$?
  grep -vE 'Installing|fetch https|^\+ docker build|^#0 |^#[0-9]+ \[|^#[0-9]+ (DONE|CACHED|exporting|naming|unpacking|transferring|resolve|sha256)|^$' "$D/imagenes.log" | tail -30
  echo "imagenes.sh salida=$rc"
  if [ "$rc" -eq 0 ]; then
    (cd "$D" && SYNKRA_REGISTRO_IMAGENES=$R bash scripts/verificar-imagenes.sh > "$D/ver.log" 2>&1); rc=$?
    grep -v '^ok ' "$D/ver.log"; echo "verificar-imagenes.sh salida=$rc"
  fi
  rm -rf "$D"
}
imagen user-0 backend/Dockerfile 's/^USER 10001:10001/USER 0/'
imagen sin-digest init-descarga/Dockerfile 's/^\(FROM alpine:3.20\)@sha256:[0-9a-f]*/\1/'
imagen uid-java backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java 's/UID_CONTENEDORES = 10001L/UID_CONTENEDORES = 10002L/'
imagen shellcheck init-descarga/descargar.sh '$a ls $1'

echo; echo "== limpieza: docker rmi de las etiquetas mutacion.local:1"
docker rmi $R/synkra-backend:0.1.0 $R/synkra-core-ejemplo:2.1.0 $R/synkra-init-descarga:1.0.0 2>&1 | grep -c Untagged
echo "== Id de las imagenes buenas DESPUES"; ids
