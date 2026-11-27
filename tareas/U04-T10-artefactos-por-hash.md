# U04-T10: Servir artefactos por hash (R-C13)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-15 (contribuye)
**Depende de:** U04-T07

---

## Alcance

**Dentro** (una línea, concreta):

> `CatalogoApi.abrirArtefacto(sha256)` y `GET /api/artefactos/{sha256}`, que sirve el JAR guardado con `Content-Type: application/java-archive`, `Content-Length` y el hash en la cabecera `X-Synkra-Sha256`. Un hash desconocido da 404.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Autenticación (U10) y rangos HTTP (`Range`).
- El init container que descarga (U07).
- La reconciliación completa de disco contra base de C-48, salvo lo que diga la decisión 4.

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-rules.md` (R-C13)
- `backend/src/main/java/io/synkra/catalogo/` (`SubidaPlugin`, `PluginRepositorio`, `RutasCores`)
- `CANDIDATAS.md` (C-47, C-48)

---

## Decisiones del orquestador

1. **Entrada.** `sha256` son 64 caracteres hexadecimales en minúsculas. Cualquier otra cosa da 404, igual que un hash desconocido, sin tocar la base ni el disco.
2. **Varias filas, un hash.** Si varios plugins tienen los mismos bytes, cualquiera de sus rutas sirve. Se elige una de forma determinista (por ejemplo, la primera por clave).
3. **Ruta en disco.** La ruta se calcula desde la columna `ruta` relativa a `$SYNKRA_DATA`, con `normalize().startsWith(raiz)` y sin seguir enlaces simbólicos (`NOFOLLOW_LINKS`). Si falla esa comprobación, es un error interno (500) y no se sirve nada.
4. **Integridad al servir.** El archivo se sirve en flujo, sin cargarlo entero. Si el archivo falta en disco pero la fila existe, da 500 con un motivo constante (es C-48, el caso inverso). Si el tamaño en disco no coincide con `tamano`, también da 500, antes de mandar el primer byte. El hash no se recalcula en cada descarga, y se dice en la bitácora.
5. **Cabeceras.** `Content-Length` es igual a `tamano`. `X-Synkra-Sha256` es igual al hash pedido. `Content-Disposition: attachment; filename="<nombre>-<version>.jar"`, con nombres ya validados.

---

## Criterios de aceptación

- [ ] **CA-1**: R-C13, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*ArtefactosPorHashTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ArtefactosPorHashTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 8, `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - el cuerpo descargado tiene el mismo sha256 que la subida, con las tres cabeceras;
  - un hash desconocido: 404 `ErrorApi`;
  - un hash en mayúsculas, de 63 caracteres o con `../`: 404;
  - dos plugins con los mismos bytes: los dos se sirven por el mismo hash;
  - el archivo borrado a mano: 500, no 200 vacío;
  - el archivo truncado a mano: 500 antes del cuerpo;
  - la ruta guardada apunta fuera de la raíz (fila editada en la prueba): 500 y nada servido.

- [ ] **CA-2**: el `curl` de H-15 contra el backend arrancado
  Mismo arranque y subida que en el CA-2 de U04-T07. Luego:
  ```bash
  S=$(sha256sum "$W/kyc-pe-1.3.0.jar" | cut -d' ' -f1)
  curl -s localhost:18080/api/artefactos/$S | sha256sum | cut -d' ' -f1; echo $S
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18080/api/artefactos/$(printf '%064d' 0)
  ```
  Esperado: los dos hashes iguales y `404`.

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
  Esperado: `catalogo/**`, las pruebas nuevas, la bitácora y `revisiones/U04-T10/`.

---

## Plan de pruebas

- `ArtefactosPorHashTest` (`RANDOM_PORT`, `@TempDir`) sube por la API y descarga por HTTP.

**Rojo primero:** la prueba con la ruta sin implementar, registrando el fallo.
