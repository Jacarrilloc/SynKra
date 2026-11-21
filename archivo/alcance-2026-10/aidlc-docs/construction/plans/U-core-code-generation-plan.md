# Plan de Code Generation — U-core

**Estado:** Part 1 (Planning). Este plan es la fuente única de verdad para la Parte 2. En este
trabajo **no se ejecuta la Parte 2**: no se escribe ningún archivo de código.
**Diseño previo:** `aidlc-docs/construction/U-core/functional-design/` (entidades, reglas,
flujos), aprobado. NFR Requirements, NFR Design e Infrastructure Design se omiten para esta unidad.

## Contexto de la unidad

| Campo | Valor |
|---|---|
| Propósito | Kernel: contratos de extensión, modelo y lector del manifiesto, cargador y registro de plugins, guardia de país, API de KYC sin estado, `GET /plugins` y health |
| Historias | US-2.2, US-2.3, US-2.4, US-2.5 |
| Requisitos | RF-01 a RF-07, NFR-02 |
| Depende de | `U-scaffold`: `pom.xml` raíz, esqueleto `core/pom.xml`, `LLM_PROVIDER=fake` en la suite |
| Lo usan | `U-plugins` y `U-agent` (JAR plano), `U-deploy` (JAR ejecutable) |
| Propietario de | `core/` |
| Raíz del workspace | `C:\Users\julia\OneDrive\Documents\Universidad\2026-3\Topicos Avnzados en Informatica\SynKra`; los comandos se ejecutan en la raíz del repositorio, en Linux o WSL2 |

## Interfaces y contratos que entrega

| Entrega | Consumidor | Contrato |
|---|---|---|
| JAR plano `core` (`com.synkra.core.spi`, `com.synkra.core.manifest`) | U-plugins, U-agent | Tres interfaces de extensión, tipos de entrada y salida, enumeraciones, modelo y lector del manifiesto. Sin Spring en la API pública |
| JAR ejecutable de Spring Boot (clasificador `boot`) | U-deploy | `java -jar` con `synkra.country` y `synkra.plugins.dir` |
| HTTP | Clientes y U-deploy | `/kyc/{country}/...`, `/plugins`, `/actuator/health` |

## Estructura de código (rutas exactas)

```
core/pom.xml
core/README.md
core/src/main/java/com/synkra/core/spi/        DocumentValidator, ConsentPolicy, RetentionPolicy,
                                               DocumentRequest, ValidationResult,
                                               ConsentRequirements, RetentionRule, DataCategory
core/src/main/java/com/synkra/core/manifest/   CountryCode, ExtensionPoint, Topic,
                                               ExtensionPointBinding, PluginManifest,
                                               ManifestReader, ManifestException
core/src/main/java/com/synkra/core/plugin/     PluginLoader, LoadedPlugin, PluginRegistry,
                                               PluginSummary, PluginLoadException
core/src/main/java/com/synkra/core/web/        CountryGuard, KycController, PluginsController,
                                               ErrorResponse, ApiExceptionHandler
core/src/main/java/com/synkra/core/config/     KernelProperties, KernelStartupValidator
core/src/main/java/com/synkra/core/KernelApplication.java
core/src/main/resources/application.yaml
core/src/test/java/com/synkra/core/...         (una clase de prueba por clase principal)
core/src/test/java/com/synkra/core/fixtures/   PluginJarBuilder y plugins de prueba xx / yy
aidlc-docs/construction/U-core/code/summary.md
```

Países de prueba: solo `xx` e `yy` (BR-I2). Ninguna ruta de `core/src` contiene nombres de país.

## Pasos de generación

Cada paso termina con el comando que demuestra que está hecho (AUTONOMIA-03). Los comandos se
ejecutan en la raíz del repositorio.

- [ ] **Paso 1 — `core/pom.xml`.**
  Reemplazar el esqueleto: parent `com.synkra:synkra`; dependencias `spring-boot-starter-web`, `spring-boot-starter-actuator`, `jackson-dataformat-yaml`, y de prueba `spring-boot-starter-test`. Configurar el plugin de Spring Boot con `classifier=boot` para que el artefacto principal siga siendo el JAR plano. **Sin** dependencias de datos, JDBC ni JPA.
  *Verificación:* `mvn -q -pl core package -DskipTests && ls core/target/core-*-boot.jar core/target/core-0.1.0-SNAPSHOT.jar` lista los dos JAR, y `grep -nE "spring-boot-starter-data|jdbc|jpa" core/pom.xml` no imprime nada.

- [ ] **Paso 2 — Contratos de extensión y tipos (`spi`).**
  Crear las tres interfaces, `DocumentRequest`, `ValidationResult`, `ConsentRequirements`, `RetentionRule` y `DataCategory` (`IDENTITY_DATA`, `CONSENT_RECORD`, `KYC_FILE`) según `domain-entities.md`. Cada interfaz declara el país del plugin. *Historia US-2.2.*
  *Verificación:* `grep -rlE "interface (DocumentValidator|ConsentPolicy|RetentionPolicy)" core/src/main | wc -l` imprime `3` y `mvn -q -pl core compile` termina en `0`.

- [ ] **Paso 3 — Modelo y lector del manifiesto (`manifest`).**
  Crear `CountryCode` (normaliza y valida dos letras), `ExtensionPoint`, `Topic`, `ExtensionPointBinding`, `PluginManifest` y `ManifestReader` con las reglas BR-M1 a BR-M7. Pruebas `ManifestReaderTest`: manifiesto válido, y un caso por cada violación (campo ausente, `id` con formato inválido, país de tres letras, punto repetido, punto faltante, tema fuera del conjunto cerrado, tema repetido, lista de temas vacía). *Historia US-2.8 (aporta el lector).*
  *Verificación:* `mvn -q -pl core test -Dtest=ManifestReaderTest` termina en `0` y `grep -h "Tests run" core/target/surefire-reports/*ManifestReaderTest.txt` muestra al menos 9 pruebas sin fallos.

- [ ] **Paso 4 — Fixtures de plugins de prueba (`fixtures`).**
  Crear `PluginJarBuilder` en el árbol de pruebas: arma JAR temporales con clases de prueba de los países ficticios `xx` e `yy`, su `plugin.yaml` y `META-INF/services`. Variantes: consistente, sin manifiesto, con clase que no aparece, país duplicado, `id` duplicado, clase duplicada entre dos JAR.
  *Verificación:* `mvn -q -pl core test -Dtest=PluginJarBuilderTest` termina en `0` (la prueba construye cada variante y comprueba el contenido del JAR con `unzip -l` equivalente).

- [ ] **Paso 5 — Cargador de plugins (`plugin`).**
  Crear `PluginLoader` con BR-L1 a BR-L8: orden alfabético, cargador de clases único, ligadura de cada punto con su clase, comprobación de duplicados, fail-fast con `PluginLoadException` que nombra JAR y motivo, directorio vacío o inexistente como registro vacío. *Historia US-2.3.*
  *Verificación:* `mvn -q -pl core test -Dtest=PluginLoaderTest` termina en `0`; la clase tiene una prueba por cada regla BR-L1 a BR-L8 (`grep -c "void .*BR_L" core/src/test/java/com/synkra/core/plugin/PluginLoaderTest.java` imprime al menos `8`).

- [ ] **Paso 6 — Registro y resumen (`plugin`).**
  Crear `LoadedPlugin`, `PluginRegistry` (búsqueda por país, `all()`) y `PluginSummary`. Orden por `id`. *Historia US-2.3.*
  *Verificación:* `mvn -q -pl core test -Dtest=PluginRegistryTest` termina en `0` e incluye la prueba de orden por `id` y la de registro vacío.

- [ ] **Paso 7 — Configuración y validación de arranque (`config`).**
  Crear `KernelProperties` (`synkra.country`, `synkra.plugins.dir`) y `KernelStartupValidator`: BR-C1 a BR-C3. Un fallo del cargador termina el proceso con código distinto de cero.
  *Verificación:* `mvn -q -pl core test -Dtest=KernelPropertiesTest,KernelStartupFailureTest` termina en `0`, y `java -jar core/target/core-0.1.0-SNAPSHOT-boot.jar; echo $?` imprime un número distinto de `0` y una línea que contiene `synkra.country`.

- [ ] **Paso 8 — Guardia de país y API de KYC (`web`).**
  Crear `CountryGuard`, `KycController` (validar documento, consultar consentimiento, consultar retención), `ErrorResponse` y `ApiExceptionHandler` con el orden de comprobaciones y los códigos de error del diseño (`invalid_country`, `country_not_served`, `plugin_not_loaded`, `invalid_request`, `invalid_category`, `plugin_error`). *Historia US-2.5.*
  *Verificación:* `mvn -q -pl core test -Dtest=CountryGuardTest,KycControllerTest` termina en `0`, con una prueba por cada código de error y una que comprueba que un documento inválido responde 200 con `valid:false`.

- [ ] **Paso 9 — `GET /plugins` (`web`).**
  Crear `PluginsController`. *Historia US-2.3.*
  *Verificación:* `mvn -q -pl core test -Dtest=PluginsControllerTest` termina en `0` con las pruebas «un plugin listado con tres puntos y versión», «lista vacía con directorio vacío» y «plugins de otro país también se listan».

- [ ] **Paso 10 — Salud.**
  Configurar `application.yaml` para exponer liveness y readiness; la readiness es `UP` al terminar el arranque (BR-H2). *Historia US-2.4.*
  *Verificación:* `mvn -q -pl core test -Dtest=HealthTest` termina en `0`; con el JAR en marcha y un directorio de plugins vacío, `curl -s localhost:8080/actuator/health | grep -c '"status":"UP"'` imprime al menos `1`.

- [ ] **Paso 11 — Comprobación de independencia de país (NFR-02).**
  Crear la prueba `NoCountryWordsTest` que recorre `core/src` y falla si encuentra nombres o códigos de país como palabras sueltas, sin distinguir mayúsculas. *Historia US-2.2.*
  *Verificación:* `mvn -q -pl core test -Dtest=NoCountryWordsTest` termina en `0` y `grep -rniE "colombia|peru|\bCO\b|\bPE\b" core/src` no imprime nada (código de salida `1`).

- [ ] **Paso 12 — Prueba de integración del kernel.**
  Crear `KernelIT` (arranque completo de la aplicación con un JAR fixture `xx`): cubre US-2.3, US-2.4 y US-2.5 de extremo a extremo, incluidos «país distinto del activo da 404 que nombra el país» y «directorio vacío: arranca y `/plugins` devuelve `[]`».
  *Verificación:* `mvn -q -pl core verify` termina en `0` y `grep -h "Tests run" core/target/surefire-reports/*KernelIT.txt` muestra 0 fallos.

- [ ] **Paso 13 — Sin estado ni persistencia (RF-05).**
  Comprobar el árbol de dependencias y que ninguna clase de `core` usa almacenamiento persistente.
  *Verificación:* `mvn -pl core dependency:tree | grep -ciE "jdbc|jpa|spring-data|hibernate"` imprime `0`.

- [ ] **Paso 14 — Documentación del módulo.**
  Escribir `core/README.md`: formato del `plugin.yaml` (con ejemplo de país ficticio), propiedades de configuración, endpoints y códigos de error.
  *Verificación:* `grep -c "plugin.yaml" core/README.md` imprime al menos `1` y `grep -c "synkra.country" core/README.md` imprime al menos `1`.

- [ ] **Paso 15 — Resumen de la unidad.**
  Escribir `aidlc-docs/construction/U-core/code/summary.md` (solo Markdown) con los archivos creados y la correspondencia pasos a historias.
  *Verificación:* `test -s aidlc-docs/construction/U-core/code/summary.md && echo OK` imprime `OK`.

- [ ] **Paso 16 — Comprobación final de la unidad.**
  Ejecutar la suite completa del módulo y revisar que solo cambiaron rutas de la unidad.
  *Verificación:* `mvn -q -pl core verify && ! grep -rniE "colombia|peru|\bCO\b|\bPE\b" core/src && git status --porcelain | grep -vE "^( M|\?\?) (core/|aidlc-docs/)"` termina con la última parte sin imprimir nada.

## Cobertura de historias

| Historia | Pasos | Criterio de la historia cubierto |
|---|---|---|
| US-2.2 | 2, 11, 16 | `grep` sin resultados en `core/src`; tres interfaces |
| US-2.3 | 4, 5, 6, 9, 12 | `/plugins` lista el plugin con tres puntos y versión; con directorio vacío devuelve lista vacía y el kernel sigue arriba |
| US-2.4 | 10, 12 | `/actuator/health` contiene `UP`. El criterio de probes en el Deployment lo verifica `U-deploy` |
| US-2.5 | 8, 12, 13 | Documento válido da 200; otro país da 404 que nombra el país; sin dependencias de base de datos |

Cada historia de la unidad aparece en al menos un paso. Requisitos: RF-01 (2, 11), RF-02 (5), RF-03 (6, 9), RF-04 y RF-06 (8), RF-05 (1, 13), RF-07 (10), NFR-02 (11).

## Secciones estándar que no aplican a esta unidad

| Sección estándar | Motivo |
|---|---|
| Repository Layer | El kernel es sin estado (RF-05) |
| Frontend Components | No hay interfaz de usuario |
| Database Migration Scripts | No hay modelo de datos persistente |
| Deployment Artifacts | Corresponden a `U-deploy` (imagen, probes en el Deployment) |

## Cumplimiento de reglas

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. Ningún paso hace `merge`, `push` a `main`, `kubectl apply` ni `helm install`; el kernel no tiene agente |
| AUTONOMIA-02 | Cumple. Ningún paso usa un modelo de lenguaje; la suite corre con `LLM_PROVIDER=fake` heredado de la raíz |
| AUTONOMIA-03 | Cumple. Los 16 pasos nombran su comando de verificación |

## Resumen del alcance

16 pasos; 1 `pom.xml`, 1 `application.yaml`, 1 `README.md`, aproximadamente 28 clases principales
y 14 clases de prueba. Sin base de datos, sin manifiestos de Kubernetes.
