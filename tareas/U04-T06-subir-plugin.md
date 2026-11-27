# U04-T06: Subir un plugin (R-C4, R-C7 a R-C10)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-02, H-03
**Depende de:** U04-T04

---

## Alcance

**Dentro** (una línea, concreta):

> `CatalogoApi.subirPlugin(core, InputStream, Map<String,String> etiquetas)` y `obtenerPlugin(core, nombre, version)`. Su implementación usa `RecepcionArtefacto` de U04-T04 y `LectorDescriptor.leer` de U01 sobre el temporal. Aplica R-C4 y R-C7 a R-C10, mueve el JAR a `$SYNKRA_DATA/artefactos/<core>/<nombre>/<version>.jar` y guarda `cat_plugin` y `cat_etiqueta` en una sola transacción.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Las rutas HTTP y el multipart (U04-T07).
- Listar y filtrar plugins (U04-T08).
- Cambios en `verificador/` y en `V2`. Si `LectorDescriptor` necesita cambios (C-13, C-20), se anota y se para.
- Cambios en las reglas de U04-T04, salvo usarlas.

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/` (las reglas, el flujo y las entidades)
- `aidlc-docs/inception/user-stories/stories.md` (H-02, H-03)
- `verificador/src/main/java/io/synkra/verificador/LectorDescriptor.java`, `Descriptor.java`, `Motivo.java` y `CodigoMotivo.java`
- `tareas/U04-T04-recepcion-jar.md`
- `backend/src/main/java/io/synkra/catalogo/` (`CatalogoServicio`, `MotivosCatalogo`, los repositorios)
- `CANDIDATAS.md` (C-13, C-19, C-20, C-31)

---

## Decisiones del orquestador

1. **Orden.** Es el del diagrama de `business-logic-model.md`, y el primer fallo corta:
   - core inexistente: 404 `NO_ENCONTRADO`, antes de leer el flujo;
   - tamaño y rutas: lo resuelve U04-T04;
   - descriptor (R-C7);
   - core del descriptor (R-C8);
   - repetido (R-C9);
   - etiquetas (R-C10);
   - mover y guardar.
2. **Errores del descriptor (R-C7).** 422 `ENTIDAD_NO_PROCESABLE`, con un detalle por cada `Motivo` de U01, en su orden:
   - `campo` es `motivo.campo()` o, si no tiene, `"descriptor"`;
   - `motivo` es `"<CodigoMotivo>: <mensaje>"`, sin traducir ni copiar los textos de U01;
   - como mucho 50 detalles; si hay más, el último dice cuántos se omitieron (C-19).
3. **R-C8.** 422 con `codigo = "CORE_EQUIVOCADO"` (de `CodigoMotivo.CORE_EQUIVOCADO.name()`). El detalle es `campo = "core"`, con un motivo que nombra los dos cores.
4. **R-C9.** Si ya existen el nombre y la versión en esa carpeta: 409 `CONFLICTO` con el detalle `version`. No cambia nada en la base ni en disco, ni siquiera si el sha256 es otro.
5. **R-C10.** Las etiquetas del descriptor (`origen = 'descriptor'`) se unen a las de la petición (`origen = 'subida'`):
   - la misma clave con el mismo valor se guarda una vez, con `origen = 'descriptor'`;
   - la misma clave con otro valor es un choque: 422 `codigo = "ETIQUETA_EN_CONFLICTO"`, con un detalle por clave (`campo = "etiquetas.<clave>"`);
   - las claves y los valores de la petición se validan con las mismas reglas que U01 aplica a las del descriptor, reutilizando su código sin copiarlo; si U01 no las expone, se anota y se para.
6. **Mover y guardar (R-C11).**
   - El JAR se mueve con `Files.move(…, ATOMIC_MOVE)` desde `.tmp/`, que está en el mismo volumen.
   - Los metadatos se insertan en la misma transacción. Si el insert falla después de mover, se borra el archivo movido.
   - Si el movimiento falla, no se inserta nada.
   - La ruta final se calcula desde nombres ya validados y se comprueba con `normalize().startsWith(raiz)`.
7. **Entradas duplicadas (C-44).** Un JAR con dos entradas del mismo nombre se rechaza con 422 `RUTA_NO_PERMITIDA` antes de leer el descriptor, con un motivo constante, porque `ZipFile.getEntry` y el lector en flujo pueden ver contenidos distintos. La comprobación vive en `RecepcionArtefacto` (U04-T04), y aquí se agrega con su prueba.
8. **Lo que devuelve.** `PluginSubido(core, nombre, version, coreCompatible, etiquetas, sha256, tamano)`. `obtenerPlugin` devuelve lo mismo, o 404.
   - `subido_por` va como `'anonimo'`.
   - `descripcion` puede ser nula.
   - `ruta` es relativa a `$SYNKRA_DATA`.

---

## Criterios de aceptación

- [ ] **CA-1**: R-C4 y R-C7 a R-C10, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*SubirPluginTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*SubirPluginTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 12, `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - un plugin válido:
    - devuelve el descriptor y un `sha256` igual al de los bytes;
    - el archivo está en `artefactos/<core>/<nombre>/<version>.jar`;
    - hay una fila en `cat_plugin` y sus etiquetas tienen el origen correcto;
  - core inexistente: 404 y nada en disco;
  - JAR sin `synkra-plugin.yaml`: 422 con `DESCRIPTOR_SIN_ARCHIVO` en el motivo y nada en disco;
  - descriptor sin `version`: 422 con `campo = "version"`;
  - campo desconocido: 422 con `DESCRIPTOR_CAMPO_DESCONOCIDO`;
  - rango `>=2.x<`: 422 con `RANGO_INVALIDO`;
  - `core: otro-core` en la carpeta `synkra-core`: 422 `CORE_EQUIVOCADO`;
  - versión repetida con otros bytes: 409, el archivo y la fila originales quedan intactos (se compara el sha256 leído de vuelta);
  - etiqueta en conflicto: 422 `ETIQUETA_EN_CONFLICTO` con `campo = "etiquetas.pais"`;
  - la misma etiqueta con el mismo valor: se acepta y se guarda una vez;
  - atomicidad: con un trigger `raise(abort)` sobre `cat_etiqueta`, no queda ni la fila de `cat_plugin` ni el archivo final;
  - `obtenerPlugin` de uno inexistente: 404.
  - JAR con dos `synkra-plugin.yaml` (o dos entradas con el mismo nombre): 422 `RUTA_NO_PERMITIDA` y nada en disco.

- [ ] **CA-2**: el descriptor se lee con U01, sin copia
  ```bash
  grep -rn 'LectorDescriptor.leer' backend/src/main/java/io/synkra/catalogo/
  grep -rn 'synkra-plugin.yaml\|snakeyaml' backend/src/main/java/io/synkra/catalogo/ || echo "sin copia"
  ```
  Esperado: aparece la llamada, y la segunda línea imprime `sin copia`.

- [ ] **CA-3**: todo el backend y las fronteras siguen pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check :verificador:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. `InterfazCatalogoTest` sigue pasando.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado: `backend/src/main/java/io/synkra/catalogo/**`, las pruebas nuevas, la bitácora y `revisiones/U04-T06/`.

---

## Plan de pruebas

- `SubirPluginTest` (`@SpringBootTest` con `@TempDir`) arma los JAR en memoria con un `synkra-plugin.yaml` generado con `LectorDescriptor.aYaml` o escrito a mano para los casos inválidos. Registra el core con `registrarCore`, llama a `CatalogoApi` y lee de vuelta con `JdbcTemplate` y `Files`.

**Rojo primero:** se escribe la prueba con `subirPlugin` sin implementar y se registra el fallo de CA-1.

---

## Notas

- Los `motivo` propios van en `MotivosCatalogo`, y una prueba fija sus textos.
- El `grep` de CA-2 es una ayuda. El revisor vigila la copia con mutaciones (C-37).
