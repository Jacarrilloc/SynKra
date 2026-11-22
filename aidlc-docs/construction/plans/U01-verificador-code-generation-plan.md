# Plan de tareas: U01 Verificador y suite

> **Parte 1 de Code Generation.** Este plan es la única fuente de verdad para escribir el código
> de U01. **No se ejecuta en el módulo 5**: la Parte 2 corre después, solo con aprobación y
> avisando a Julian antes de escribir código.

Raíz del proyecto (greenfield). Crea la raíz de Gradle y el proyecto `verificador/`. Código en la
raíz; el resumen va en `aidlc-docs/construction/U01-verificador/code/`.

## Contexto

- **Historias dueñas:** H-07, H-08, H-10. **Contribuye a:** H-02, H-03, H-21.
- **Depende de:** ninguna unidad. **Expone:** `Version`, `RangoVersion`, `LectorDescriptor`,
  `Verificador`, la interfaz `Catalogo` y, en `testFixtures`, `SuiteCombinaciones`.
- **Diseño:** `../U01-verificador/functional-design/` (reglas R-V1 a R-V6, suite S1 a S12, 5 propiedades).
- **Entidades de base de datos:** ninguna (U01 no persiste).

## Tareas

- [ ] **U01-T01 · Raíz de Gradle**
  `settings.gradle.kts`, `build.gradle.kts` con convenciones comunes (Java 21, JUnit 5) y el
  *wrapper* de Gradle; incluye `verificador`.
  **Criterio:** `./gradlew projects` lista `:verificador` y `./gradlew --version` muestra JVM 21. · H-07

- [ ] **U01-T02 · Proyecto `verificador/` sin dependencias de framework**
  `verificador/build.gradle.kts` con `java-library` y `java-test-fixtures`; dependencias: un
  lector YAML seguro (SnakeYAML Engine) y, solo en pruebas, JUnit 5 y jqwik.
  **Criterio:** `./gradlew :verificador:dependencies --configuration runtimeClasspath | grep -ciE "spring|fabric8"` devuelve `0`. · H-07

- [ ] **U01-T03 · `Version` (R-V1) y sus pruebas**
  Record con `parsear`, orden y `toString`; rechaza pre-release y ceros a la izquierda. Crea también
  `Resultado<T>`, `Motivo` y la enumeración `CodigoMotivo` con los 9 códigos, porque `parsear` ya
  devuelve `VERSION_INVALIDA`.
  **Criterio:** `./gradlew :verificador:test --tests '*VersionTest'` pasa, con casos `2.1.0`, `2.01.0`, `2.1.0-rc1`, `2.1`. · H-07

- [ ] **U01-T04 · `RangoVersion` (R-V2) y sus pruebas**
  Las cuatro formas reducidas a `[minimo, maximo)`; `incluye`.
  **Criterio:** `./gradlew :verificador:test --tests '*RangoVersionTest'` pasa con la tabla de R-V2 y los rangos inválidos (`>=3.0.0 <2.0.0`, `2.x.x`). · H-07

- [ ] **U01-T05 · Propiedades de rangos (PBT-03, PBT-07, PBT-08)**
  Generadores de versiones y rangos; propiedades "`N.x` incluye ⇔ mismo mayor" e ida y vuelta de
  `parsear(r.texto())`; semilla registrada en el reporte.
  **Criterio:** `./gradlew :verificador:test --tests '*RangoVersionPropiedades'` pasa con 1000 intentos por propiedad. · H-07

- [ ] **U01-T06 · `Descriptor` y `LectorDescriptor` (R-V3)**
  Lectura segura de `synkra-plugin.yaml` desde un JAR; descriptor estricto; todos los motivos de
  una vez; `aYaml`/`desdeYaml`.
  **Criterio:** `./gradlew :verificador:test --tests '*LectorDescriptorTest'` pasa con: JAR sin archivo, campo faltante, campo desconocido, YAML con etiqueta de tipo (rechazado) y descriptor válido. · H-03

- [ ] **U01-T07 · Propiedad de ida y vuelta del descriptor (PBT-02)**
  **Criterio:** `./gradlew :verificador:test --tests '*DescriptorPropiedades'` pasa con 1000 intentos. · H-03

- [ ] **U01-T08 · Modelo del veredicto y códigos de motivo**
  `Seleccion`, `Alternativa`, `VeredictoPlugin` y `Veredicto` (`Motivo` y `CodigoMotivo` nacen en T03).
  **Criterio:** `./gradlew :verificador:test --tests '*CodigoMotivoTest'` pasa y afirma que la enumeración tiene exactamente los 9 códigos de `domain-entities.md`. · H-07

- [ ] **U01-T09 · `Verificador.verificar` (R-V4) y la interfaz `Catalogo`**
  Orden de comprobaciones de R-V4, determinista.
  **Criterio:** `./gradlew :verificador:test --tests '*VerificadorTest'` pasa con un caso por código (`PLUGIN_REPETIDO`, `PLUGIN_AUSENTE`, `CORE_EQUIVOCADO`, `VERSION_INCOMPATIBLE`) y la selección vacía aprobada. · H-07

- [ ] **U01-T10 · `Verificador.alternativas` (R-V5)**
  Más alta del plugin compatible; más baja del core que acepta el plugin; "no hay" explícito.
  **Criterio:** `./gradlew :verificador:test --tests '*AlternativasTest'` pasa con S4 y S5 de la suite. · H-08

- [ ] **U01-T11 · Propiedades del verificador (PBT-03)**
  Determinismo y "toda alternativa aplicada da APROBADA".
  **Criterio:** `./gradlew :verificador:test --tests '*VerificadorPropiedades'` pasa con 1000 intentos. · H-08

- [ ] **U01-T12 · Suite de combinaciones en `testFixtures` (S1 a S12)**
  `SuiteCombinaciones.casos()` con el catálogo fijo y los resultados esperados; S11 y S12
  declarados y remitidos a U04, U05 y U02.
  **Criterio:** `./gradlew :verificador:test --tests '*SuiteCombinacionesTest'` pasa los 10 casos ejecutables. · H-10

- [ ] **U01-T13 · La suite bloquea el build**
  `check` depende de la suite; una prueba de control demuestra que alterar un resultado esperado
  hace fallar el build.
  **Criterio:** `./gradlew :verificador:check` = 0; con el caso S4 alterado en una rama de prueba, `./gradlew :verificador:check` ≠ 0. · H-10

- [ ] **U01-T14 · Resumen de la unidad**
  `aidlc-docs/construction/U01-verificador/code/summary.md`: qué expone, cómo lo usan U02, U04 y
  U07, y la salida real de `./gradlew :verificador:check`.
  **Criterio:** el resumen existe y contiene la salida pegada de `./gradlew :verificador:check`. · H-10

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-07 Una regla, en una librería | T01 a T05, T08, T09 |
| H-08 Explicar por qué no sirve | T10, T11 |
| H-10 La suite bloquea el build | T12, T13, T14 |
| H-03 (contribuye) Descriptor inválido | T06, T07 |

**Terminada cuando:** `./gradlew :verificador:check` pasa y la comprobación de T02 da `0`.

## Notas

- Las pruebas se escriben en cada tarea, no se aplazan a Build and Test.
- Ninguna tarea toca un clúster ni la red (AUTONOMIA-01: N/A).
