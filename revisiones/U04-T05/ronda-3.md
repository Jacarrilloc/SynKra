# Ronda 3 — U04-T05

VEREDICTO: VERDE

F-05 y F-06 quedaron corregidos. El oráculo no se volvió permisivo, la propiedad es estable y los cuatro criterios pasan con mis propias corridas. No queda ningún ROJO ni NARANJA. Quedan dos AMARILLO, que no bloquean.

Datos de partida: el hash de la tarea es `a0794820b735ab61d95827942e9e98c4b8adc8d1` y coincide. HEAD es 7279f81. El código está en d05e8d0: `git diff --stat d05e8d0 HEAD` solo toca la bitácora y la respuesta. La base es origin/main (dc3f852). Al terminar, el worktree estaba limpio. Los JAR que mis mutantes dejaron en `/tmp` los borré.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | La propiedad pasa sin caché | `./gradlew :backend:test --rerun --tests '*RutasSegurasPropiedades'` y el grep del XML | pasa: BUILD SUCCESSFUL, `tests="4" skipped="0" failures="0" errors="0"`. Las 2 propiedades tardan 2,5 s y 2,1 s, y los 2 `@Example` 0,08 s y 0,01 s. Aceptados 32-34 %, rechazados 66-68 %. Las semillas quedan en el XML |
| 2 | Detecta una comprobación rota | mis r3-P1, r3-P2 y r3-P3 en copias `git archive HEAD` | pasa: se reducen a `'../'`, `'..\../'` y un enlace |
| 3 | Todo el backend pasa | `./gradlew :backend:test --rerun :backend:check` y el bucle de XML | pasa: BUILD SUCCESSFUL en 18 s, 17 XML, todos con `skipped="0" failures="0" errors="0"` |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 9 archivos, todos permitidos. Nada en `catalogo/**` de main y `build.gradle.kts` solo trae jqwik |

## El oráculo no se volvió permisivo (punto 1)
- **Barrido exhaustivo:** en una copia, una sonda `@Example` recorrió los 97 656 nombres de hasta 7 caracteres del alfabeto `{. / \ a :}`, más el vacío. Para cada uno comparó tres cosas:
  - `dentroDelDestino`;
  - un cálculo aparte de «fuera»: alguna lectura no queda bajo el destino, o el nombre es vacío, o lleva letra de unidad;
  - `motivoDeRechazo` del código, por reflexión.

  Resultado: **`permisivo=0`** (el oráculo no da por dentro nada que esté fuera) y **`inestable=0`** (ningún nombre que el código acepta es rechazado por el oráculo).
- **Casos sueltos:** `./\..`, `.\..\`, `./\../`, `.\..`, `./\/..`, `./\.\..`, `//` y `\.` dan todos `oraculo=false` y el código los rechaza. `./\\`, `.//` y `...` dan dentro y el código los acepta, que es lo que decidió el orquestador. El vacío sigue fuera: r3-P7 muere con `'' (0x7075 '')`.
- **Mutantes de la comprobación de rutas:** los 11 mueren (P1-P8, P12, P13 y P14).
- **Los demás mutantes del ciclo:** también mueren r3-U1, reducido a `'a/' (0x7075 '..')`, y r3-E1, reducido a `ENLACE 120755 'a'`.

## Disco vacío y la caja
- Mueren los siete mutantes de esta tanda:
  - D1: deja un directorio residual.
  - D2: mueve el temporal al padre de `datos`.
  - D3: mueve el temporal al abuelo de `datos`, en `/tmp`, que en la ronda 2 sobrevivía.
  - D5: deja el temporal vacío.
  - D6: deja un enlace.
  - D7: crea el temporal en `/tmp`, fuera de `.tmp`.
- Sobrevive **r3-D4**: el temporal movido a `/tmp` con otro nombre, `otro-recepcion-*`. Ver F-07.

## Estabilidad (punto 2)
- **Semilla de F-05:** con `seed = "-4345582717168822446"` fijada sobre HEAD, da `tests="4" failures="0"`. En b7dcce7 fallaba.
- **25 corridas limpias de CA-1:** en una copia de HEAD, borrando `.jqwik-database` entre una y otra, las 25 dieron exit 0 con `tests="4" failures="0"`.
- **Corrida larga:** con `tries = 40000` en las dos propiedades, hubo 0 fallos, con 73,7 s y 67,8 s. En la ronda 2, el fallo aparecía a los 25 793 intentos.

## Hallazgos
### F-07 · AMARILLO · RutasSegurasPropiedades.java:230-246 · La comprobación de `java.io.tmpdir` es heurística y depende del estado de la máquina
Tiene dos consecuencias:
- **Hay mutantes que la esquivan:** solo mira el prefijo `recepcion-` y solo el primer nivel de `/tmp`, así que r3-D4 sobrevive.
- **Puede fallar por otro proceso:** si otro proceso deja `recepcion-*` en `/tmp` mientras corre la prueba, la propiedad falla. Lo reproduje creando `/tmp/recepcion-ajeno-*` en segundo plano: `failures="2"`, con `recepcion-* nuevos en java.io.tmpdir, fuera de la caja ... [recepcion-ajeno-3433-43.jar]`.

Ninguna prueba del backend escribe ahí. El disparador realista es un mutante de otra sesión, como mis propios D3. El riesgo está declarado y la solución de fondo cae fuera de CA-4. Además, la aserción va en el `finally`: si las dos fallan, tapa el fallo original de `recibirEnCaja`. No bloquea.

### F-02 · AMARILLO · RutasSegurasPropiedades.java:46 y 151-155 · El oráculo copia la regex de unidad y el reemplazo de `\`
Viene de la ronda 1. Está anotado en el javadoc y lo acepto.

F-01, F-03, F-04, F-05 y F-06 quedan cerrados, según las mutaciones y las corridas de arriba.

## Tareas candidatas (defectos reales fuera de alcance)
- Redirigir el `java.io.tmpdir` de la JVM de pruebas a un directorio propio del build. Sellaría la caja, cerraría r3-D4 y quitaría la dependencia de otros procesos (F-07). Es configuración de `build.gradle.kts` y queda fuera de CA-4.
- Que el código acepte `.`, `./` y `./\` (autorreferencias), ya decidido por el orquestador.
- Ya listadas en rondas anteriores: los puntos y barras unicode con «best-fit» de Windows, y C11 (`isInterrupted`).

## Rutas de transcripciones largas
En el scratchpad de la sesión: `r3/ca1.log`, `r3/ca3.log`, `mut/r3-*.log` y `mut/r3-*/`, `mut/r3-sonda.log` y el XML de `mut/r3-sonda/`, `mut/r3-bucle.txt`, `mut/r3-largo.log`, `mut/r3-ajeno.log` y `bin/lote5.sh`.

```
VEREDICTO: VERDE
AMARILLO|RutasSegurasPropiedades.java:230-246|La comprobación de tmpdir es heurística: sobrevive r3-D4 y falla si otro proceso deja recepcion-* en /tmp
AMARILLO|RutasSegurasPropiedades.java:46,151-155|El oráculo copia la regex de unidad y el reemplazo de \ (anotado, aceptado)
INFORME: revisiones/U04-T05/ronda-3.md
```
