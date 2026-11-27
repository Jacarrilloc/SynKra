# U04-T07: Ruta de subida de plugins

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-02
**Depende de:** U04-T06

---

## Alcance

**Dentro** (una línea, concreta):

> Dos rutas HTTP en `RutasCores` (o en una clase hermana package-private):
> - `POST /api/cores/{nombre}/plugins`, multipart con la parte `archivo` (el JAR) y la parte opcional `etiquetas` (JSON `{"clave":"valor"}`), que llama a `CatalogoApi.subirPlugin` y responde 201 con `PluginSubido` y `Location`;
> - `GET /api/cores/{nombre}/plugins/{plugin}/{version}`, que llama a `obtenerPlugin`.
>
> Además, los topes de C-38 (cuerpo JSON), C-50 (valor de etiqueta) y los límites de multipart.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Listar y filtrar (U04-T08). `GET /api/cores/{nombre}/plugins` sigue como lo dejó U04-T03, salvo lo que haga falta para que no choque con la ruta nueva.
- Cambios en `SubidaPlugin`, `RecepcionArtefacto` o las reglas de U04-T04 y U04-T06, salvo el tope de C-50, que se valida en la ruta.
- C-39 (campos de más en el JSON) y C-43 (400 de Tomcat).

---

## Archivos de contexto

- `tareas/U04-T06-subir-plugin.md` y `backend/src/main/java/io/synkra/catalogo/` (`RutasCores`, `SubidaPlugin`, `api/`)
- `backend/src/main/java/io/synkra/plataforma/` (`ManejadorErrores`, `ErrorApi`, `CodigosError`)
- `backend/src/main/resources/application.properties`
- `CANDIDATAS.md` (C-38, C-39, C-50)

---

## Decisiones del orquestador

1. **Flujo sin copia en memoria.** El `archivo` se pasa a `subirPlugin` como `InputStream` de la parte. El límite de 20 MB lo sigue aplicando `RecepcionArtefacto` (413).
   - `spring.servlet.multipart.max-file-size` y `max-request-size` se fijan por encima del límite del artefacto (por ejemplo `synkra.max-artefacto` + 1 MB), para que el 413 venga de R-C5 y no de Tomcat.
   - Si Tomcat corta antes (`MaxUploadSizeExceededException`), el `ManejadorErrores` lo traduce a 413 con `ErrorApi`, nunca a HTML ni a 500.
   - `file-size-threshold` queda en 0 o se documenta dónde escribe Tomcat su temporal.
2. **Partes.**
   - Sin `archivo`: 422 con `campo = "archivo"`.
   - Si `etiquetas` no es un objeto de texto a texto: 422 con `campo = "etiquetas"`.
   - Sin `etiquetas`: es un mapa vacío.
   - Si el cuerpo no es multipart: 415 o 400 con `ErrorApi` (se dice cuál en la bitácora).
3. **C-38.** Cuerpo JSON de las rutas de cores, como máximo 64 KB. Si lo pasa: 413 con `ErrorApi`, sin leerlo entero. La parte `etiquetas` tiene el mismo tope.
4. **C-50.** Cada valor de etiqueta de la subida: como máximo 63 caracteres. Si lo pasa: 422 `campo = "etiquetas.<clave>"`. La regla se pone en `MotivosCatalogo`, y una prueba fija su texto.
5. **Respuesta.** El 201 lleva `Location: /api/cores/{core}/plugins/{plugin}/{version}` y el JSON de `PluginSubido`, con `sha256` en hexadecimal y en minúsculas. El GET devuelve el mismo cuerpo, o 404.
6. Los errores de `subirPlugin` (404, 409, 413, 422) salen tal cual por `ManejadorErrores`.

---

## Criterios de aceptación

- [ ] **CA-1**: las rutas responden según el contrato, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*RutasPluginsTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RutasPluginsTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Con `RANDOM_PORT`, las pruebas cubren:
  - el 201 con `Location`, el `sha256` igual al de los bytes, y el GET de vuelta;
  - sin `archivo`: 422;
  - `etiquetas` que no son JSON, o con un valor numérico: 422;
  - un valor de 64 caracteres: 422 `etiquetas.<clave>`; uno de 63 se acepta;
  - un JAR de límite+1: 413 con `ErrorApi`, y nada en disco;
  - un cuerpo de más que `max-request-size`: 413 con `ErrorApi`, no HTML;
  - un JSON de 65 KB en `POST /api/cores`: 413;
  - un core inexistente: 404; una versión repetida: 409;
  - `GET` de un plugin inexistente: 404.

- [ ] **CA-2**: el `curl` de H-02 contra el backend arrancado
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); W=$(mktemp -d)
  printf 'nombre: kyc-pe\nversion: 1.3.0\ncore: synkra-core\ncoreCompatible: 2.x\netiquetas:\n  pais: pe\n' > "$W/synkra-plugin.yaml"
  (cd "$W" && jar cf kyc-pe-1.3.0.jar synkra-plugin.yaml)
  java -jar backend/build/libs/backend-*.jar --server.port=18080 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
  D=sha256:$(printf '%064d' 0)
  curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:18080/api/cores -H 'Content-Type: application/json' -d "{\"nombre\":\"synkra-core\",\"version\":\"2.1.0\",\"imagen\":\"localhost:5001/synkra-core@$D\"}"
  curl -s -F archivo=@"$W/kyc-pe-1.3.0.jar" localhost:18080/api/cores/synkra-core/plugins | python3 -I -c "import json,sys; print(json.load(sys.stdin)['sha256'])"
  sha256sum "$W/kyc-pe-1.3.0.jar" | cut -d' ' -f1
  curl -s -o /dev/null -w '%{http_code}\n' -F archivo=@"$W/kyc-pe-1.3.0.jar" localhost:18080/api/cores/synkra-core/plugins
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18080/api/cores/synkra-core/plugins/kyc-pe/1.3.0
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado: `201`, dos hashes iguales, `409` y `200`. Si el descriptor de ejemplo no cumple el formato de U01, se ajusta el YAML (no el código) y se dice en la bitácora.

- [ ] **CA-3**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos en ningún XML.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `catalogo/**`, `plataforma/ManejadorErrores.java` (y la configuración de plataforma que haga falta para el tope de C-38), `application.properties`, las pruebas nuevas, la bitácora y `revisiones/U04-T07/`.

---

## Plan de pruebas

- `RutasPluginsTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` con `@TempDir` como `synkra.data`) manda multipart con el cliente HTTP de la JDK o con `RestClient`, y arma los JAR en memoria como `SubirPluginTest`.

**Rojo primero:** se escribe la prueba con las rutas sin implementar y se registra el fallo de CA-1.

---

## Notas

- Para el JSON enorme y el cuerpo multipart enorme, la prueba no debe reservar el cuerpo entero en memoria si se puede evitar: puede usar un `InputStream` generado.
- Si C-38 obliga a tocar algo fuera de `plataforma/` y `catalogo/`, se anota y se para.
