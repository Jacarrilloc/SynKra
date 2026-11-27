# Ronda 2 — U04-T05

VEREDICTO: NO-VERDE

Los cuatro criterios pasaron en mi corrida del worktree, y las correcciones de F-01, F-03 y F-04 funcionan: D1, D2, P7, U1 y E1 mueren. Pero el oráculo nuevo de F-03 falla de vez en cuando contra el código de producción sin tocar. El caso es la entrada `'./\/'`, una referencia al destino mismo escrita con `\`. La reproduzco con la semilla `-4345582717168822446`, y en 32 corridas limpias falló 2 veces, alrededor de un 6 %. Eso es un ROJO (F-05).

Datos de partida: el hash de la tarea es `a0794820b735ab61d95827942e9e98c4b8adc8d1` y coincide. HEAD es bc9225a. El código está en b7dcce7: `git diff --stat b7dcce7 HEAD` solo toca la bitácora y la respuesta. La base es origin/main (dc3f852). El worktree quedó limpio. No queda nada mío en `/tmp`: borré los `recepcion-*.jar` que dejó mi mutante D3.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La propiedad pasa sin caché | `./gradlew :backend:test --rerun --tests '*RutasSegurasPropiedades'` y el grep del XML | en el worktree pasó: BUILD SUCCESSFUL, `tests="2" skipped="0" failures="0" errors="0"`, 1,87 s y 1,09 s. Aceptados 33 %, rechazados 67 %, extra 0x7075 en el 27-28 % de las entradas, semillas registradas. **Pero no es estable: ver F-05** |
| 2 | Detecta una comprobación rota | mis r2-P1, r2-P2 y r2-P3 en copias `git archive HEAD` | pasa: se reducen a `'../'`, `'..\../'` y `ENLACE 120777 'a'` |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` y el bucle de XML | pasa: BUILD SUCCESSFUL en 16 s, 17 XML, todos con `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 7 archivos de la lista permitida, nada en `catalogo/**` de main y `revisiones/U04-T05/` permitido |

## Mutaciones propias de la ronda 2 (copias `git archive HEAD`, filtro `*RutasSegurasPropiedades`)
- **Mueren:**
  - r2-P7, sin `isEmpty`. Contraejemplo `'' (0x7075 '')`.
  - r2-D1, que deja el directorio `residuo`.
  - r2-D2, que mueve el temporal al padre de `datos`.
  - r2-D5, que deja el temporal vacío.
  - r2-D6, que deja un enlace en `.tmp`.
  - r2-A1, que crea un directorio también al aceptar.
  - r2-U1, sin `unicodeDistinto`. Contraejemplo `'META-INF/' (0x7075 '..')`.
  - r2-E1, que pide `getUnixMode()==0120777`. Contraejemplo `ENLACE 120755 'a'`.
- **Sobrevive:** r2-D3, que mueve el temporal al abuelo de `datos`, es decir, al `/tmp` del sistema. Dejó 1307 JAR en `/tmp` y la propiedad quedó en verde (F-06).

## Hallazgos
### F-05 · ROJO · RutasSegurasPropiedades.java:196 y 210 (el oráculo de F-03) · El oráculo marca como fuga `./\`, algo que el código acepta y que no sale del destino; CA-1 falla según la semilla
`autoReferencia` se calcula sobre el nombre crudo: solo cuenta `.` y `/`. En cambio, la comparación con el destino se hace sobre cada lectura, y en la lectura con `\` como separador, `./\` y `.\` resuelven al destino mismo. Como el nombre tiene `\`, no se les aplica la excepción y el oráculo dice «fuera». El código las acepta: `split("/")` descarta los vacíos del final y no aparece `..`.

El generador produce ese caso: un segmento `.` (de «puntos») seguido del segmento `\` (de «barra invertida»).

La sonda que agregué en una copia da esto:

| Nombre | Lo acepta el código | El oráculo dice dentro |
|---|---|---|
| `./\` | sí | no |
| `.\` | sí | no |
| `./\/` | sí | no |
| `./`, `.`, `./.` | sí | sí |

Evidencia sobre HEAD sin modificar:
- **Semilla fijada:** en una copia que solo agrega `seed = "-4345582717168822446"`, la propiedad falla con `aceptada y fuera del destino: './\/' (0x7075 './\/')`.
- **Frecuencia:** 30 corridas de CA-1 con `.jqwik-database` borrado entre una y otra dan 1 fallo, y en una primera tanda falló la segunda corrida. Son 2 fallos de 32, alrededor de un 6 %, siempre con el mismo caso reducido.
- **Efecto de la base de datos de jqwik:** con `after-failure = SAMPLE_FIRST`, un fallo se repite en todas las corridas siguientes. En mi primer bucle fallaron 39 de 40 corridas por eso. Una sola corrida mala deja la máquina en rojo hasta que alguien borre la base.

Esto no cumple CA-1 («la propiedad pasa») ni la condición del orquestador: su excepción cubre `.` y `./`, y `./\` es la misma autorreferencia en la lectura con `\`.

Arreglo sugerido: calcular la autorreferencia por lectura, sobre `conBarras`, o sea, nombres hechos solo de `.`, `/` y `\`. Así se sigue sin aceptar el vacío ni nada que resuelva fuera, porque el `startsWith` se mantiene. Conviene fijar `./\` como caso de ejemplo y la semilla anterior en la bitácora.

### F-06 · AMARILLO · RutasSegurasPropiedades.java:219-225 · La caja se esquiva subiendo un nivel más
r2-D3 lo demuestra. Es un límite declarado y cualquier caja de profundidad fija se puede esquivar así, pero el caso realista, dejar el temporal o directorios en `datos`, ya está cubierto. Un refuerzo posible sin salir del alcance: comparar `Files.list(java.io.tmpdir)` filtrado a `recepcion-*` antes y después de cada intento. No bloquea.

### F-02 (ronda 1) · AMARILLO · Quedó anotado en el javadoc
Está documentado y lo acepto.

### F-01, F-03 y F-04 (ronda 1) · Cerrados
Lo verifican las mutaciones de arriba. La parte de F-03 que se corrigió es la que introdujo F-05.

## Tareas candidatas (defectos reales fuera de alcance)
- Que el código acepte `.`, `./` y `./\` (autorreferencias). El orquestador ya decidió no tocarlo; queda como candidata.
- Redirigir el `java.io.tmpdir` de la JVM de pruebas a un directorio propio, para sellar la caja (F-06). Es configuración de `build.gradle.kts` y queda fuera de CA-4.
- Ya listadas en la ronda 1: los puntos y barras unicode con «best-fit» de Windows, y C11 (`isInterrupted`).

## Rutas de transcripciones largas
En el scratchpad de la sesión: `r2/ca1.log`, `r2/ca3.log`, `mut/r2-*.log` y `mut/r2-*/`, `mut/r2-largo.log`, `mut/r2-bucle.txt`, `mut/r2-bucle2.txt` y `bin/lote4.sh`.

```
VEREDICTO: NO-VERDE
ROJO|RutasSegurasPropiedades.java:196,210 / CA-1|El oráculo da './\' como fuga aunque el código lo acepta y no sale del destino: la propiedad falla según la semilla (-4345582717168822446, 2 de 32 corridas)
AMARILLO|RutasSegurasPropiedades.java:219-225|La caja se esquiva escribiendo en el abuelo de datos (sobrevive r2-D3); límite declarado
AMARILLO|RutasSegurasPropiedades.java:46,151-155|El oráculo copia la regex de unidad y el reemplazo de \ (anotado, aceptado)
INFORME: revisiones/U04-T05/ronda-2.md
```
