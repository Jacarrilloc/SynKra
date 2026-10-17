# Plan de Functional Design — U-plugins

**Insumos:** `unit-of-work.md`, `unit-of-work-story-map.md`, `U-core/functional-design/` (aprobado),
`requirements.md` (RF-10 a RF-13, decisión Q6-B de requisitos: reglas basadas en normativa real
con fuente primaria verificada).
**Alcance de la unidad:** plugins `co` y `pe`. Cada uno implementa `DocumentValidator`,
`ConsentPolicy` y `RetentionPolicy`, trae su manifiesto `plugin.yaml` y un catálogo de reglas con
fuente. **Historias:** US-2.6, US-2.7, US-2.8, US-2.9, US-2.10.

**Contexto que condiciona el diseño (ya decidido):**
- Los tipos de documento son cadenas libres y los códigos de motivo son cadenas libres por plugin (U-core Q3-A, Q4-B).
- `ConsentPolicy` solo describe (Q5-A). `RetentionPolicy` responde por `IDENTITY_DATA`, `CONSENT_RECORD`, `KYC_FILE`, en años enteros (Q6-A, Q7-B).
- La API no expone el estado de verificación de las reglas (Q11-B); solo `ruleRefs`.
- Una regla sin fuente verificada lleva la marca `verificar` y no entra en criterios de aceptación (RF-12).

**Límite de honestidad de esta etapa.** Este diseño no afirma contenido de ninguna norma sin
fuente primaria consultada. Las preguntas 7 a 9 deciden cómo se obtiene y se registra esa fuente.

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
¿Qué tipos de documento valida cada plugin?

A) Dos por país: Colombia con cédula de ciudadanía (CC) y NIT; Perú con DNI y RUC

B) Tres por país: los de A más el documento de extranjero (cédula de extranjería en Colombia, carné de extranjería en Perú), este último solo con regla de formato

C) Uno por país: cédula de ciudadanía en Colombia y DNI en Perú

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 2
¿Qué profundidad tiene la validación de cada documento?

A) Formato (longitud y solo dígitos) y, donde una fuente primaria publique el algoritmo oficial del dígito de control, también ese dígito. Donde no haya fuente primaria del algoritmo, solo formato, y la regla lo declara

B) Solo formato (longitud y caracteres permitidos) para todos los documentos

C) Formato y dígito de control para todos los documentos que lo tengan en la práctica, aunque no se logre citar la fuente del algoritmo; esas reglas quedan con la marca `verificar`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3
Los números de documento suelen escribirse con separadores (por ejemplo puntos, guiones o espacios). ¿Cómo los trata el validador?

A) Normaliza antes de validar: quita espacios, puntos y guiones, y valida lo que queda

B) Estricto: solo acepta dígitos (y el guion del dígito de control cuando el documento lo usa); cualquier otro carácter da `FORMAT_INVALID`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4
Los códigos de motivo son cadenas libres por plugin (U-core Q4-B). ¿Se unifican entre los dos plugins?

A) Convención común, con las mismas cadenas en ambos: `FORMAT_INVALID`, `LENGTH_INVALID`, `CHECKSUM_INVALID`, `UNSUPPORTED_TYPE`. Un cliente puede tratarlos igual en cualquier país

B) Cada plugin define los suyos, incluso con nombres distintos

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
`ConsentPolicy` devuelve `requiredFields`, `purposeRequired` y `ruleRefs`. ¿De dónde sale la lista de campos?

A) Solo de lo que la norma exige informar o registrar (Ley 1581 y su reglamentación en Colombia; Ley 29733 y su reglamento en Perú), cada campo con su artículo. La lista puede ser corta

B) De la norma más campos prácticos habituales (por ejemplo fecha, canal, identidad del responsable), con la marca `verificar` los que no tengan artículo

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
`RetentionPolicy` debe devolver años enteros para tres categorías, pero la norma puede no fijar un plazo para alguna de ellas. ¿Cómo se maneja una categoría sin plazo verificable?

A) Se devuelve un valor provisional con la regla marcada `verificar`, y la respuesta lo hace visible poniendo el prefijo `VERIFICAR:` en el identificador dentro de `ruleRefs`

B) Se devuelve un valor provisional con la regla marcada `verificar`, sin que la respuesta lo indique (la marca solo existe en el catálogo del plugin)

C) El plugin no define esa categoría y la respuesta es el error `plugin_error` con un mensaje que dice que no hay regla verificada

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7
¿Quién obtiene y verifica las fuentes primarias de las reglas?

A) Yo (Claude) investigo las fuentes oficiales durante la generación del diseño funcional, consulto los textos en sitios oficiales (por ejemplo del Congreso, del Senado, de la DIAN o de la SUNAT), registro cada cita con fecha de consulta y marco `verificar` lo que no logre confirmar; tú revisas el resultado antes de aprobar

B) Tú entregas los enlaces o textos oficiales y yo solo los transcribo y estructuro

C) Hoy no se verifica nada: todas las reglas quedan con la marca `verificar` y la verificación es una tarea de la Parte 2

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 8
¿Con qué formato se registra la fuente de cada regla?

A) Estructurado: nombre de la norma, artículo o numeral, enlace oficial y fecha de consulta

B) Texto libre con la cita

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 9
¿Dónde viven las reglas dentro de cada plugin?

A) En un archivo de datos del plugin (`rules.yaml`) que leen las tres clases; el catálogo y sus fuentes se revisan sin tocar código. El agente puede citar ese archivo como evidencia de un cambio

B) En el código de cada clase, con constantes y comentarios; el catálogo se arma desde esas constantes

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Parte 2: Pasos de generación (tras responder)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. (Omitido por Q7-C: no se reúnen fuentes en esta etapa; todas las reglas quedan `verificar` y la verificación pasa al plan de Code Generation)
- [x] 3. Crear `aidlc-docs/construction/U-plugins/functional-design/domain-entities.md`: regla, catálogo, cita de fuente, tipos de documento por país, reglas de validación y estructura del manifiesto de cada plugin
- [x] 4. Crear `business-rules.md`: reglas de documento, consentimiento y retención de cada país con su fuente, reglas de normalización, códigos de motivo, tratamiento de lo provisional
- [x] 5. Crear `business-logic-model.md`: flujo de validación de un documento, de consulta de consentimiento y de retención en cada plugin; diagramas Mermaid validados con alternativa en texto
- [x] 6. Verificar que las cinco historias (US-2.6 a US-2.10) se pueden satisfacer, que `co` y `pe` no dependen entre sí y que ninguna regla sin fuente entra en criterios de aceptación
- [x] 7. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`

---

## Preguntas de seguimiento (aclaraciones de las respuestas)

Revisé tus respuestas: Q1 C, Q2 B, Q3 B, Q4 A, Q5 A, Q6 B, Q7 C, Q8 B, Q9 B.
Q7-C deja **todas** las reglas de ambos plugins con la marca `verificar`, y eso choca con tres
cosas ya decididas. Necesito que las resuelvas antes de generar el diseño.

### Seguimiento 1 — Q7-C contra RF-12, US-2.5 y Q6-B
- **RF-12 y US-2.9** dicen que una regla con `verificar` no entra en criterios de aceptación. Pero US-2.5 (aprobada) acepta «un documento peruano válido responde 200», y US-2.6 y US-2.7 aceptan que la suite de cada plugin pase. Todas esas pruebas ejercen reglas que ahora serían `verificar`.
- **Q6-B** (la respuesta no indica que un valor es provisional) más Q7-C (todo es provisional) significa que la API responde con aspecto de autoridad usando reglas que nadie ha verificado. Es justo el riesgo R8 del PRD (reglas confundidas con reales).

## Question F1
¿Cómo se concilia?

A) Se acepta tal cual. Los criterios de aceptación comprueban la **mecánica** (el plugin responde 200 con la estructura correcta; la suite pasa), nunca la exactitud legal de una regla. La API no muestra el estado. El README y la demo declaran que todas las reglas son provisionales hasta verificarlas en la Parte 2

B) Igual que A, y además, mientras una regla no esté verificada, su identificador en `ruleRefs` lleva el prefijo `VERIFICAR:`. Es una excepción temporal a Q6-B (visible en la respuesta); el prefijo se quita al verificar la regla

C) Se revisa Q7: se verifican ahora las reglas de documento (formato de cédula y de DNI) con fuente oficial, y solo el consentimiento y la retención quedan provisionales

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Seguimiento 2 — Q6-B: valores provisionales de retención
Sin ninguna fuente verificada, cualquier número de años que ponga en `RetentionPolicy` es un valor
inventado. Hay que decidir quién lo elige.

## Question F2
¿Qué valores provisionales de retención (en años) se usan para `IDENTITY_DATA`, `CONSENT_RECORD` y `KYC_FILE`?

A) El mismo valor en ambos países y categorías (5 años), declarado arbitrario. Los países no se distinguen por retención hasta que se verifique la norma

B) Tú me das ahora los valores por país y categoría (seis números)

C) Valores distintos por país que yo propongo para que la demo muestre variación, declarados arbitrarios (por ejemplo, 5 años en un país y 10 en el otro)

X) Other (please describe after [Answer]: tag below)

[Answer]: A
