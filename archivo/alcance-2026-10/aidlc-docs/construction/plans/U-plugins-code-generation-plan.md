# Plan de Code Generation — U-plugins

**Estado:** Part 1 (Planning). Este plan es la fuente única de verdad para la Parte 2. En este
trabajo **no se ejecuta la Parte 2**: no se escribe ningún archivo de código.
**Diseño previo:** `aidlc-docs/construction/U-plugins/functional-design/` (aprobado).
NFR Requirements, NFR Design e Infrastructure Design se omiten para esta unidad.

## Contexto de la unidad

| Campo | Valor |
|---|---|
| Propósito | Plugins `co` y `pe`: implementan los tres puntos de extensión, traen manifiesto y catálogo de reglas, y se empaquetan como JAR independientes |
| Historias | US-2.6, US-2.7, US-2.8, US-2.9, US-2.10 |
| Requisitos | RF-10, RF-11, RF-12, RF-13 |
| Depende de | `U-scaffold` (POM raíz) y `U-core` (JAR plano con `spi` y `manifest`) |
| Lo usan | `U-agent` (lee `plugin.yaml`, escribe en `changes/`) y `U-deploy` (empaqueta los JAR en la imagen `synkra/plugins`) |
| Propietario de | `plugins/` |
| Estado de las reglas | Las 14 reglas son provisionales (`verificar`). La verificación es el paso 14 |
| Raíz del workspace | `C:\Users\julia\OneDrive\Documents\Universidad\2026-3\Topicos Avnzados en Informatica\SynKra`; comandos desde la raíz del repositorio, en Linux o WSL2 |

**Convención de comandos.** Para compilar un plugin junto con `core` se usa
`PL="mvn -q -am -Dsurefire.failIfNoSpecifiedTests=false"`. Por ejemplo `$PL -pl plugins/pe test -Dtest=PeruManifestTest`.

## Interfaces y contratos que entrega

| Entrega | Consumidor | Contrato |
|---|---|---|
| `plugins/co/target/co-*.jar` y `plugins/pe/target/pe-*.jar` | U-deploy | JAR sin clases de `core` (`core` en alcance `provided`), con `plugin.yaml` y `META-INF/services` |
| `plugin.yaml` de cada plugin | U-core (carga), U-agent (inventario) | País, versión, tres puntos con `implementationClass` y `sourceFile`, temas `document`, `consent`, `retention` |
| Directorios `plugins/co/changes/` y `plugins/pe/changes/` | U-agent | Reciben los archivos de solicitud de cambio; ni el plugin ni el kernel los leen |

## Estructura de código (rutas exactas)

```
plugins/pom.xml                                      (agregador de co y pe)
plugins/README.md                                    (reglas provisionales y cómo verificarlas)
plugins/co/pom.xml
plugins/co/src/main/java/com/synkra/plugin/colombia/
    ColombiaDocumentValidator, ColombiaConsentPolicy, ColombiaRetentionPolicy,
    ColombiaRuleCatalog, Rule
plugins/co/src/main/resources/plugin.yaml
plugins/co/src/main/resources/META-INF/services/
    com.synkra.core.spi.DocumentValidator, ...ConsentPolicy, ...RetentionPolicy
plugins/co/src/test/java/com/synkra/plugin/colombia/...
plugins/co/changes/.gitkeep
plugins/pe/...                                       (misma estructura, paquete com.synkra.plugin.peru, clases Peru*)
aidlc-docs/construction/U-plugins/code/summary.md
```

`Rule` se declara en cada plugin y no se comparte (BR-I1): se acepta la duplicación para que
los plugins sigan siendo independientes.

## Pasos de generación

Cada paso termina con el comando que demuestra que está hecho (AUTONOMIA-03).

- [ ] **Paso 1 — POM del agregador y de los plugins.**
  Crear `plugins/pom.xml` (packaging pom, módulos `co` y `pe`) y reemplazar los esqueletos `plugins/co/pom.xml` y `plugins/pe/pom.xml`: parent `com.synkra:synkra`, dependencia de `com.synkra:core` (JAR plano) en alcance `provided`, y de prueba JUnit 5. Ninguna dependencia entre `co` y `pe`. *Historia US-2.10.*
  *Verificación:* `$PL -pl plugins/co,plugins/pe package -DskipTests` termina en `0` y `unzip -l plugins/pe/target/pe-*.jar | grep -c "com/synkra/core/"` imprime `0` (y lo mismo con `co`).

- [ ] **Paso 2 — Catálogo de reglas de Colombia.**
  Crear `Rule` (id, kind, description, source, verificar, provisionalNote) y `ColombiaRuleCatalog` con las 7 reglas `CO-DOC-001..003`, `CO-CON-001`, `CO-RET-001..003`, todas con `verificar` y `source` como fuente prevista. Prueba `ColombiaRuleSourceTest`: BR-S1 a BR-S3, incluida una regla sintética verificada sin enlace que debe fallar. La prueba escribe `plugins/co/target/rules-report.txt` con una línea `<id> verificar=<valor>` por regla. *Historia US-2.9.*
  *Verificación:* `$PL -pl plugins/co test -Dtest=ColombiaRuleSourceTest` termina en `0` y `grep -c "verificar=true" plugins/co/target/rules-report.txt` imprime `7`.

- [ ] **Paso 3 — Validador de documento de Colombia.**
  Crear `ColombiaDocumentValidator` con BR-D1 a BR-D8 (tipo `CC`, solo dígitos, 6 a 10 dígitos, `ruleRefs` `CO-DOC-001..003`). Pruebas de la tabla de decisión: tipo no soportado, válido, formato, longitud, ambos, tipo en minúsculas, número con espacios. Las pruebas llevan `@Tag("verificar")` porque ejercen reglas provisionales (BR-T2). *Historia US-2.6.*
  *Verificación:* `$PL -pl plugins/co test -Dtest=ColombiaDocumentValidatorTest` termina en `0` con al menos 7 pruebas (`grep -h "Tests run" plugins/co/target/surefire-reports/*ColombiaDocumentValidatorTest.txt`).

- [ ] **Paso 4 — Consentimiento y retención de Colombia.**
  Crear `ColombiaConsentPolicy` (BR-CS1 a BR-CS4: cuatro campos candidatos, finalidad obligatoria, `ruleRefs` `CO-CON-001`) y `ColombiaRetentionPolicy` (BR-R1 a BR-R3: 5 años para las tres categorías, `ruleRefs` por categoría). Pruebas con `@Tag("verificar")`. *Historia US-2.6.*
  *Verificación:* `$PL -pl plugins/co test -Dtest=ColombiaConsentPolicyTest,ColombiaRetentionPolicyTest` termina en `0`; la prueba de retención cubre las tres categorías.

- [ ] **Paso 5 — Manifiesto, servicios y carga real de Colombia.**
  Crear `plugin.yaml` (`id: co-kyc`, `country: co`, tres puntos con `sourceFile`, temas `document`, `consent`, `retention`) y los tres archivos de `META-INF/services`. Pruebas: `ColombiaManifestTest` (lee el manifiesto con el `ManifestReader` de `core`, comprueba que cada clase existe y que un manifiesto con una clase inexistente falla) y `ColombiaPluginLoadTest` (arma un JAR temporal desde `target/classes` y lo carga con el `PluginLoader` de `core`; el registro contiene `co-kyc` con los tres puntos). *Historias US-2.6 y US-2.8.*
  *Verificación:* `$PL -pl plugins/co test -Dtest=ColombiaManifestTest,ColombiaPluginLoadTest` termina en `0`.

- [ ] **Paso 6 — Catálogo de reglas de Perú.**
  Igual que el paso 2 para `pe`: `Rule`, `PeruRuleCatalog` con `PE-DOC-001..003`, `PE-CON-001`, `PE-RET-001..003` y `PeruRuleSourceTest`. *Historia US-2.9.*
  *Verificación:* `$PL -pl plugins/pe test -Dtest=PeruRuleSourceTest` termina en `0` y `grep -c "verificar=true" plugins/pe/target/rules-report.txt` imprime `7`.

- [ ] **Paso 7 — Validador de documento de Perú.**
  Crear `PeruDocumentValidator` con BR-D1 a BR-D8 (tipo `DNI`, solo dígitos, exactamente 8 dígitos, `ruleRefs` `PE-DOC-001..003`). Pruebas con las cinco entradas del ejemplo de `business-rules.md` y con `@Tag("verificar")`. *Historia US-2.7.*
  *Verificación:* `$PL -pl plugins/pe test -Dtest=PeruDocumentValidatorTest` termina en `0` con al menos 7 pruebas.

- [ ] **Paso 8 — Consentimiento y retención de Perú.**
  Crear `PeruConsentPolicy` (cinco campos candidatos, finalidad obligatoria, `PE-CON-001`) y `PeruRetentionPolicy` (5 años, tres categorías). Pruebas con `@Tag("verificar")`. *Historia US-2.7.*
  *Verificación:* `$PL -pl plugins/pe test -Dtest=PeruConsentPolicyTest,PeruRetentionPolicyTest` termina en `0`.

- [ ] **Paso 9 — Manifiesto, servicios y carga real de Perú.**
  Igual que el paso 5 con `id: pe-kyc`, `country: pe`, `PeruManifestTest` y `PeruPluginLoadTest`. *Historias US-2.7 y US-2.8.*
  *Verificación:* `$PL -pl plugins/pe test -Dtest=PeruManifestTest,PeruPluginLoadTest` termina en `0`.

- [ ] **Paso 10 — Independencia entre países (BR-I1, BR-I3).**
  Comprobar que el código de cada plugin no menciona al otro país. *Historia US-2.10.*
  *Verificación:* `! grep -rniE "peru|\bPE\b|\bDNI\b" plugins/co/src && ! grep -rniE "colombia|\bCO\b|\bCC\b" plugins/pe/src && echo OK` imprime `OK`.

- [ ] **Paso 11 — Empaquetado independiente.**
  Comprobar que el árbol de dependencias de cada plugin no incluye al otro y que se generan dos JAR. *Historia US-2.10.*
  *Verificación:* `mvn -q -pl plugins/co dependency:tree | grep -c "plugins/pe\|:pe:"` imprime `0`, igual para `pe` con `co`, y `mvn -q package -DskipTests && ls plugins/co/target/*.jar plugins/pe/target/*.jar` lista dos archivos.

- [ ] **Paso 12 — Separación de pruebas de mecánica y de reglas provisionales (BR-T1, BR-T2).**
  Comprobar que las pruebas que ejercen reglas provisionales llevan `@Tag("verificar")` y que la suite de aceptación de mecánica corre sin ellas. *Historia US-2.9.*
  *Verificación:* `grep -rln '@Tag("verificar")' plugins/*/src/test | wc -l` imprime al menos `6`; `mvn -q -pl plugins/co,plugins/pe -am test -Dgroups='!verificar' -Dsurefire.failIfNoSpecifiedTests=false` termina en `0`; `mvn -q -pl plugins/co,plugins/pe -am test -Dgroups=verificar -Dsurefire.failIfNoSpecifiedTests=false` termina en `0`.

- [ ] **Paso 13 — README del módulo y directorios `changes/`.**
  Escribir `plugins/README.md`: las reglas son provisionales, lista de las 14 con su fuente prevista, el 5 de retención es arbitrario, y cómo se verifica una regla (flujo 4 del diseño). Confirmar que existen `plugins/co/changes/` y `plugins/pe/changes/`. *Historias US-2.9 y BR-T3.*
  *Verificación:* `grep -ci "provisional" plugins/README.md` imprime al menos `3` y `test -d plugins/co/changes && test -d plugins/pe/changes && echo OK` imprime `OK`.

- [ ] **Paso 14 — Verificación de fuentes primarias (BR-S5).**
  Para cada una de las 14 reglas: consultar la fuente oficial, completar `source` con enlace `https://` y la fecha consultada, y pasar `verificar` a `false`; o corregir el valor; o eliminar la regla o el campo sin respaldo (BR-S6, BR-S7), consultando al usuario cuando la norma no fije un plazo. Este paso puede dejar reglas pendientes; lo que quede sin verificar se informa. *Historia US-2.9.*
  *Verificación:* `$PL -pl plugins/co,plugins/pe test -Dtest=*RuleSourceTest` termina en `0` y `cat plugins/co/target/rules-report.txt plugins/pe/target/rules-report.txt | grep -c "verificar=false"` imprime el número de reglas verificadas; el informe final lista cada regla pendiente con `grep "verificar=true"`.

- [ ] **Paso 15 — Resumen de la unidad.**
  Escribir `aidlc-docs/construction/U-plugins/code/summary.md` (solo Markdown) con los archivos creados, la correspondencia pasos a historias y el estado de verificación de las reglas.
  *Verificación:* `test -s aidlc-docs/construction/U-plugins/code/summary.md && echo OK` imprime `OK`.

- [ ] **Paso 16 — Comprobación final de la unidad.**
  Ejecutar la suite completa de los dos módulos y revisar que solo cambiaron rutas de la unidad.
  *Verificación:* `mvn -q -pl plugins/co,plugins/pe -am verify -Dsurefire.failIfNoSpecifiedTests=false && git status --porcelain | grep -vE "^( M|\?\?) (plugins/|aidlc-docs/)"` termina con la última parte sin imprimir nada.

## Cobertura de historias

| Historia | Pasos | Criterio de la historia cubierto |
|---|---|---|
| US-2.6 | 3, 4, 5 | El overlay `co` lista solo `co` con los tres puntos (la prueba de carga lo comprueba antes de desplegar); `mvn -pl plugins/co test` pasa |
| US-2.7 | 7, 8, 9 | Ídem para `pe` |
| US-2.8 | 5, 9 | `ManifestTest` pasa; falla con una clase ausente |
| US-2.9 | 2, 6, 12, 13, 14 | `RuleSourceTest` pasa; falla sin `source` ni `verificar`; las `verificar` quedan etiquetadas y excluidas de aceptación |
| US-2.10 | 1, 10, 11 | Árbol de dependencias sin el otro país; dos JAR |

Los criterios que dependen de un clúster (`/plugins` en el overlay) se verifican en `U-deploy`;
aquí se cubre su equivalente con la carga real del JAR por el cargador de `core`.

## Secciones estándar que no aplican

| Sección estándar | Motivo |
|---|---|
| API Layer | Los plugins no exponen HTTP; lo hace el kernel |
| Repository Layer, Base de datos, Frontend | No hay persistencia ni interfaz |
| Deployment Artifacts | Corresponden a `U-deploy` (imagen `synkra/plugins`) |

## Cumplimiento de reglas

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. Ningún paso hace `merge`, `push` a `main`, `kubectl apply` ni `helm install` |
| AUTONOMIA-02 | Cumple. Ningún paso usa un modelo de lenguaje; la suite corre con `LLM_PROVIDER=fake` heredado de la raíz |
| AUTONOMIA-03 | Cumple. Los 16 pasos nombran su comando de verificación |

## Resumen del alcance

16 pasos; 3 POM, 14 clases principales por país más `Rule`, 2 `plugin.yaml`, 6 archivos de
servicios, aproximadamente 16 clases de prueba, 1 `README.md`. Sin HTTP, sin manifiestos de
Kubernetes, sin llamadas de red.
