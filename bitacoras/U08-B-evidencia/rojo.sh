# Rojo previo a implementar (decision CA-8): verificadores contra Dockerfiles y manifiestos de 8306ee7.
# Requiere: JAVA_HOME en Java 21; construye con etiquetas rojo.local:1 para no pisar las buenas.
set -u
cd "$(git rev-parse --show-toplevel)"
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)  JAVA_HOME=$JAVA_HOME"
echo '$ ./gradlew :backend:bootJar :core:bootJar'; ./gradlew -q :backend:bootJar :core:bootJar; echo "gradle=$?"
R=rojo.local:1
echo '$ docker build -t '$R'/synkra-backend:0.1.0 backend'; docker build -q -t $R/synkra-backend:0.1.0 backend
echo '$ docker build -t '$R'/synkra-core-ejemplo:2.1.0 core'; docker build -q -t $R/synkra-core-ejemplo:2.1.0 core
echo '$ docker build -t '$R'/synkra-init-descarga:1.0.0 init-descarga'; docker build -q -t $R/synkra-init-descarga:1.0.0 init-descarga
echo '$ SYNKRA_REGISTRO_IMAGENES='$R' bash scripts/verificar-imagenes.sh'
SYNKRA_REGISTRO_IMAGENES=$R bash scripts/verificar-imagenes.sh; echo "verificar=$?"
echo '$ python3 -I scripts/verificar-backend.py < /dev/null'
python3 -I scripts/verificar-backend.py </dev/null; echo "salida=$?"
echo '$ kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py'
kubectl kustomize deploy/overlays/kind 2>&1 | python3 -I scripts/verificar-backend.py; echo "salida=$?"
docker rmi $R/synkra-backend:0.1.0 $R/synkra-core-ejemplo:2.1.0 $R/synkra-init-descarga:1.0.0 >/dev/null 2>&1; true
