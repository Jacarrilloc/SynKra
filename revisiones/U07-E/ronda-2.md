# Ronda 2 — U07-E

VEREDICTO: VERDE

Corrí los siete criterios yo mismo y todos pasan. Las siete mutaciones de la ronda 1 (A, B, C, F, G, I y J) ahora hacen caer la prueba de su regla. Probé además cinco variantes más finas: cuatro caen y una sobrevive, pero es casi equivalente (F-05). No queda ningún ROJO ni NARANJA abierto. Quedan dos AMARILLO, que no bloquean.

Comprobaciones previas:
- **Hash de la tarea:** 4252363a54786bae39c5ed75ae4ed6cd8c27c629, coincide.
- **Worktree:** en 1783515, limpio antes y después (`git status --short` da 0 líneas). El puerto 18094 estaba libre antes y después, y no queda ningún JAR vivo.
- **03ffd16 solo trae pruebas:** `git diff 2d9d21f 03ffd16 -- backend/src/main` da 0 líneas. El commit trae solo `VigilanciaCoincideTest`, `VigilanciaDobleTest` y el informe de la ronda 1.
- **Nada de código después de 03ffd16:** `git diff 03ffd16 HEAD -- backend` está vacío. 53e42e3 y 1783515 solo traen evidencia y bitácora.
- **Edad de la evidencia:** es coherente. 03ffd16 es de las 03:16:41Z; las mutaciones del codificador son de las 03:16 a 03:17Z y `ca1` de las 03:22Z. `ca7` es de las 03:24:37Z, entre 53e42e3 (03:24:35Z) y su propio commit.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Vigilancia sin escrituras | Comandos literales de CA-1 | **pasa**: `tests="8"` 0/0/0, los 8 testcase uno por país, la lista de Python exacta y `[(0, 0, 2)]`. El glob también recoge `RetomarVigilanciaTest`; eso ya está en C-122 |
| 2 | Retomar tras reinicio y C-109 | Comandos literales de CA-2 | **pasa**: `tests="2"` 0/0/0 con `time="6.276"` (Awaitility real), las tres listas exactas y `sleep=1`. Tampoco hay `Thread.sleep` en las dos clases nuevas |
| 3 | C-76, C-82, C-83 y C-116; U05 y U06 igual | Comandos literales de CA-3 | **pasa**: las ocho clases dan 0/0/0 (3, 6, 3, 1, 20, 10, 10, 8). Hay una sola línea `40: .withRequestRetryBackoffLimit(0)` y la lista de 41 a 45 es exacta |
| 4 | El JAR real sin clúster | `bootJar` y luego el guion literal, con `$JAVA_HOME/bin/java`, HOME y KUBECONFIG falsos y el puerto 18094 | **pasa**: `espera=0`, `91 fallido {… PLAZO\nNo se pudo leer …}`, `92 aplicando None`, `[(91,'confirmado'),(92,'confirmado'),(93,'vencido'),(94,'pendiente')]` y `[(91,'fallido'),(92,'aplicando')]`. El JAR no trae `config/application.properties` |
| 5 | Cada regla muerde | 12 mutaciones mías en copias (`git archive 1783515` + `git init -q`) con las 8 clases | **pasa**: A, B, C, F, G, I, J, A2, B2, G2 y J2 dan BUILD FAILED en la prueba de su regla. K sobrevive (ver F-05) |
| 6 | Todo el backend; la vigilancia solo lee | Comandos literales de CA-6 | **pasa**: BUILD SUCCESSFUL y los 59 XML con 0/0/0. Todos los greps dan lo esperado: solo `ConfirmacionServicio.java`; solo los cuatro métodos de lectura; `registro-interno=1`, `tablas-reg=1`; `TareasProgramadas.java` con `2`; `vigilancia-tx=1`, `requiresNew=1`; `Reloj.java` y `RegistroServicio.java`; `synkra.programacion.activa=false` |
| 7 | Diff acotado | Comandos literales de CA-7 con la base 39a242c | **pasa**: solo rutas permitidas. Las dos pruebas nuevas son `A` dentro de `despliegue`; la tercera orden da `0`; no hay nada de `plataforma` ni de `catalogo` |

Comprobé el estado en las bases de datos de las clases nuevas, no solo que terminaran sin error:
- **`VigilanciaDobleTest`:** 970 queda `fallido` con «cierre de la prueba» (lo cierra la propia prueba). 971 y 975 quedan `listo`. 972 da `('', '', PLAZO\nSIN_LECTURA)` y 973 da `('lc', 'li', PLAZO\nWarning X: y)`. 974 queda `listo`, porque lo cerró el gancho. El log trae «No se pudo revisar el despliegue 970 de xx», así que el aislamiento por fila se ejercitó de verdad.
- **`VigilanciaCoincideTest`:** 960, 961 y 962 quedan `fallido` con `PLAZO\nNO_APLICADO`.

## Respuestas de la ronda 1, verificadas

**F-01 (resuelto).** Tabla de mutaciones; las de A, F, G y J las escribí yo:
| Mut. | Qué cambié | Prueba que cayó |
|---|---|---|
| A | `catch (RuntimeException)` pasa a `catch (Error)` | `unaFilaQueLanzaNoDetieneLaPasada` y otras tres en cascada |
| A2 | el `catch` hace `break` | las mismas cuatro |
| B | `SIN_LECTURA` pasa a `return false` | `unFallidoConContextoIlegible…` |
| B2 | `SIN_LECTURA` pasa a `EN_CURSO` | `unFallidoConContextoIlegible…` |
| C | se relanza `TransicionNoPermitidaException` | `unaFilaQueOtraPasadaYaCerro…` |
| F | `if (false)` en la comparación de la imagen | `soloLaImagenDifiereNoEsListo` |
| G | `return true` | `soloLosPlugins…` y `soloElSha…` |
| G2 | solo se compara el tamaño del conjunto | las dos de G |
| I | sin `"\n" + eventos` | `alVencerElPlazo…` |
| J | logCore y logInit a `""` | `alVencerElPlazo…` |
| J2 | solo logInit a `""` | `alVencerElPlazo…` |

La mutación C cae porque un `throw` dentro de un `catch` no lo atrapa el `catch` hermano, así que la excepción sale de `revisar()`.

**¿El doble de `VigilanciaDobleTest` esconde algo? Basta.** Las reglas A, B, C, I y J son lógica pura de `VigilanciaDespliegues`, y el doble solo le da las entradas.
- **Dónde lee:** `estado` y `contextoFallo` responden por namespace (`synkra-vv`, `synkra-yy`…). Así la prueba también fija el mapeo `"synkra-" + pais`: si fuera otro, `vv` no llegaría a LISTO.
- **Escrituras:** `construir`, `aplicar` y `existeNamespace` lanzan `UnsupportedOperationException`. Cualquier escritura la atraparía el `catch` de la fila, la fila no se cerraría o el conteo no cuadraría, y la prueba caería.
- **Orden:** el orden de `aplicando()` sí importa, y la prueba lo respeta. `xx` (T) va antes que `vv` (T+5 s), así que una excepción que no se aísle deja sin revisar la fila siguiente.
- **Lo que se pierde al no delegar en el simulador:** el conteo de peticiones y la semántica real de `LectorPais`. Ambas ya las cubren `VigilanciaTest` (cero escrituras, `cl`, `ar`, `uy`, `ec`) y `EstadoRolloutTest`.

**F y G en una clase aparte (`VigilanciaCoincideTest`): cubren lo pedido.** Hay tres casos, cada uno con una sola diferencia: los plugins (`kyc-co`), la imagen (B) o el sha. El clúster está `listo` (con `marcaListo`). Cada caso afirma 0 al minuto y `PLAZO\nNO_APLICADO` a los 2 minutos, sobre su propia carpeta. Separarlos de `VigilanciaTest` para no tocar la salida literal de CA-1 es razonable.

**F-02 (resuelto).** La sección «Ronda 2» de `bitacoras/U07-E.md` enumera lo que 4869522 tocó en pruebas.

**F-03.** El orquestador lo aceptó y no hay nada que verificar.

## Hallazgos

### F-04 · AMARILLO · `VigilanciaDobleTest.java`, `unaFilaQueLanzaNoDetieneLaPasada` · El cierre de la fila 970 no va en un `finally`
Si esta prueba falla, la fila `xx` queda `aplicando`, y las otras tres pruebas del doble también caen en cuanto vuelven a pasar por ella. Las mutaciones A y A2 tumban las 4 pruebas por una sola regla. No esconde nada, porque la prueba dueña también cae, pero el diagnóstico queda ruidoso. Lo correcto es poner el `actualizarResultado(970, …)` en un `finally`, como ya se hace con el gancho de `ww`.

### F-05 · AMARILLO · `VigilanciaDespliegues.java:113` · «Sin llamar a nada más» si no hay cliente: no hay prueba que muerda
La mutación K cambia el primer `if (!kubernetes.disponible())` de `leer` por `if (false)`, y las 30 pruebas quedan en verde. Es casi equivalente: `AdaptadorFabric8.leerPais` lanza `ClienteNoDisponibleException` (línea 102), que acaba en SIN_LECTURA de todos modos, y CA-4 sigue pasando. Solo se pierde la parte de la decisión 2 que dice «sin llamar a nada más». Bastaría un caso en el doble con `disponible()` en falso y `leerPais` lanzando.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. C-122, C-125 y C-126 ya están registradas.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-E-r2/`:
- Criterios: `ca1.txt`, `ca2.txt`, `ca3.txt`, `ca4.txt`, `ca6.txt`, `ca7.txt`
- Mutaciones: `mut/{A,A2,B,B2,C,F,G,G2,I,J,J2,K}.txt`
- Guiones: `mut.sh`, `ca4.sh`

No escribí en el worktree: este mensaje es el informe completo, para que lo guardes tú en `revisiones/U07-E/ronda-2.md`.

```
VEREDICTO: VERDE
AMARILLO|VigilanciaDobleTest.java:unaFilaQueLanzaNoDetieneLaPasada|El cierre de la fila 970 no va en finally: un fallo cae en cascada sobre las otras tres pruebas del doble
AMARILLO|VigilanciaDespliegues.java:113|«Sin llamar a nada más» sin cliente no tiene prueba que muerda (mutación K casi equivalente)
INFORME: revisiones/U07-E/ronda-2.md
```
