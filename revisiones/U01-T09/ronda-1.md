# Ronda 1 — U01-T09

VEREDICTO: NO-VERDE

Contexto: worktree /home/user/wt-m6r-U01-T09, HEAD e13666e, base origin/main 078a4d1. El hash de la tarea coincide (`git hash-object` da 7865240311c637cf568f88bb6e3c13e34a58e3ce). Al terminar, el worktree está limpio (`git status --short` sin salida). Las mutaciones se hicieron en una copia `git archive HEAD` en /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r1/copia.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas de VerificadorTest | `./gradlew :verificador:test --rerun --tests '*VerificadorTest'` + grep del XML | pasa: `tests="13" skipped="0" failures="0" errors="0"`. Tarda 0,077 s, pero son pruebas puras sin estado externo, así que el tiempo cuadra. Cada caso afirma la lista exacta (código\|plugin\|campo) |
| 2 | CatalogoEnMemoria de solo lectura | `--rerun --tests '*CatalogoEnMemoriaTest'` | pasa: 5/0/0/0. Mis mutaciones (quitar la copia, quitar el control de duplicado, clave sin core) las detecta todas |
| 3 | Mutaciones (a)–(d) | copia de archive, una a la vez, CA-1, y revertir | pasa. (a), (b) y (c) fallan en `s7CoreEquivocadoSinOtroMotivo`. (d) falla en `s10…` y `tresApariciones…`. Al revertir: 13/0/0/0, y `cmp` dice que el archivo es idéntico. En el worktree, `git diff --stat -- verificador/src/main` muestra solo Catalogo.java y Verificador.java |
| 4 | R-V4 con las decisiones 1, 2 y 3 | `sed -n '/## R-V4/,/## R-V5/p' …` | pasa. Dice en qué carpeta se busca, el corte y que los repetidos quedan fuera de `porPlugin`. El diff del archivo son solo +6 líneas |
| 5 | C-06 atado por una prueba | `--rerun --tests '*LectorDescriptorTest'` | pasa: 29/0/0/0, con `versionYCoreCompatibleInvalidosLlevanSuCampo` en el XML. La fila C-06 de CANDIDATAS.md está correcta |
| 6 | Sin Spring ni Fabric8 | `dependencies --configuration runtimeClasspath \| grep -ciE …` | pasa: `0` |
| 7 | Todo el verificador | `./gradlew :verificador:test --rerun :verificador:check` | pasa: BUILD SUCCESSFUL, 9 XML, 132 pruebas, todas con `skipped="0"` |
| 8 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 9 archivos, todos de la lista. No hay desborde de alcance |

## Hallazgos

### F-01 · NARANJA · Verificador.java:54-61 / VerificadorTest · Ninguna prueba vigila el corte que sigue a PLUGIN_AUSENTE (decisión 2)
La decisión 2 dice que «el primero que falla corta los siguientes», y eso incluye al paso 2. Probé la mutación b2: PLUGIN_AUSENTE se acumula en vez de cortar y los pasos 3 y 4 siguen como están. Con ella, VerificadorTest sigue dando **13/0/0/0, BUILD SUCCESSFUL** (log en `mut-b2.log`). La mutación sobrevive porque todos los plugins ausentes de la suite (`KYC_MX`) son del mismo core y tienen un rango compatible.

El caso sí se da en la práctica. Corrí una sonda con `d("kyc-zz", 9.0.0, "otro-core", "5.x")`: el código actual da solo `PLUGIN_AUSENTE`, y bajo b2 da 2 motivos (`expected: <1> but was: <2>`).

Falta un caso con un plugin ausente que además sea de otro core o tenga un rango incompatible, y que afirme un único `PLUGIN_AUSENTE`. Esto también responde a la desviación (2), más abajo.

### F-02 · NARANJA · VerificadorTest (s6, s7, s4, dosPlugins…) · La parte de los mensajes de la decisión 4 no tiene cobertura
La decisión 4 pide que los mensajes nombren el plugin y las versiones o cores en juego. Probé la mutación `msg`: los mensajes de CORE_EQUIVOCADO pasan a ser `"core equivocado"` y los de PLUGIN_AUSENTE a `"ausente"`. Aun así, VerificadorTest da **13/0/0/0** (log en `mut-msg.log`).

El mismo hueco aparece en los tres motivos por plugin, PLUGIN_AUSENTE, CORE_EQUIVOCADO y VERSION_INCOMPATIBLE. Solo `s10` comprueba el contenido de un mensaje (contiene "1.3.0" y "2.0.0").

Los mensajes que produce el código hoy sí cumplen la decisión; lo comprobé con la sonda. Lo que falta es una prueba que los ate. Pido al menos una aserción de contenido por cada código: nombre del plugin, más cores o versiones según el caso.

### F-03 · AMARILLO · Verificador.java:84-86 · `texto(Version)` duplica `Version.toString()`
`Version.toString()` ya devuelve `mayor.menor.parche`. Tener los dos es un riesgo de divergencia si `Version` cambia. CatalogoEnMemoria usa `toString`, así que en el mismo diff conviven las dos formas.

### F-04 · AMARILLO · Verificador.java:24-27 · El orden de los PLUGIN_REPETIDO entre nombres distintos no está fijado por ninguna prueba
Con la mutación `hash` (`LinkedHashMap` cambiado por `HashMap`), las pruebas siguen en 13/0/0/0. La implementación usa el orden de primera aparición, que es correcto y determinista, pero ninguna prueba tiene dos nombres repetidos.

### F-05 · AMARILLO · business-rules.md R-V4 · La cabecera sigue diciendo «acumulando motivos»
La lista numerada no cambió: dice «acumulando motivos» y pone `PLUGIN_REPETIDO` como paso 1. El párrafo nuevo dice «a lo sumo un motivo por plugin». Leídos juntos se entienden, pero la frase de cabecera ahora engaña. CA-4 se cumple igual.

### Sondeo adversarial sin defecto
- `null` dentro de `plugins`: lo rechaza `Seleccion`, porque usa `List.copyOf`.
- Catálogo que devuelve `null`: da una NPE con el mensaje «…devolvió null en vez de Optional.empty()».
- El mismo descriptor dos veces da un solo REPETIDO con `[1.3.0, 1.3.0]`. Tres apariciones dan uno solo.
- Plugin con su descriptor en otra carpeta del catálogo (`kyc-xx` declarado en `synkra-core`): da `PLUGIN_AUSENTE`, con un mensaje correcto.
- El descriptor recibido manda sobre el del catálogo.
- `porPlugin` sale en el orden recibido.
- La clave `core + "/" + plugin` no puede colisionar, porque `Descriptor.NOMBRE` no admite `/`.

## Juicio de las desviaciones declaradas
1. **CA-5 sin rojo previo.** Lo acepto. Comprobé que la prueba muerde: si cambio el campo a "versión" en Version.java, o a "rango" en RangoVersion.java, `versionYCoreCompatibleInvalidosLlevanSuCampo` falla.
2. **La mutación (b) quita solo el corte 3→4.** Cumple CA-3(b) al pie de la letra, y quitar el corte entero (`bfull`) también falla S7. Pero el corte 2→3/4 queda sin vigilar: es F-01.
3. **PLUGIN_REPETIDO lista todas las versiones, incluida la primera.** Lo acepto: es compatible con «lista las versiones pedidas».
4. **R-V4 sin renumerar.** Lo acepto. Queda solo la ambigüedad de F-05.
5. **No creó revisiones/U01-T09/.** Correcto: según loop/orquestador.md, ese archivo lo escribe el orquestador.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Rutas de transcripciones largas
En /tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u01t09r1/:
- Criterios: `ca125.txt` y `ca678.txt`.
- Mutaciones: `mut-a.log`, `mut-b.log`, `mut-b2.log`, `mut-bfull.log`, `mut-c.log`, `mut-d.log`, `mut-msg.log`, `mut-hash.log`, `mut-repglobal.log`.
- Generador de las mutaciones: `mutar.py`, y las fuentes mutadas en `mut/`.

```
VEREDICTO: NO-VERDE
NARANJA|Verificador.java:54-61 / VerificadorTest|ninguna prueba vigila el corte tras PLUGIN_AUSENTE (mutación b2 sobrevive, 13/0/0/0)
NARANJA|VerificadorTest (PLUGIN_AUSENTE, CORE_EQUIVOCADO, VERSION_INCOMPATIBLE)|el contenido de los mensajes de la decisión 4 no tiene cobertura (mutación msg sobrevive)
INFORME: revisiones/U01-T09/ronda-1.md
```
