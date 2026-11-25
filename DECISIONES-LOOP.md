# Decisiones del loop (módulo 6)

> **Borrador del orquestador con los datos reales del loop.** La entrega pide que esta media página
> la escriba el responsable del proyecto: úsala como apuntes y reescríbela con tu voz.

## 1. Una vez que arbitré un empujón de vuelta del codificador

U03-T01, ronda 1, F-02 (AMARILLO): el revisor pidió centralizar `version` y los repositorios en la
raíz de Gradle. El codificador lo empujó fuera de alcance porque la raíz no era de su tarea. Se le dio
la razón: el hallazgo se cerró y quedó como candidata C-03. Otros empujones aceptados con el mismo
criterio: U01-T04 F-03 (C-06) y U03-T03 ronda 2 F-01 (C-08).

## 2. La tarea que más rondas costó, y qué estaba mal en su especificación

U03-T02 (SQLite con Flyway), 4 rondas. La tarea no decía nada del dialecto SQL de Spring Data JDBC y
el codificador lo eligió por su cuenta (ANSI). La ronda 1 mostró que la paginación no funcionaba en
SQLite y la ronda 2, que `saveAll` tampoco. En la ronda 2 el orquestador pidió revisar todas las
capacidades del dialecto, y con eso el alcance llegó a los tipos de fecha. La ronda 3 encontró fechas
corruptas según la zona horaria. Al llegar a la ronda 4 se paró el loop y el responsable aprobó
recortar: los convertidores de fecha salieron y pasaron a C-05. Lo que faltaba en la tarea era un
criterio sobre el dialecto y una lista «fuera» que dejara las fechas fuera.

## 3. Un hallazgo del revisor que yo no habría visto

U03-T02, ronda 3, F-07: con los convertidores de fecha, un `LocalDate` escrito con la zona
`Asia/Tokyo` se leía un día antes con `UTC`, y nada lo detectaba. El revisor lo demostró con un
programa propio y leyendo los valores guardados con el `sqlite3` de Python. Las pruebas pasaban
porque escribían y leían en la misma zona.

## 4. Qué costó en tokens la ola en paralelo frente a correrla en serie

Tokens de los subagentes (según el aviso de cierre de cada uno; en las rondas de retrabajo el
codificador se reanudó, así que su cifra acumula todas sus rondas):

| Ola | Tarea | Codificador | Revisores (suma de rondas) |
|---|---|---|---|
| 1 | U01-T02 | 21 585 | 23 778 |
| 1 | U03-T01 | 26 968 | 29 099 + 24 464 |
| 2 | U01-T03 | 31 589 | 29 771 + 25 653 |
| 2 | U03-T02 | 70 862 | 35 825 + 44 924 + 51 594 + 46 739 |

Correr en paralelo no cambia los tokens: son los mismos despachos, con las mismas rutas. Lo que
cambia es el tiempo: en la ola 1 los dos codificadores trabajaron a la vez (57 s y 213 s) en lugar de
uno detrás del otro. El costo extra del paralelo está en el orquestador, que tiene que arbitrar dos
diffs a la vez.
