# U04-T03: Rutas de cores

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-01
**Depende de:** U04-T02

---

## Alcance

**Dentro** (una línea, concreta):

> Un controlador en el módulo `catalogo` con `POST /api/cores`, `GET /api/cores`, `GET /api/cores/{nombre}` y `GET /api/cores/{nombre}/plugins` sobre `CatalogoApi`. Por ahora la lista de plugins sale vacía. Las validaciones de `@Valid` se traducen a `detalles` (C-07).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Subir y listar plugins de verdad: la ruta `…/plugins` devuelve `[]` hasta U04-T07 y U04-T08, pero ya responde 404 si el core no existe.
- Cambios en las reglas de U04-T02. Si una regla no cuadra con la ruta, se registra y se para.
- Seguridad y roles (U10).
- Cambios en `verificador/`.

---

## Archivos de contexto

- `aidlc-docs/inception/user-stories/stories.md` (H-01, con sus `curl`)
- `aidlc-docs/construction/plans/U04-catalogo-code-generation-plan.md` (U04-T03)
- `backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java`
- `tareas/U04-T02-registrar-core.md`
- `CANDIDATAS.md` (C-07)

---

## Decisiones del orquestador

1. **Contrato.** `POST /api/cores` recibe `{"nombre","version","imagen"}`.
   - Si el registro sale bien, responde **201** con `Location: /api/cores/{nombre}` y el cuerpo `{"nombre","versiones":[{"version","imagen"}]}`.
   - `GET /api/cores` devuelve la lista ordenada por nombre.
   - `GET /api/cores/{nombre}` devuelve el core, o 404.
   - `GET /api/cores/{nombre}/plugins` devuelve `[]`, o 404 si el core no existe.
2. **C-07.** Si el DTO usa `@Valid`, `ManejadorErrores` traduce `MethodArgumentNotValidException` y `HandlerMethodValidationException` a `detalles`, con el `campo` y el `motivo` reales. Si los campos faltantes los resuelve la validación de U04-T02 y no `@Valid`, C-07 sigue abierta y se dice en la bitácora. **Recomendado:** cerrarla aquí, porque es el primer DTO del backend.
3. **Arranque y prueba.** CA-2 se corre contra el JAR real, igual que en U03-T02, y no con MockMvc.

---

## Criterios de aceptación

- [ ] **CA-1**: las rutas responden según el contrato, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*RutasCoresTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RutasCoresTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 7, `skipped="0" failures="0" errors="0"`. Con `@SpringBootTest(webEnvironment = RANDOM_PORT)`, las pruebas comprueban:
  - el 201 con `Location` y el cuerpo;
  - una segunda versión: el GET muestra las dos;
  - el 409 en una repetida, con el cuerpo `ErrorApi`;
  - el 422 con los tres detalles;
  - un JSON sin `imagen`: 422 con `campo = "imagen"`, no 400 genérico (C-07);
  - un JSON malformado: 400 `SOLICITUD_INVALIDA`;
  - `GET …/plugins` de un core existente: `[]`. De uno inexistente: 404.

- [ ] **CA-2**: los `curl` de H-01 contra el backend arrancado
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d)
  java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  D=sha256:$(printf '%064d' 0)
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:18080/api/cores -H 'Content-Type: application/json' -d "{\"nombre\":\"synkra-core\",\"version\":\"2.1.0\",\"imagen\":\"localhost:5001/synkra-core@$D\"}"
  curl -s localhost:18080/api/cores/synkra-core/plugins; echo
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:18080/api/cores -H 'Content-Type: application/json' -d "{\"nombre\":\"synkra-core\",\"version\":\"3.0.0\",\"imagen\":\"localhost:5001/synkra-core@$D\"}"
  curl -s localhost:18080/api/cores/synkra-core | python3 -I -c "import json,sys; print([v['version'] for v in json.load(sys.stdin)['versiones']])"
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:18080/api/cores -H 'Content-Type: application/json' -d "{\"nombre\":\"synkra-core\",\"version\":\"3.0.0\",\"imagen\":\"localhost:5001/synkra-core@$D\"}"
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select core, version from cat_version_core order by version').fetchall())" "$SYNKRA_DATA/synkra.db"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado, en orden: `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409` y `[('synkra-core', '2.1.0'), ('synkra-core', '3.0.0')]`.

- [ ] **CA-3**: todo el backend y las fronteras siguen pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos en ningún XML.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: el controlador y sus DTO en `backend/src/main/java/io/synkra/catalogo/**`, `ManejadorErrores.java` si se cierra C-07, las pruebas nuevas, `CANDIDATAS.md` si se cierra C-07, la bitácora y `revisiones/U04-T03/`.

---

## Plan de pruebas

- `RutasCoresTest` con `RANDOM_PORT` y `TestRestTemplate` o `RestClient`, y `@TempDir` como `synkra.data`.

**Rojo primero:** se escribe `RutasCoresTest` antes del controlador. Se registra que CA-1 falla con 404 en `POST /api/cores` y después pasa.

---

## Notas

- Si el puerto 18080 está ocupado, se anota en la bitácora y se cambia en todo CA-2.
- `jq` puede no estar en la máquina, así que CA-2 usa `python3 -I`.
