export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
JAVA=$JAVA_HOME/bin/java
export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d); H=$(mktemp -d)
arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=pe $JAVA -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18092 > "$J/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18092/actuator/health >/dev/null; do sleep 1; done'; }
arranca 0; kill $PID; wait $PID 2>/dev/null
python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1]); ahora = int(time.time() * 1000); m = 60000
for i, estado, en in ((71, "pendiente", ahora - 31 * m), (72, "pendiente", ahora), (73, "confirmado", ahora - 31 * m), (74, "pendiente", ahora - 31 * m)):
    c.execute("insert into dep_plan (id, pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, estado, creado_por,"
              " creado_en, vence_en) values (?, 'pe', 'despliegue', null, 'synkra-core', '2.1.0', '[]', '{}', '{}', ?, 'anonimo', ?, ?)",
              (i, estado, en, en + 30 * m))
c.commit()
EOF
arranca 1
for id in 71 71 72 73 999; do
  curl -s -o "$J/r.json" -w '%{http_code} ' -X POST localhost:18092/api/despliegues/$id/confirmacion
  python3 -I -c "import json,sys; r=json.load(open(sys.argv[1])); print(r['codigo'], r['mensaje'])" "$J/r.json"; done
kill $PID; wait $PID 2>/dev/null
python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, estado from dep_plan order by id').fetchall())" "$SYNKRA_DATA/synkra.db"
