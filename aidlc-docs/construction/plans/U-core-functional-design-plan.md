# Plan de Functional Design — U-core

**Insumos:** `unit-of-work.md`, `unit-of-work-story-map.md`, `application-design/` (aprobados).
**Alcance de la unidad:** contratos de extensión, modelo y lector del manifiesto, cargador y
registro de plugins, guardia de país, API de KYC sin estado, `GET /plugins` y health.
**Historias:** US-2.2, US-2.3, US-2.4, US-2.5. **Requisitos:** RF-01 a RF-07, NFR-02.
**Esta etapa es independiente de la tecnología:** define reglas y modelos, no infraestructura ni código.

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
El agente decide por reglas deterministas cruzando el tema del cambio con los temas de cada manifiesto. Por eso el vocabulario de temas debe ser común. ¿Cómo se define?

A) Conjunto cerrado definido en `core`, uno por punto de extensión: `document`, `consent`, `retention`. Cada manifiesto liga esos temas a su punto de extensión; un tema fuera del conjunto no es válido

B) Cadenas libres declaradas por cada plugin (por ejemplo `consent-purpose`, `document-checksum`), cada una ligada a un punto de extensión; el cambio YAML usa esas mismas cadenas

C) Conjunto base cerrado (`document`, `consent`, `retention`) más subtemas libres opcionales por plugin (por ejemplo `consent/purpose`)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Un plugin puede estar mal armado: el manifiesto nombra una clase que `ServiceLoader` no encuentra, un JAR no trae manifiesto, o dos JAR declaran el mismo país. ¿Qué hace el kernel?

A) Fail-fast: cualquier inconsistencia detiene el arranque con un mensaje que nombra el JAR y el motivo

B) Tolerante: omite el plugin inconsistente, registra el error y sigue. La sonda de readiness queda en `DOWN` si el plugin omitido era el del país activo

C) Fail-fast solo para el plugin del país activo; los demás plugins inconsistentes se omiten con un aviso

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
El tipo de documento (cédula, NIT, DNI, RUC, etc.) es propio de cada país. ¿Cómo lo modela el kernel?

A) Cadena libre en `DocumentRequest(type, number)`; el plugin la interpreta y devuelve el código `UNSUPPORTED_TYPE` si no la reconoce

B) Enumeración genérica en el kernel (`NATIONAL_ID`, `TAX_ID`, `FOREIGN_ID`) que cada plugin traduce a sus documentos

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Los resultados de validación llevan códigos de motivo. ¿Quién los define?

A) Un conjunto cerrado en el kernel (`FORMAT_INVALID`, `CHECKSUM_INVALID`, `UNSUPPORTED_TYPE`, `LENGTH_INVALID`), común a todos los países; el plugin añade la referencia a su regla (`ruleRefs`)

B) Cadenas libres definidas por cada plugin

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 5
`ConsentPolicy` ¿solo describe lo que se exige o también evalúa un consentimiento concreto?

A) Solo describe (`GET`): campos obligatorios, si la finalidad es obligatoria y referencias de regla

B) Describe y evalúa: además de la descripción, un `POST` recibe un registro de consentimiento y devuelve `compliant` o la lista de campos faltantes

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
`RetentionPolicy` responde por categoría de dato. ¿Cuáles son las categorías?

A) Conjunto cerrado en el kernel: `IDENTITY_DATA`, `CONSENT_RECORD`, `KYC_FILE`

B) Cadenas libres definidas por cada plugin, descubribles en la respuesta

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
¿Cómo se representa el periodo de retención?

A) Duración ISO-8601 (por ejemplo `P5Y`)

B) Número entero de años

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 8
El kernel arranca con un país activo (`synkra.country`). ¿Qué pasa si no está configurado?

A) Fail-fast: el kernel no arranca y el mensaje dice que falta la configuración

B) Si hay exactamente un plugin cargado, ese país es el activo; si hay cero o más de uno, fail-fast

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
¿Qué formato tienen las respuestas de error (404 de país, validación fallida de la petición, etc.)?

A) RFC 7807 (`application/problem+json`): `type`, `title`, `status`, `detail`

B) JSON propio sencillo: `{ "error": "...", "message": "..." }`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 10
¿Qué significa readiness en el kernel?

A) `UP` solo si el plugin del país activo está cargado y trae los tres puntos de extensión; liveness es `UP` mientras el proceso responda

B) `UP` si el proceso arrancó; el estado de plugins se consulta solo en `/plugins`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 11
RF-12 pide que cada regla de país cite su fuente o lleve la marca `verificar`. ¿Esa información se expone por la API?

A) `/plugins` incluye por plugin un resumen de reglas: total, con fuente y con marca `verificar`. El contrato de extensión incluye un `RuleCatalog` que cada plugin implementa

B) No se expone por la API; solo existe en el plugin y la comprueba su prueba unitaria

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Parte 2: Pasos de generación (tras responder)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. Crear `aidlc-docs/construction/U-core/functional-design/domain-entities.md`: contratos de extensión, tipos de entrada y salida, manifiesto, plugin cargado, país activo, vocabulario de temas, y sus relaciones
- [x] 3. Crear `business-rules.md`: reglas de carga y validación del manifiesto, reglas de la guardia de país, reglas de readiness, formato de errores, reglas de independencia de país del kernel (NFR-02)
- [x] 4. Crear `business-logic-model.md`: flujos de arranque y carga de plugins, de una petición KYC, de `/plugins` y de health, con diagramas Mermaid validados y alternativa en texto
- [x] 5. Verificar que cada criterio de US-2.2 a US-2.5 se puede satisfacer con el diseño, y que ningún elemento nombra un país (NFR-02)
- [x] 6. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`

---

## Preguntas de seguimiento (aclaración de las respuestas)

Revisé tus respuestas: Q1 A, Q2 A, Q3 A, Q4 B, Q5 A, Q6 A, Q7 B, Q8 A, Q9 B, Q10 B, Q11 B.
Encontré un caso que ninguna respuesta cubre y que choca con una historia ya aprobada.

### Seguimiento 1 — Plugin del país activo ausente (Q2-A y Q8-A contra Q10-B y US-2.3)
Q2-A cubre JAR inconsistentes (clase que falta, JAR sin manifiesto, país duplicado). Q8-A cubre
la falta de `synkra.country`. Pero no dicen qué pasa cuando el país activo está configurado y
**no hay ningún plugin cargado para él** (directorio vacío, o el init container falló al copiar
el JAR). La historia US-2.3 (aprobada) dice que, con el directorio vacío, `/plugins` devuelve una
lista vacía y "el kernel sigue arriba". Con Q10-B (readiness `UP` si el proceso arrancó), ese pod
quedaría `Ready` y Kubernetes le enviaría tráfico que siempre recibiría 404.

## Question F1
¿Qué hace el kernel cuando el país activo no tiene plugin cargado?

A) Arranca y queda `Ready` (Q10-B tal cual). `/plugins` devuelve lista vacía y cada petición KYC recibe 404. US-2.3 queda como está. El riesgo (tráfico a un pod inútil) se asume y se detecta por el 404 y por `/plugins`

B) Arranca, pero con una excepción a Q10-B: readiness `DOWN` mientras falte el plugin del país activo (liveness `UP`). US-2.3 queda como está, y se añade un criterio: `/actuator/health/readiness` devuelve `DOWN` con el directorio vacío

C) Fail-fast: el kernel no arranca sin el plugin del país activo (como en Q2-A). Se enmienda el segundo criterio de US-2.3: con directorio vacío el proceso termina con código distinto de cero y un mensaje que lo explica

X) Other (please describe after [Answer]: tag below)

[Answer]: A
