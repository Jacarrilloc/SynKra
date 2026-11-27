# U04-T08: Filtrar por etiqueta y aislamiento de carpetas (R-C12)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-05, H-06
**Depende de:** U04-T07

---

## Alcance

**Dentro** (una línea, concreta):

> `CatalogoApi.listarPlugins(core, Map<String,String> filtro)` y `GET /api/cores/{nombre}/plugins?clave=valor&…`. Devuelve los plugins de esa carpeta que cumplen **todas** las parejas del filtro. Sin filtro, devuelve todos los de la carpeta. Una carpeta nunca muestra los plugins de otra (P4).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- La subida (U04-T06 y U04-T07) y la forma de `PluginSubido`.
- Paginación y orden configurable. El orden es fijo (decisión 3).
- `Catalogo` de U01 (U04-T09).

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-rules.md` (R-C12)
- `aidlc-docs/inception/user-stories/stories.md` (H-05, H-06)
- `backend/src/main/java/io/synkra/catalogo/` (`RutasCores`, los repositorios, `CatalogoServicio`)
- `CANDIDATAS.md` (C-49)

---

## Decisiones del orquestador

1. **Filtro.** Cada parámetro de consulta es una pareja `clave=valor` contra `cat_etiqueta`, con cualquier `origen`.
   - Se aplican todas a la vez (AND).
   - Una clave repetida en la consulta (`?pais=pe&pais=co`) da 422 con `campo = "pais"`.
   - Las claves se validan con `Descriptor.NOMBRE`, y una clave inválida da 422.
   - La comparación es exacta y distingue mayúsculas (C-49 se registra en la bitácora, no se cambia).
2. **SQL.** Una sola consulta con parámetros enlazados. No se concatena texto del usuario. Un filtro de N parejas no hace N consultas por plugin.
3. **Respuesta.** Lista de `PluginSubido` ordenada por `nombre` y luego por versión semántica (con `Version` de U01, no por texto). Un core inexistente da 404. Un core sin plugins o sin coincidencias da `[]`.
4. U04-T03 ya devuelve `[]` en `GET …/plugins`; esta tarea lo reemplaza por el listado real, y su prueba se actualiza sin perder el caso de 404.

---

## Criterios de aceptación

- [ ] **CA-1**: R-C12, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*ListarPluginsTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ListarPluginsTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 9, `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - sin filtro: todos los de la carpeta, en orden (con `1.10.0` después de `1.9.0`);
  - `?pais=pe`: solo los de Perú;
  - `?pais=pe&cliente=x`: solo los que cumplen las dos;
  - una etiqueta que vino de la subida también filtra;
  - un filtro sin coincidencias: `[]`;
  - `otro-core` con un plugin de etiqueta `pais=pe`: no aparece en la carpeta de `synkra-core`, y al revés;
  - una clave repetida: 422; una clave inválida: 422;
  - un core inexistente: 404;
  - un valor con `'` o `%`: no rompe la consulta y se compara literal.

- [ ] **CA-2**: los `curl` de H-05 y H-06 contra el backend arrancado
  Mismo arranque que en el CA-2 de U04-T07. Se registran `synkra-core` y `otro-core`, se suben `kyc-pe` (con `pais: pe`) y `kyc-co` (con `pais: co`) a `synkra-core`, y `kyc-xx` (con `pais: pe`) a `otro-core`. Luego:
  ```bash
  curl -s "localhost:18080/api/cores/synkra-core/plugins?pais=pe" | python3 -I -c "import json,sys; print(len(json.load(sys.stdin)))"
  curl -s "localhost:18080/api/cores/synkra-core/plugins" | python3 -I -c "import json,sys; print(sorted(p['nombre'] for p in json.load(sys.stdin)))"
  ```
  Esperado: `1` y `['kyc-co', 'kyc-pe']`. La bitácora copia el script completo y su salida.

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
  Esperado: `catalogo/**`, las pruebas nuevas o ajustadas, la bitácora y `revisiones/U04-T08/`.

---

## Plan de pruebas

- `ListarPluginsTest` sube los plugins por `CatalogoApi.subirPlugin` (no por SQL directo) y llama tanto a `listarPlugins` como a la ruta.

**Rojo primero:** la prueba con `listarPlugins` sin implementar, registrando el fallo de CA-1.
