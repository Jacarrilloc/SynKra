I=localhost:5001/synkra-backend:0.1.0; N=synkra-u08b-backend-$$
D=$(mktemp -d); chmod 0777 "$D"
trap 'docker rm -f "$N" >/dev/null 2>&1; rm -rf "$D"' EXIT          # registrado antes del docker run
T0=$(date +%s)
docker run -d --name "$N" --user 10001:10001 --read-only --tmpfs /tmp:rw,exec,nosuid,nodev --cap-drop ALL --security-opt no-new-privileges \
  -e SYNKRA_DATA=/data -e SYNKRA_PAISES=co,pe -e MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true -e JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -e SYNKRA_REGISTRO_PERMITIDO=localhost:5001 \
  -m 512m --cpus 0.5 -v "$D:/data" -p 127.0.0.1::8080 "$I" || { echo "FALLA ca6: docker run"; exit 1; }
P=$(docker port "$N" 8080/tcp | head -1 | cut -d: -f2)
timeout 180 bash -c "until curl -sf 127.0.0.1:$P/actuator/health/readiness >/dev/null; do sleep 2; done"; echo "espera=$?"
echo "readiness tras $(( $(date +%s) - T0 )) s"
for r in /actuator/health/liveness /actuator/health/readiness /actuator/health; do printf '%s ' "$r"; curl -s -o /dev/null -w '%{http_code}\n' 127.0.0.1:$P$r; done
docker exec "$N" id -u
ls -ln "$D"
python3 -I -c "import sqlite3,sys; print(sqlite3.connect(sys.argv[1]).execute('select max(version) from flyway_schema_history').fetchone())" "$D/synkra.db"
docker logs "$N" 2>&1 | grep -ciE 'read-only file system|permission denied'
