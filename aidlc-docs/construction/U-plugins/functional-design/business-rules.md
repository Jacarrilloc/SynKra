# Reglas de negocio — U-plugins

> **Todas las reglas de país son provisionales** (Q7-C, F1-A). Este documento define cómo se
> comporta cada plugin con valores de trabajo, y cómo se registran y comprueban las fuentes. No
> afirma contenido legal. Ver también `domain-entities.md`.

## Reglas de documento

Se aplican en este orden. `T` es el tipo normalizado y `N` el número tal como llegó.

| Id | Regla | Resultado | Origen |
|---|---|---|---|
| BR-D1 | `T` = tipo recortado y en mayúsculas. Si `T` no es el tipo soportado del plugin (`CC` en `co`, `DNI` en `pe`) | `valid: false`, `reasonCodes: [UNSUPPORTED_TYPE]`; no se evalúan formato ni longitud | Q1-C, Q4-A |
| BR-D2 | Si `N` contiene cualquier carácter que no sea un dígito (incluidos espacios, puntos y guiones) | Se añade `FORMAT_INVALID` | Q3-B |
| BR-D3 | Si la longitud de `N` está fuera del rango de la regla (`co`: 6 a 10; `pe`: exactamente 8) | Se añade `LENGTH_INVALID` | Q2-B |
| BR-D4 | BR-D2 y BR-D3 se evalúan los dos; pueden aparecer ambos códigos | — | Determinismo |
| BR-D5 | Si ningún código aplica | `valid: true`, `reasonCodes: []` | — |
| BR-D6 | No se valida dígito de control; `CHECKSUM_INVALID` no se emite | Ningún resultado lo contiene | Q2-B |
| BR-D7 | `ruleRefs` siempre lista los identificadores de las reglas de documento aplicadas: `CO-DOC-001` a `CO-DOC-003` o `PE-DOC-001` a `PE-DOC-003` | — | Trazabilidad |
| BR-D8 | El resultado es el mismo para la misma entrada (sin dependencia de fecha ni de estado) | — | RF-05 del kernel |

Ejemplos con entradas ficticias (solo para ilustrar el orden, no son números reales):

| Plugin | Entrada (tipo, número) | Resultado |
|---|---|---|
| `pe` | `dni`, `12345678` | `valid: true` |
| `pe` | `DNI`, `1234567` | `valid: false`, `[LENGTH_INVALID]` |
| `pe` | `DNI`, `1234567A` | `valid: false`, `[FORMAT_INVALID]` (8 caracteres, uno no es dígito) |
| `pe` | `DNI`, `12.345.67` | `valid: false`, `[FORMAT_INVALID, LENGTH_INVALID]` (9 caracteres) |
| `pe` | `CC`, `12345678` | `valid: false`, `[UNSUPPORTED_TYPE]` |
| `co` | `CC`, `1234567` | `valid: true` |

## Reglas de consentimiento

| Id | Regla | Origen |
|---|---|---|
| BR-CS1 | `requirements()` devuelve siempre la misma estructura: `requiredFields`, `purposeRequired` y `ruleRefs` (`CO-CON-001` o `PE-CON-001`) | Q5-A de U-core |
| BR-CS2 | `purposeRequired` es `true` en ambos plugins | Q5-A |
| BR-CS3 | La lista de campos es la de `domain-entities.md`, sin campos añadidos por práctica habitual | Q5-A |
| BR-CS4 | El plugin no evalúa registros de consentimiento; solo describe | Q5-A de U-core |

## Reglas de retención

| Id | Regla | Origen |
|---|---|---|
| BR-R1 | `retention(category)` devuelve `years` = 5 para las tres categorías en ambos plugins | F2-A |
| BR-R2 | `ruleRefs` contiene el identificador de la regla de esa categoría (por ejemplo `PE-RET-002`) | Trazabilidad |
| BR-R3 | La respuesta no indica que el valor es provisional (Q6-B); la marca vive solo en el catálogo | Q6-B, F1-A |
| BR-R4 | Una categoría fuera de las tres no llega al plugin: el kernel la rechaza con `invalid_category` | U-core BR-A3 |

## Reglas de fuente y verificación

| Id | Regla | Origen |
|---|---|---|
| BR-S1 | Toda regla tiene `id`, `kind`, `description` y `source` no vacíos | RF-12 |
| BR-S2 | Toda regla tiene `verificar: true` o está verificada. Una regla con `verificar: true` conserva su `source` como fuente **prevista** | Q7-C, RF-12 |
| BR-S3 | Una regla con `verificar: false` (verificada) debe citar en `source` un enlace `https://` oficial y la palabra «consultado» seguida de la fecha | Q8-B (texto libre con verificación mínima de forma) |
| BR-S4 | Hoy las 14 reglas son `verificar: true` | Q7-C |
| BR-S5 | La verificación es un paso del plan de Code Generation: consultar la fuente oficial, completar `source` con enlace y fecha, y pasar `verificar` a `false` o eliminar la regla | Q7-C |
| BR-S6 | Un campo de consentimiento sin artículo que lo respalde se elimina al verificar (no se conserva como práctico) | Q5-A |
| BR-S7 | Un valor de retención arbitrario se reemplaza por el verificado, o la categoría pasa a otra decisión del usuario si la norma no fija plazo | F2-A, Q6-B |

## Interpretación de los criterios de aceptación (F1-A)

| Id | Regla |
|---|---|
| BR-T1 | Los criterios de aceptación de las historias de plugins y de la API de KYC comprueban la **mecánica**: el plugin responde, la estructura es la esperada, la suite pasa. Nunca comprueban la exactitud legal de una regla |
| BR-T2 | Las pruebas que usan valores de una regla `verificar` son pruebas de comportamiento del diseño y se nombran como tales; no son criterios de aceptación de cumplimiento legal |
| BR-T3 | El README del módulo `plugins` declara que todas las reglas son provisionales y que la verificación está pendiente. La demo repite esa declaración |
| BR-T4 | La API no muestra el estado de verificación (F1-A, Q6-B). El único registro público es el README y la demo |

Efecto sobre historias aprobadas:

| Historia | Efecto |
|---|---|
| US-2.5 | «Documento peruano válido responde 200» comprueba que un DNI con formato aceptado por la regla provisional da 200 y `valid: true`; no afirma que sea legalmente válido |
| US-2.6, US-2.7 | `mvn test` pasa; los plugins listan sus tres puntos |
| US-2.9 | Se cumple: ninguna regla carece de `source` o de `verificar`; las `verificar` quedan excluidas de aceptación legal (BR-T1) |

## Manifiesto

| Id | Regla |
|---|---|
| BR-MF1 | `plugin.yaml` en la raíz del JAR, con `id` (`co-kyc` o `pe-kyc`), `country`, `version`, los tres puntos y `topics: [document, consent, retention]` |
| BR-MF2 | `sourceFile` de cada punto apunta a la clase de implementación dentro del plugin, relativa a la raíz del repositorio |
| BR-MF3 | El manifiesto pasa la validación de U-core (BR-M1 a BR-M7) |

## Independencia y empaquetado (US-2.10)

| Id | Regla |
|---|---|
| BR-I1 | Cada plugin depende solo del JAR plano de `core` y de nada más de SynKra; `co` no depende de `pe` ni al revés |
| BR-I2 | Cada plugin se empaqueta como un JAR independiente |
| BR-I3 | Ninguna regla, constante ni mensaje de un plugin menciona al otro país |
| BR-I4 | Cada plugin tiene el directorio `changes/` para los archivos de solicitud que escribe el agente; ni el plugin ni el kernel los leen |

## Cumplimiento de reglas obligatorias

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | N/A: los plugins no tienen agente ni acciones de Git o de clúster |
| AUTONOMIA-02 | N/A: no usan modelos de lenguaje |
| AUTONOMIA-03 | Cumple: cada regla se comprueba con una prueba o un comando definidos en el plan de Code Generation |
