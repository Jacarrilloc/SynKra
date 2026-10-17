# Reglas de negocio — U-core

Cada regla indica la respuesta del plan que la origina y la historia o requisito que la prueba.

## Configuración

| Id | Regla | Origen |
|---|---|---|
| BR-C1 | `synkra.country` es obligatoria. Si falta, el kernel no arranca y el mensaje dice que falta la configuración | Q8-A |
| BR-C2 | `synkra.country` debe ser un `CountryCode` válido (dos letras). Si no, el kernel no arranca | Q8-A |
| BR-C3 | `synkra.plugins.dir` es el directorio de JAR. Si no existe, se trata como directorio vacío y se registra un aviso | Suposición (coherente con F1-A) |

## Manifiesto

| Id | Regla | Origen |
|---|---|---|
| BR-M1 | Cada JAR de plugin trae `plugin.yaml` en su raíz. Sin él, el JAR es inconsistente | Q2-A |
| BR-M2 | `id`, `country`, `version`, `extensionPoints` y `topics` son obligatorios y no vacíos | RF-11 |
| BR-M3 | `id` usa solo minúsculas, dígitos y guion | Suposición de formato |
| BR-M4 | `country` es un `CountryCode` válido | Q1-A y BR-C2 |
| BR-M5 | `extensionPoints` contiene exactamente los tres puntos, una vez cada uno, cada uno con `implementationClass` y `sourceFile` no vacíos | RF-10 |
| BR-M6 | Cada elemento de `topics` pertenece al conjunto cerrado `document`, `consent`, `retention`, sin repetidos y al menos uno | Q1-A |
| BR-M7 | Un manifiesto con cualquier violación es inconsistente y se informa con el nombre del JAR y el campo | Q2-A |

## Carga de plugins

| Id | Regla | Origen |
|---|---|---|
| BR-L1 | Se examinan los archivos `.jar` del directorio, sin recorrer subdirectorios, en orden alfabético por nombre (arranque determinista) | RF-02 |
| BR-L2 | Todos los JAR se cargan en un único cargador de clases | Q3-B de Application Design |
| BR-L3 | Para cada punto del manifiesto, la clase `implementationClass` debe ser descubierta por el mecanismo de servicios y provenir de ese mismo JAR. Si no, el JAR es inconsistente | Q2-A |
| BR-L4 | Si dos JAR declaran el mismo `country`, o el mismo `id`, el arranque se detiene y el mensaje nombra ambos JAR | Q2-A |
| BR-L5 | Si dos JAR contienen una clase con el mismo nombre completo, el arranque se detiene y el mensaje nombra la clase y ambos JAR (riesgo de cargador único) | Riesgo 4 de Application Design |
| BR-L6 | Cualquier inconsistencia (BR-M1, BR-M7, BR-L3, BR-L4, BR-L5) detiene el arranque con código de salida distinto de cero. No se omiten plugins | Q2-A |
| BR-L7 | Un directorio vacío, o sin plugin para el país activo, **no** es una inconsistencia. El kernel arranca, `GET /plugins` devuelve la lista de lo cargado (posiblemente vacía) y las peticiones KYC reciben 404 | F1-A, US-2.3 |
| BR-L8 | Los plugins de países distintos del activo se cargan y se listan en `/plugins`, pero no atienden peticiones KYC | RF-03, Q4-C |

## Guardia de país y orden de comprobaciones

Una petición KYC pasa por estas comprobaciones, en este orden. La primera que falla decide la respuesta.

| Orden | Comprobación | Respuesta si falla |
|---|---|---|
| 1 | El país de la ruta, recortado y en minúsculas, es un `CountryCode` válido | 400, `invalid_country` |
| 2 | El país es igual al país activo | 404, `country_not_served`; el mensaje nombra el país pedido |
| 3 | Hay un plugin cargado para el país | 404, `plugin_not_loaded`; el mensaje nombra el país |
| 4 | El cuerpo o los parámetros de la petición son válidos (BR-A1, BR-A3) | 400, `invalid_request` o `invalid_category` |
| 5 | El plugin responde | 200 con el resultado, o 500 `plugin_error` (BR-A5) |

## API de KYC

| Id | Regla | Origen |
|---|---|---|
| BR-A1 | Validar documento: `type` y `number` son cadenas no vacías; si no, 400 `invalid_request`. Un documento inválido **no** es un error: se responde 200 con `valid: false` y sus `reasonCodes` | Q3-A, Q4-B |
| BR-A2 | Consentimiento: solo descripción (lectura). Devuelve `requiredFields`, `purposeRequired` y `ruleRefs`. No existe evaluación de un registro de consentimiento | Q5-A |
| BR-A3 | Retención: el parámetro `category` es obligatorio y debe ser una de `IDENTITY_DATA`, `CONSENT_RECORD`, `KYC_FILE`. Si falta o no pertenece, 400 `invalid_category` con la lista válida en el mensaje. Devuelve `years` y `ruleRefs` | Q6-A, Q7-B |
| BR-A4 | Sin estado: ninguna petición lee ni escribe almacenamiento persistente; no hay sesiones. El kernel no declara dependencias de base de datos | RF-05 |
| BR-A5 | Si el plugin lanza una excepción, la respuesta es 500 `plugin_error` con un mensaje genérico, sin traza; la causa se registra en el log | Suposición de robustez |
| BR-A6 | Todas las respuestas de error usan `ErrorResponse` con `error` y `message` | Q9-B |

## Listado de plugins

| Id | Regla | Origen |
|---|---|---|
| BR-P1 | `GET /plugins` devuelve una lista de `PluginSummary` ordenada por `id`. Vacía si no hay plugins | RF-03, US-2.3 |
| BR-P2 | Cada entrada lista los tres puntos de extensión y la versión. No expone reglas ni su estado de verificación (Q11-B) | RF-03, Q11-B |

## Salud

| Id | Regla | Origen |
|---|---|---|
| BR-H1 | Liveness: `UP` mientras el proceso responde | RF-07 |
| BR-H2 | Readiness: `UP` cuando el arranque terminó. Una vez terminado el arranque, el estado de los plugins no cambia la readiness; se consulta en `/plugins`. Como el arranque nunca termina con inconsistencias (BR-L6), un pod `Ready` tiene todos sus JAR consistentes | Q10-B, F1-A |
| BR-H3 | Riesgo asumido: un pod sin plugin del país activo queda `Ready` y devuelve 404 (F1-A) | F1-A |

## Independencia de país (NFR-02, P2)

| Id | Regla |
|---|---|
| BR-I1 | Ningún código, recurso ni comentario de `core/src` contiene nombres de país ni códigos de país como palabras sueltas (la comprobación `grep -rniE "colombia\|peru\|\\bCO\\b\|\\bPE\\b" core/src` no devuelve resultados). Esto incluye pruebas y datos de prueba |
| BR-I2 | Las pruebas de `core` usan países ficticios (`xx` y `yy`) y plugins de prueba construidos para la prueba, nunca `co` ni `pe` |
| BR-I3 | Los mensajes de error nombran el país **pedido**, tomado de la petición, nunca una lista fija |

## Trazabilidad con las historias

| Historia | Reglas que la sostienen |
|---|---|
| US-2.2 | BR-I1, BR-I2 y el diseño de tres contratos |
| US-2.3 | BR-L1 a BR-L8, BR-P1, BR-P2, BR-C3 |
| US-2.4 | BR-H1, BR-H2 |
| US-2.5 | Orden de comprobaciones, BR-A1 a BR-A6, BR-I3 |

## Cumplimiento de reglas obligatorias

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | N/A en esta unidad: el kernel no tiene agente ni acciones de Git o de clúster |
| AUTONOMIA-02 | N/A: el kernel no usa modelos de lenguaje |
| AUTONOMIA-03 | Cumple. Cada regla tiene un criterio verificable en las historias o se verificará en el plan de tareas |
