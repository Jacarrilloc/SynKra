# Decisiones del loop (módulo 6)

> Lo redactó el agente orquestador porque el equipo (dos integrantes) se lo pidió de forma
> explícita. Usa los datos reales del loop, y el equipo lo revisa antes de fusionarlo.

## 1. Un empujón de vuelta del codificador que arbitramos

En U03-T01 (proyecto backend), ronda 1, el revisor marcó F-02 (AMARILLO): `version = "0.1.0"`
estaba fijada en `backend/build.gradle.kts` y pidió centralizarla en la raíz de Gradle, para que
`verificador` y `backend` no terminaran con versiones distintas. El codificador empujó de vuelta con
dos argumentos. La versión hacía falta para que el JAR `backend-*.jar` cumpliera el criterio CA-2, y
la raíz de Gradle es de U01-T01, que estaba fuera del alcance de su tarea. Le dimos la razón. El
hallazgo no rompía ningún criterio, era AMARILLO y tocar la raíz metía a la tarea en terreno de otra.
Cerramos el hallazgo y anotamos el trabajo como candidata C-03, para que no se pierda. La regla que
nos llevamos es esta: si algo está fuera de alcance y no rompe un criterio, se cierra y se anota como
candidata, pero no se deja sin decidir. Con el mismo criterio aceptamos otros dos empujones: U01-T04
F-03 (C-06) y U03-T03 ronda 2 F-01 (C-08).

## 2. La tarea que más rondas costó, y qué le faltaba a su especificación

U03-T02 (SQLite con Flyway) costó 4 rondas. La tarea no fijaba el dialecto SQL de Spring Data JDBC,
y el codificador eligió ANSI por su cuenta. La ronda 1 mostró que la paginación fallaba en SQLite y
la ronda 2, que `saveAll` tampoco funcionaba. Al revisar todas las capacidades del dialecto, el
alcance creció hasta los tipos de fecha, y la ronda 3 encontró fechas corruptas según la zona
horaria. Al llegar a la ronda 4 paramos el loop, sin bajar el estándar del revisor, y recortamos el
alcance: los convertidores de fecha salieron de la tarea y pasaron a C-05. El problema estaba en la
especificación y no en el codificador. Le faltaba un criterio de aceptación sobre el dialecto y una
lista de lo que quedaba «fuera» que dejara las fechas por fuera. De ahora en adelante, las tareas de
persistencia fijan el dialecto en un criterio con su comando.

## 3. Un hallazgo del revisor que no habríamos visto

En U03-T02, ronda 3, el revisor encontró F-07: con los convertidores de fecha, un `LocalDate` que se
escribía con la zona `Asia/Tokyo` se leía un día antes con `UTC`. Todas las pruebas pasaban, porque
escribían y leían en la misma zona. El revisor lo demostró con un programa propio y leyendo los
valores guardados directamente con el módulo `sqlite3` de Python. Nosotros no lo habríamos visto:
habríamos confiado en las pruebas en verde.

## 4. Qué costó en tokens la ola en paralelo frente a correrla en serie

Tokens de los subagentes, según el aviso de cierre de cada uno. En el retrabajo el codificador se
reanudó, así que su cifra acumula todas sus rondas.

| Ola | Tarea | Codificador | Revisores (suma de rondas) |
|---|---|---|---|
| 1 | U01-T02 | 21 585 | 23 778 |
| 1 | U03-T01 | 26 968 | 29 099 + 24 464 |
| 2 | U01-T03 | 31 589 | 29 771 + 25 653 |
| 2 | U03-T02 | 70 862 | 35 825 + 44 924 + 51 594 + 46 739 |

En paralelo se gastan los mismos tokens que en serie, porque son los mismos despachos con las mismas
rutas. Lo que se gana es tiempo: en la ola 1 los dos codificadores trabajaron a la vez (57 s y 213 s)
en lugar de uno detrás del otro. El costo extra lo paga el orquestador, que tiene que arbitrar dos
diffs a la vez. Volveríamos a usar el paralelo con tareas independientes, que no tocan los mismos
archivos.

## Nota sobre las fusiones

La guía pide que los pull requests los fusione una persona. Los PRs #8 a #16 los fusionó el agente
porque el equipo se lo pidió de forma explícita, y así quedó registrado en `aidlc-docs/audit.md`. Antes
de cada fusión se comprobó que no hubiera conflictos y, después, que
`./gradlew :verificador:check :backend:check --rerun` pasara sobre la rama base.
