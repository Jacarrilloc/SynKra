# U01 · Verificador y suite: plan de Functional Design

**Unidad:** `unit-of-work.md` §U01. **Historias dueñas:** H-07, H-08, H-10; contribuye a H-02,
H-03, H-21. **Salidas:** `aidlc-docs/construction/U01-verificador/functional-design/`
(`business-logic-model.md`, `business-rules.md`, `domain-entities.md`).

## Pasos

- [ ] 1. Modelar las entidades: `Version`, `RangoVersion`, `Descriptor`, `Seleccion`, `Veredicto`, `Motivo`, `Alternativa`, `Caso` de la suite
- [ ] 2. Definir la gramática de versiones y rangos (preguntas 1 y 2)
- [ ] 3. Definir las reglas de validación del descriptor (pregunta 4)
- [ ] 4. Definir el algoritmo de verificación y los códigos de motivo (pregunta 5)
- [ ] 5. Definir cómo se eligen las alternativas (pregunta 3)
- [ ] 6. Escribir los casos de la suite (7 grupos del PRD §11.1) con su resultado esperado
- [ ] 7. Identificar las propiedades para PBT parcial (PBT-02 ida y vuelta del descriptor, PBT-03 invariantes de rangos) y los generadores (PBT-07)
- [ ] 8. Revisar cumplimiento de extensiones

---

## Preguntas

Letra después de `[Answer]:`, o "aprobado" en el chat para aceptar las recomendaciones.

## Question 1
¿Qué versiones acepta el verificador?

A) Solo `MAYOR.MENOR.PARCHE` (por ejemplo `2.1.0`); se rechazan las pre-release (`2.1.0-rc1`) y los metadatos de build

B) Semver completo, con pre-release y su orden

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El demo no publica versiones candidatas, y el orden de las pre-release es
la parte de semver que más errores produce.

[Answer]: 

## Question 2
¿Qué formas de rango acepta la etiqueta "versión del core compatible"?

A) Cuatro: `N.x` (`>=N.0.0 <N+1.0.0`), `N.M.x` (`>=N.M.0 <N.M+1.0`), exacta (`2.1.0`) e intervalo `>=a <b` (también `>=a` sin techo)

B) Solo `N.x` e intervalo `>=a <b`

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Son las formas que un desarrollador de plugins escribe de verdad, y las
cuatro se reducen al mismo intervalo `[mínimo, máximo)` por dentro, así que no complican la regla.

[Answer]: 

## Question 3
Cuando un plugin no sirve, ¿qué alternativa propone el rechazo?

A) Las dos, si existen: la versión **más alta** del mismo plugin compatible con el core elegido, y la versión **más baja** registrada del core que sí acepta el plugin elegido. Si no hay ninguna, lo dice

B) Solo la versión del plugin compatible

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** CU4 pide "qué versión del core **o** del plugin sí sirve"; la más alta del
plugin es la más actual, y la más baja del core es el cambio más pequeño.

[Answer]: 

## Question 4
¿Qué hace el lector con un campo desconocido en `synkra-plugin.yaml`?

A) Lo rechaza ("campo no reconocido"): descriptor estricto

B) Lo ignora

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El descriptor lo escribe un tercero (PRD §11.4); un error de tipeo
(`coreCompatibel`) ignorado dejaría pasar un plugin sin regla. Campos: `nombre` (minúsculas,
dígitos y guiones), `version`, `core`, `coreCompatible`, `etiquetas` (opcional, clave=valor) y
`descripcion` (opcional).

[Answer]: 

## Question 5
¿Qué pasa si una selección trae dos versiones del mismo plugin (`kyc-pe 1.3.0` y `kyc-pe 2.0.0`)?

A) Se rechaza la selección completa con el motivo `PLUGIN_REPETIDO`

B) Se queda la versión más alta

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El motor nunca elige por el usuario: lo que se despliega es exactamente lo
confirmado (adherencia, PRD §11.2).

[Answer]: 
