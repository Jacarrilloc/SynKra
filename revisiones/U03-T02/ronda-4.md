# Ronda 4 — U03-T02

VEREDICTO: VERDE

Corrí yo mismo los cuatro criterios y la suite completa, desde cero, sobre `988681e`. Todo pasa. Los convertidores de fecha se retiraron por completo, como pedía el arbitraje. Con el código actual, una fecha guardada no se puede leer en silencio con otro valor: la lectura falla con una excepción explícita (lo comprobé abajo). No queda ningún hallazgo ROJO ni NARANJA.

Contexto verificado:
- **Tarea:** el hash del archivo coincide con `c34b208af15cb5d1161df49a576d5c33560924b1`.
- **Rama:** HEAD es `988681e`, el sha reportado, en `m6r/U03-T02`. Lleva 7 commits sobre `m6r/U03-T01`.
- **Puerto:** el 18080 estaba libre.
- **`/data`:** no existía ni antes ni después de las corridas.
- **Base fuera de sitio:** no apareció ningún `synkra.db` en el worktree fuera de `build/`.
- **Estado final:** el worktree quedó limpio (`git status --short` vacío).
- **Cerca de alcance:** el diff no toca `verificador/`, la raíz de Gradle, `settings.gradle.kts` ni ningún `build.gradle.kts`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La prueba de la base pasa sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*BaseDeDatosTest'` + grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="3" skipped="0" failures="0" errors="0"`, 10,2 s (arranque del contexto en frío) |
| 2 | Lectura de vuelta con un cliente SQLite independiente | `bootJar` y luego CA-2 tal cual, con `SYNKRA_DATA=$(mktemp -d)` en el scratchpad | pasa: `[('1', 1)]`. El log dice `Successfully applied 1 migration ... now at version v1` |
| 3 | Reiniciar no vuelve a migrar | CA-3 con el mismo `SYNKRA_DATA` | pasa: `1`. El log dice `Schema "main" is up to date. No migration necessary.` |
| 4 | Sin `SYNKRA_DATA`, la ruta por defecto es `/data` | `./gradlew :backend:test --rerun --tests '*RutaDatosPorDefectoTest'` + grep | pasa: `tests="1" skipped="0" failures="0" errors="0"`. No se creó `/data` |

El jar sale de las fuentes actuales:
- `bootJar` estaba al día, es decir, Gradle no vio entradas cambiadas.
- El jar es de las 15:31, la misma hora del commit `988681e`.
- `unzip -l` lista solo `DialectoSqlite`, `$1`, `$2`, `$3` y `$EnteroABooleano`. No queda ninguna clase de los convertidores de fecha.

Suite completa: `./gradlew :backend:test --rerun` da `BUILD SUCCESSFUL`.

| Clase | tests / skipped / failures / errors | Tiempo |
|---|---|---|
| BaseDeDatosTest | 3/0/0/0 | 1,14 s |
| DialectoSqliteTest | 6/0/0/0 | 0,06 s |
| RepositorioSqliteTest | 4/0/0/0 | 0,83 s |
| RutaDatosPorDefectoTest | 1/0/0/0 | 0,007 s |
| SynkraAppTest | 1/0/0/0 | 6,25 s |

Los tiempos bajos tienen la misma explicación que en la ronda 3:
- SynkraAppTest paga el arranque y las demás clases lo reutilizan.
- DialectoSqliteTest corre sobre SQLite en memoria.
- RutaDatosPorDefectoTest no abre ninguna base.

## Estado de los hallazgos de la ronda 3
**F-07 (convertidores de fecha): resuelto por retirada, según el arbitraje.** Comprobé que no quedan restos.
- **Código:** `git grep -i -E 'instant|localdate|zoneid|java\.time|fecha|epoch|milisegundo' -- backend` no devuelve nada.
- **Lo único que queda en `getConverters()`:** `EnteroABooleano`. `@ReadingConverter` ya solo lo usa ese convertidor, en `DialectoSqlite.java:81`.
- **Javadoc:** se borró el comentario falso sobre los milisegundos.
- **Pruebas:** se quitaron de `Elemento` los campos `instante`, `fecha` y `fechaHora`, las columnas del esquema de prueba y la aserción de `instante`.

**Ya no hay corrupción silenciosa.** Volví a correr el programa de la ronda 3 contra las clases del jar actual: escritura en `Asia/Tokyo` y lectura en `UTC`.
- **`Instant` y `LocalDate` en columna INTEGER:** toda lectura falla con `ConverterNotFoundException`, tanto con `Long` como con `Integer`. También fallan las consultas derivadas que devuelven esas filas. La falla es explícita: no sale una fecha corrida ni se pierde nada sin aviso.
- **`LocalDateTime`:** se guarda como TEXT ISO sin zona. Su conversión es la de Spring, no la del diff, y no depende de la TZ.
- **Nulos:** se leen bien.

**F-08 (afirmación sin evidencia en la bitácora): sin objeto.** La bitácora de la ronda 4 retira la afirmación de forma expresa, y ya no hay convertidores de escritura en el diff.

## Hallazgos
Ninguno en pie.

### F-09 · AMARILLO · bitacoras/U03-T02.md (Ronda 4) · El «Verde» de CA-2 pega una salida que su comando no produce
La bitácora pega `CA-2: [('1', 1)] 1`, pero el comando de CA-2 solo imprime `[('1', 1)]`. Parece la salida de CA-2 y CA-3 juntas. Mi corrida da los dos valores esperados por separado, así que no afecta a ningún criterio. Es un tema de precisión de la evidencia y no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- **Para C-05:** la retirada deja una sola cosa pendiente. sqlite-jdbc sigue escribiendo `LocalDate` como milisegundos de la medianoche en la zona del proceso: `2026-01-02` se guarda como `1767279600000` en Tokio. Hoy la lectura falla con una excepción explícita. Pero una consulta por rango que compare esos valores en SQL depende de la zona con la que se escribió. C-05 debería fijar una representación que no dependa de la zona, tanto al escribir como al leer.
- C-04 (las PRAGMA) sigue pendiente del orquestador, sin cambios.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/r4/`:
- `ca1.log`, `ca4.log`, `bootjar.log`, `ca2-app.log`, `ca3-app.log`, `full.log`: los criterios y la suite completa.
- `prog/io/synkra/plataforma/Verif3.java` y `Verif4.java`: los programas de prueba de las fechas.
- `v4-escribir-tokio.log` y `v4-leer-utc.log`: `Instant` falla con una excepción.
- `v4b-escribir-tokio.log` y `v4b-leer-utc.log`: `LocalDate` falla con una excepción y `LocalDateTime` se guarda como TEXT.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U03-T02.md (Ronda 4, Verde)|El «Verde» de CA-2 pega una salida que su comando no produce
INFORME: revisiones/U03-T02/ronda-4.md
```
