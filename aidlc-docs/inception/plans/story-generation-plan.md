# Plan de generación de historias: SynKra

**Entradas:** `aidlc-docs/inception/requirements/requirements.md` (aprobado el 21-nov-2026) y
PRD v2 §3, §5 y §7. **Salidas:** `aidlc-docs/inception/user-stories/personas.md` y `stories.md`.

## Pasos de ejecución

- [x] 1. Generar `personas.md`: una persona por usuario del PRD §3, con su rol del §9.2, lo que necesita y lo que no puede hacer
- [x] 2. Organizar las historias según el enfoque elegido en la pregunta 1
- [x] 3. Escribir cada historia con el formato de la pregunta 2 y criterios de aceptación con el formato de la pregunta 3
- [x] 4. Incluir los *edge cases* del PRD §7.3 (abandono) y §7.4 (fallo y traspaso al humano) como historias propias
- [x] 5. Trazar cada historia a sus RF y a su caso de uso o journey; comprobar que todo RF Must y Should está cubierto por al menos una historia
- [x] 6. Mapear personas a historias (tabla al final de `stories.md`)
- [x] 7. Revisar INVEST: independientes, negociables, valiosas, estimables, pequeñas y comprobables
- [x] 8. Revisar cumplimiento de extensiones: AUTONOMIA-01 (ninguna historia despliega sin confirmación) y AUTONOMIA-02 (todo criterio con comando)

## Enfoques posibles para descomponer las historias

| Enfoque | A favor | En contra |
|---|---|---|
| Por journey | Sigue el recorrido de la demo | Repite pasos comunes entre journeys |
| Por capacidad (épica por módulo M1 a M8) | Mapea directo a los componentes y luego a las unidades | Pierde el hilo del usuario |
| Por persona | Claro para los permisos | Las capacidades compartidas quedan partidas |
| Híbrido: épicas por capacidad, historias escritas desde la persona y etiquetadas con su caso de uso | Facilita Units Generation sin perder al usuario | Un poco más de estructura |

---

## Preguntas

**Cómo responder.** Igual que antes: escribe la letra después de `[Answer]:`, o dime en el chat
si aceptas las recomendaciones.

## Question 1
¿Cómo se organizan las historias?

A) Por journey del PRD §7

B) Por capacidad: una épica por módulo (catálogo, verificación, despliegue, registro y reversión, core y plugins, front, instalación y límite)

C) Por persona

D) Híbrido: épicas por capacidad, cada historia escrita desde una persona y etiquetada con su caso de uso (CU1 a CU5) o journey

X) Other (please describe after [Answer]: tag below)

**Recomendación: D.** Las épicas por capacidad se recortan casi solas en unidades; la etiqueta
de caso de uso mantiene la trazabilidad con el PRD.

[Answer]: D

## Question 2
¿Qué tamaño tiene una historia?

A) Una historia por requisito funcional (unas 39)

B) Una historia por comportamiento observable desde la API, el front o el clúster, agrupando RF pequeños (estimado: 25 a 30)

C) Una historia por caso de uso (5, más los journeys)

X) Other (please describe after [Answer]: tag below)

**Recomendación: B.** A da historias que son tareas técnicas; C da historias que no caben en
una unidad ni se prueban solas.

[Answer]: B

## Question 3
¿En qué formato van los criterios de aceptación?

A) Dado / Cuando / Entonces, y cada criterio termina con el comando que lo comprueba (`curl`, `kubectl`, `sqlite3`, `gradlew`)

B) Lista de comprobaciones, una por línea, cada una con su comando

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Deja explícito el estado previo, que es lo que cambia en los escenarios de
rechazo y de fallo, y cumple AUTONOMIA-02.

[Answer]: A

## Question 4
¿Las capacidades técnicas sin un usuario directo (librería de verificación, suite que bloquea el
build, imágenes no root, init container) se escriben como historias?

A) Sí, como historias de un "usuario técnico" (el operador o el desarrollador de plugins), para que todo RF quede en alguna historia y luego en alguna unidad

B) No; quedan como requisitos técnicos que las unidades cubren directamente

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** La guía exige que toda historia tenga unidad y que toda tarea trace a una
historia; una tarea que solo traza a un RF rompe esa cadena.

[Answer]: A

## Question 5
¿Cómo se tratan los requisitos Should en las historias?

A) Historias propias marcadas `Should`, separadas de las Must, para que se puedan cortar sin tocar las demás

B) Criterios extra dentro de las historias Must

X) Other (please describe after [Answer]: tag below)

**Recomendación: A**, por el riesgo R9 (alcance que no cabe antes de la sustentación).

[Answer]: A

## Question 6
¿Cuántas personas se definen?

A) Las 4 del PRD §3 (operador, desarrollador de plugins, responsable de país, SRE o lector), con nombres propios

B) Las 4 más un "sistema" (el core y el init container) como actor técnico

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El core no es un usuario; sus historias se escriben desde el operador o el
responsable de país, que son quienes ven el efecto.

[Answer]: X. Las 4 personas del PRD §3, identificadas solo por su rol, sin nombres propios (Julian, 21-nov-2026: "saque esos nombres, solo dejalo general, no queremos nombres propios, sin nombres"). También se quitan los nombres de los journeys del PRD.
