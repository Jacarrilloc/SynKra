# F-06: copia de HEAD con version = "0.2.0" en backend/build.gradle.kts e IMAGEN_INIT_POR_DEFECTO 1.1.0; el overlay sigue en 0.1.0 / 1.0.0.
# Debe dar FALLA versiones y salida 1 (antes de construir nada). Requiere JAVA_HOME en Java 21.
cd "$(git rev-parse --show-toplevel)"
D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
sed -i 's/^version = "0.1.0"/version = "0.2.0"/' "$D/backend/build.gradle.kts"
sed -i 's|synkra-init-descarga:1.0.0"|synkra-init-descarga:1.1.0"|' "$D/backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java"
grep -n '^version' "$D/backend/build.gradle.kts"; grep -n 'IMAGEN_INIT_POR_DEFECTO =' "$D/backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java"
echo '$ SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/imagenes.sh   (copia)'
(cd "$D" && SYNKRA_REGISTRO_IMAGENES=mutacion.local:1 bash scripts/imagenes.sh > "$D/i.log" 2>&1); rc=$?; head -8 "$D/i.log"; echo "imagenes.sh salida=$rc"; grep -c 'docker build' "$D/i.log"
echo '$ bash scripts/verificar-imagenes.sh   (copia, contra las imagenes buenas)'
(cd "$D" && bash scripts/verificar-imagenes.sh > "$D/v.log" 2>&1); rc=$?; grep -E 'versiones|OK imagenes|FALLA' "$D/v.log" | head -8; echo "verificar-imagenes.sh salida=$rc"
echo '$ kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py   (copia)'
(cd "$D" && KUBECONFIG=/dev/null kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py); echo "verificar-backend.py salida=$?"
rm -rf "$D"
docker rmi mutacion.local:1/synkra-backend:0.2.0 mutacion.local:1/synkra-core-ejemplo:2.1.0 mutacion.local:1/synkra-init-descarga:1.1.0 2>&1 | grep -c Untagged
