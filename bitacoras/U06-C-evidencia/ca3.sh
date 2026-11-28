# Se ejecuta desde la raíz del worktree: bash bitacoras/U06-C-evidencia/ca3.sh
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "# fecha: $(date -u +%FT%TZ)  sha: $(git rev-parse --short HEAD)  puerto 18091 libre: $(ss -ltn | grep -c ':18091 ') sockets"
./gradlew :backend:bootJar 2>&1 | grep -E "BUILD|FAILED"
export SYNKRA_DATA=$(mktemp -d); R=$(mktemp -d); H=$(mktemp -d)
arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=pe,co,ar $JAVA_HOME/bin/java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18091 > "$R/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18091/actuator/health >/dev/null; do sleep 1; done'; }
# 1. Primer arranque: solo migra. Se para y se siembran filas (el registro no tiene ruta de escritura).
arranca 0; kill $PID; wait $PID 2>/dev/null
python3 -I - "$SYNKRA_DATA/synkra.db" <<'PY'
import sqlite3, sys, json
c = sqlite3.connect(sys.argv[1])
ver = json.dumps({"global": "APROBADA", "porPlugin": [], "motivosGlobales": []})
img = "localhost:5001/synkra-core@sha256:" + "a" * 64
for i, pais in ((61, "pe"), (62, "mx")):
    c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
              " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', ?,"
              " 'ana', 'luis', 1795000001123, 'listo', 1795000001999, ?)", (i, pais, img, ver))
    c.execute("insert into reg_plugin (despliegue_id, nombre, version, sha256, etiquetas) values (?, 'kyc-pe', '1.3.0', ?, '{}')",
              (i, "b" * 64))
c.commit()
PY
# 2. Segundo arranque: la vista general.
arranca 1
curl -s -o "$R/v.json" -w '%{http_code}\n' localhost:18091/api/paises
echo "--- cuerpo literal:"; cat "$R/v.json"; echo
python3 -I -c "import json,sys; l=json.load(open(sys.argv[1])); print([(d['pais'], d['lectura'], d['concuerda'], d['ultimoDespliegueId'], d['versionCore'], d['imagen'], d['plugins']) for d in l]); print(sorted(l[0]))" "$R/v.json"
kill $PID; wait $PID 2>/dev/null
