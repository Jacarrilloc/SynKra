cd /home/casdav/Projects/Master/wt-m6r-U06-B
ss -ltn | grep ':18089 ' ; echo "puerto18089-ocupado=$?"
./gradlew :backend:bootJar 2>&1 | grep -E "BUILD"
export SYNKRA_DATA=$(mktemp -d); R=$(mktemp -d)
arranca() { $JAVA_HOME/bin/java -jar backend/build/libs/backend-*.jar --server.port=18089 > "$R/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18089/actuator/health >/dev/null; do sleep 1; done'; }
arranca 0; kill $PID; wait $PID 2>/dev/null
python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
import sqlite3, sys, json
c = sqlite3.connect(sys.argv[1])
ver = json.dumps({"global": "APROBADA", "porPlugin": [], "motivosGlobales": []})
img = "localhost:5001/synkra-core@sha256:" + "a" * 64
filas = [(11, "pe", 1795000001123, "listo", 1795000001999), (12, "pe", 1795000003456, "fallido", 1795000003999),
         (13, "pe", 1795000002789, "aplicando", None), (14, "co", 1795000005321, "listo", 1795000005999)]
for i, pais, en, estado, fin in filas:
    c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
              " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', ?,"
              " 'ana', 'luis', ?, ?, ?, ?)", (i, pais, img, en, estado, fin, ver))
    c.execute("insert into reg_plugin (despliegue_id, nombre, version, sha256, etiquetas) values (?, 'kyc-pe', '1.3.0', ?, ?)",
              (i, "b" * 64, json.dumps({"pais": pais})))
c.execute("insert into reg_contexto (despliegue_id, log_core, log_init, eventos) values (12, 'linea-1\n', '', 'Warning BackOff: x')")
c.commit()
EOF
consulta() {
  curl -s localhost:18089/api/paises/pe/despliegues | python3 -I -c "import json,sys; l=json.load(sys.stdin); print([d['id'] for d in l], l[0]['confirmadoEn'], sorted(l[0]))"
  curl -s localhost:18089/api/despliegues/12 | python3 -I -c "import json,sys; d=json.load(sys.stdin); print(d['estado'], d['confirmadoPor'], sorted(d['contexto']), d['contexto']['eventos'], d['veredicto']['global'], d['plugins'][0]['etiquetas'])"
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18089/api/despliegues/99
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18089/api/paises/PE/despliegues
}
arranca 1; consulta > "$R/a.txt"; kill $PID; wait $PID 2>/dev/null
arranca 2; consulta > "$R/b.txt"; kill $PID; wait $PID 2>/dev/null
echo "--- a.txt"; cat "$R/a.txt"; echo "--- b.txt"; cat "$R/b.txt"; diff "$R/a.txt" "$R/b.txt"; echo "diff=$?"
python3 -I -c "import sqlite3,sys; print(sqlite3.connect(sys.argv[1]).execute('select version from flyway_schema_history order by installed_rank').fetchall())" "$SYNKRA_DATA/synkra.db"
