# U03-T04: Salud

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24
**Depende de:** U03-T02

---

## Alcance

**Dentro** (una línea, concreta):

> `/actuator/health` responde `UP` e incluye el componente `db` (la base SQLite de `SYNKRA_DATA`), y pasa a `DOWN` cuando la base no se puede usar; el resto de endpoints de Actuator queda sin exponer.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Sondas de Kubernetes (`liveness`/`readiness` en el Deployment): son de U08.
- Métricas, Prometheus u otros endpoints de Actuator (`info`, `env`, `beans`).
- Seguridad del endpoint (U10).
- PRAGMA de SQLite (C-04) y fechas (C-05): son de U04.
- Cambios en el formato de error (U03-T03) o en la migración `V1__base.sql`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/user-stories/stories.md` (H-24)
- `backend/src/main/resources/application.properties`
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java`

---

## Criterios de aceptación

- [ ] **CA-1**: la prueba de salud pasa, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*SaludTest'
  ```
  Esperado: `BUILD SUCCESSFUL`. `SaludTest` arranca el contexto completo con `RANDOM_PORT` y un
  `SYNKRA_DATA` temporal, y cubre: `status` = `UP`, `components.db.status` = `UP`, y
  `/actuator/env` y `/actuator/beans` responden 404.

- [ ] **CA-2**: con la base inutilizable, la salud cae. Un caso de `SaludTest` deja la base
  inaccesible después de arrancar (por ejemplo, borrando el archivo y su directorio) y comprueba que
  `/actuator/health` responde 503 con `status` = `DOWN`.
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*SaludTest.xml
  ```
  Esperado: `tests` ≥ 3, `skipped="0" failures="0" errors="0"`.

- [ ] **CA-3**: lectura de vuelta con la aplicación real (el criterio de la tabla)
  ```bash
  ./gradlew :backend:bootJar
  D=$(mktemp -d); SYNKRA_DATA=$D java -jar backend/build/libs/backend-0.1.0.jar --server.port=18080 >/dev/null 2>&1 & P=$!
  for i in $(seq 60); do curl -sf localhost:18080/actuator/health >/dev/null && break; sleep 1; done
  curl -s localhost:18080/actuator/health | python3 -I -c "import sys,json; d=json.load(sys.stdin); print(d['status'], d['components']['db']['status'])"
  kill $P; ls $D
  ```
  Esperado: `UP UP` y `synkra.db` en el listado del directorio.

- [ ] **CA-4**: todo el backend sigue pasando, sin caché
  ```bash
  ./gradlew :backend:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- `SaludTest` con `@SpringBootTest(webEnvironment = RANDOM_PORT)` y `TestRestTemplate`, con el mismo
  `@TempDir` y `@DynamicPropertySource` que `SynkraAppTest`.

**Rojo primero:** prueba antes que código; registra el fallo de CA-1 (sin `components` en la
respuesta) y luego el mismo comando pasando.

---

## Notas

- Lo esperable es solo configuración en `application.properties`
  (`management.endpoints.web.exposure.include=health` y
  `management.endpoint.health.show-components=always`). Si hace falta un `HealthIndicator` propio,
  se justifica en la bitácora.
- La máquina no tiene `jq`: CA-3 lee el JSON con `python3 -I`.
