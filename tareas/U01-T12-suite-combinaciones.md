# U01-T12: Suite de combinaciones en `testFixtures` (S1 a S12)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-10
**Depende de:** U01-T11

---

## Alcance

**Dentro** (una línea, concreta):

> `SuiteCombinaciones` en `verificador/src/testFixtures` con `catalogo()` (el catálogo fijo del diseño) y `casos()` (S1 a S12 con su entrada y su resultado esperado exacto), y `SuiteCombinacionesTest`, que corre los 10 casos ejecutables contra `Verificador` y `LectorDescriptor`. Cierra C-26.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `src/main`. Si un caso no da lo que dice el diseño, se registra en la bitácora con la salida y se para: el orquestador decide qué lado está mal.
- Que `check` falle si la suite se salta o se filtra (U01-T13).
- Ejecutar S11 (integridad: U04 y U05) o S12 (plugin no activado: U02). Aquí solo se declaran.
- Migrar `VerificadorTest` o `AlternativasTest` a la suite: siguen con sus propias constantes.
- Nuevas dependencias.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (catálogo de la suite y tabla S1–S12)
- `Modulo 3/specs/prd.md` §11.1 (los 7 grupos)
- `aidlc-docs/inception/application-design/component-methods.md` (`SuiteCombinaciones.casos()`)
- `verificador/src/main/java/io/synkra/verificador/` (`Verificador`, `LectorDescriptor`, records)
- `verificador/src/testFixtures/java/io/synkra/verificador/CatalogoEnMemoria.java`
- `verificador/src/test/java/io/synkra/verificador/VerificadorPropiedades.java` (C-26)
- `CANDIDATAS.md` (C-26)

---

## Decisiones del orquestador

1. **Forma.** `SuiteCombinaciones` es `public final` con constructor privado. `Caso` es un record público
   con `id` (`"S1"` a `"S12"`), `grupo` (uno de los 7 del PRD §11.1, como enum `Grupo`), `descripcion`,
   `entrada` y `esperado`. La entrada es una de tres variantes de una interfaz sellada:
   - `PorSeleccion(Seleccion seleccion)`: S1 a S7 y S10;
   - `PorDescriptor(String yaml)`: S8 y S9, el texto de `synkra-plugin.yaml`;
   - `FueraDeU01(String remiteA)`: S11 y S12, con la unidad y la prueba que lo cubren.

   `esperado` dice el `Global` (o el rechazo del lector), la lista **exacta** de códigos en orden y,
   para S4 y S5, la lista exacta de alternativas.
2. **S8 y S9** se comprueban dos veces: con `LectorDescriptor.desdeYaml(yaml)` y con
   `LectorDescriptor.leer(jar)` sobre un JAR construido en un `@TempDir` que lleva ese YAML como
   `synkra-plugin.yaml`. Es lo que hará U02 con los JAR reales.
3. **Los datos de la suite viven solo en `SuiteCombinaciones`.** `casos()` y `catalogo()` devuelven
   listas inmutables y un catálogo nuevo en cada llamada.
4. **C-26.** `VerificadorPropiedades.catalogoDeLaSuite()` pasa a construirse con
   `SuiteCombinaciones.catalogo()`. Además, `catalogosDeTexto` registra una versión de core para
   `core-b`, de modo que el caso «solo en otra carpeta» salga también con un core registrado.
   Hay que mantener los umbrales y la estabilidad de U01-T11, y medirlo (CA-5).

---

## Criterios de aceptación

- [ ] **CA-1**: la suite pasa, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*SuiteCombinacionesTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `skipped="0" failures="0" errors="0"`. Debe haber una prueba
  parametrizada con exactamente **10** ejecuciones, una por caso ejecutable y con el `id` en su
  nombre visible. Aparte van las pruebas de forma:
  - hay 12 casos, con ids `S1` a `S12` en orden y sin repetidos;
  - aparecen los 7 grupos del PRD;
  - S11 y S12 son `FueraDeU01`: S11 nombra U04 y U05, y S12 nombra U02;
  - `casos()` devuelve una lista que no se puede modificar;
  - dos llamadas a `catalogo()` dan objetos distintos con el mismo contenido.

- [ ] **CA-2**: cada caso afirma el resultado exacto del diseño
  ```bash
  ./gradlew :verificador:test --rerun --tests '*SuiteCombinacionesTest'
  ```
  Esperado: `BUILD SUCCESSFUL`, con lo de `business-logic-model.md`:
  - S1, S2 y S3 → `APROBADA` y ningún motivo.
  - S4 → `VERSION_INCOMPATIBLE` en `kyc-pe`, y `alternativas` da exactamente `[PLUGIN kyc-pe 1.3.0,
    CORE synkra-core 3.0.0]`.
  - S5 → `VERSION_INCOMPATIBLE`, y solo `[CORE synkra-core 2.1.0]`.
  - S6 → `PLUGIN_AUSENTE`.
  - S7 → `CORE_EQUIVOCADO`.
  - S10 → un `PLUGIN_REPETIDO` global.
  - S8 → `DESCRIPTOR_FALTA_CAMPO` con `campo = "coreCompatible"`.
  - S9 → `DESCRIPTOR_CAMPO_DESCONOCIDO` (`coreCompatibel`) y `DESCRIPTOR_FALTA_CAMPO`
    (`coreCompatible`), en ese orden.

  La comparación es por la lista de `(código, plugin, campo)`; el mensaje se comprueba con al menos el
  nombre del plugin o del campo.

- [ ] **CA-3**: la suite detecta regresiones. En una copia `git archive HEAD` fuera del worktree, se
  aplica cada mutación por separado, se corre CA-1 y se revierte:
  - (a) se quita S6 de `casos()` → falla la prueba de forma;
  - (b) el descriptor de `kyc-xx` en S7 pasa a declarar `synkra-core` → falla S7;
  - (c) en S9, `coreCompatibel` se corrige a `coreCompatible` → falla S9;
  - (d) en `src/main`, `alternativas` toma la versión de core más alta → falla S4;
  - (e) en `src/main`, `LectorDescriptor` deja de exigir `coreCompatible` → fallan S8 y S9.

  La bitácora pega para cada una el comando, el caso que falla con su mensaje y la vuelta a verde.
  ```bash
  git diff --stat origin/main...HEAD -- verificador/src/main
  ```
  Esperado: vacío.

- [ ] **CA-4**: la suite se puede usar desde otro proyecto
  ```bash
  ./gradlew :verificador:testFixturesJar --rerun
  python3 -c "import zipfile;print('\n'.join(n for n in zipfile.ZipFile('verificador/build/libs/verificador-test-fixtures.jar').namelist() if n.endswith('.class')))"
  ```
  Esperado: el JAR de `testFixtures` trae `SuiteCombinaciones`, `SuiteCombinaciones$Caso` y
  `CatalogoEnMemoria`. Así U02 y U07 podrán usarla con `testFixtures(project(":verificador"))`.

- [ ] **CA-5**: C-26 queda cerrada sin romper U01-T11
  ```bash
  ./gradlew :verificador:test --rerun --tests '*VerificadorPropiedades'
  grep -o -E 'tries = [0-9]+|checks = [0-9]+' verificador/build/test-results/test/TEST-*VerificadorPropiedades.xml | sort | uniq -c
  ```
  Esperado: `BUILD SUCCESSFUL`, 6 × `tries = 1000` y 6 × `checks = 1000`. La bitácora pega una
  medición de 100 lotes de 1000 con `sampleStream()`. Muestra la media y el mínimo de los tres casos
  exigidos por `generadorCubreLosCodigos`, más el caso nuevo «solo en otra carpeta con core
  registrado». Cada caso exigido sigue con una media de al menos 3 veces su umbral. La fila C-26 de
  `CANDIDATAS.md` dice que se cerró en U01-T12.

- [ ] **CA-6**: el verificador sigue sin Spring ni Fabric8 en ejecución
  ```bash
  ./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"
  ```
  Esperado: `0`.

- [ ] **CA-7**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin pruebas saltadas en ningún XML.

- [ ] **CA-8**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado:
  - `SuiteCombinaciones.java` en `src/testFixtures`;
  - `SuiteCombinacionesTest.java` y `VerificadorPropiedades.java` en `src/test`;
  - `CANDIDATAS.md`, la bitácora y `revisiones/U01-T12/`.

  Nada más.

---

## Plan de pruebas

- `SuiteCombinacionesTest`: una `@ParameterizedTest` con `@MethodSource` sobre los casos ejecutables y
  `name = "{0}"` para que el XML muestre el id; las pruebas de forma son casos de ejemplo.
- El JAR de S8 y S9 se construye con `java.util.zip` en un `@TempDir`; sin dependencias nuevas.

**Rojo primero:** se registra CA-1 fallando por compilación (`SuiteCombinaciones` no existe) y luego
pasando; CA-3 da los cinco pares falla-pasa.

---

## Notas

- Paquete: `io.synkra.verificador`.
- Los nombres y versiones de la suite son exactamente los del diseño: `synkra-core` 2.1.0 y 3.0.0;
  `kyc-co 1.0.0` (`2.x`), `kyc-pe 1.3.0` (`2.x`), `kyc-pe 2.0.0` (`3.x`), `kyc-mx 1.0.0` (no está en el
  catálogo); `otro-core 1.0.0` con `kyc-xx 1.0.0` (`1.x`).
- Las selecciones de S1 a S7 y S10 usan `synkra-core`, con 2.1.0 en todas menos S5, que usa 3.0.0.
