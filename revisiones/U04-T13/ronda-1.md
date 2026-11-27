# Ronda 1 — U04-T13

VEREDICTO: VERDE

Contexto: tarea con hash 5c7c39c3abb833e5ee4c46f756beb73a7d7296a9 (coincide con el esperado). Worktree `/home/casdav/Projects/Master/wt-m6r-U04-T13`, HEAD d05dc32 (coincide con el reportado), un solo commit sobre origin/main. El worktree estaba limpio antes de revisar y sigue limpio después. No se tocó el `stash@{0}` de U04-T07.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | R-C3, sin caché y sin saltos | `./gradlew :backend:test --rerun --tests '*RegistroPermitidoTest'` + grep del XML | pasa: BUILD SUCCESSFUL, `tests="7" skipped="0" failures="0" errors="0"`. Los tiempos por caso no son cero (0,003 s a 1,159 s; el contexto se levanta de verdad) |
| 2 | Bordes de C-33 | `./gradlew :backend:test --rerun --tests '*RegistrarCoreTest'` | pasa: `tests="19" skipped="0" failures="0" errors="0"`, y están las cuatro pruebas nuevas: `laImagenDe255CaracteresSeAcepta`, `laImagenDe256CaracteresDa422`, `elTagDe128CaracteresSeAcepta`, `elTagDe129CaracteresDa422` |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` + bucle de grep sobre los XML | pasa: BUILD SUCCESSFUL, 22 XML con 300 pruebas en total. Ninguna línea con skipped, failures o errors distinto de 0 |
| 4 | El diff queda acotado | `git diff --stat origin/main...HEAD` | pasa: 7 archivos, todos dentro de lo permitido (`catalogo/**` en main y test, `application.properties`, `bitacoras/U04-T13.md`) |

## Comprobaciones adicionales que pidió el orquestador

**Mayúsculas, según el arbitraje.** Las pruebas cubren los dos casos.
- `sinValorNoSeAplicaYElHostNoDistingueMayusculas`: con el valor `LOCALHOST:5001`, `localhost:5001/x@…` se permite.
- `laFormaMalaNoAgregaElMotivoDeRegistro`: con el valor activo, la imagen `LOCALHOST:5001/x@…` da 422 con el motivo `IMAGEN_INVALIDA` (R-C1), no el de R-C3.

Además lo comprobé de punta a punta: arranqué el jar con `SYNKRA_REGISTRO_PERMITIDO=LOCALHOST:5001` en el puerto 18080.
- `docker.io/x@…` devuelve 422 `{"campo":"imagen","motivo":"La imagen debe venir del registro permitido 'localhost:5001'"}`.
- `localhost:5001/x@…` devuelve 201.
- Un `GET /api/cores` posterior lista solo la versión de `localhost:5001`. El rechazo no persistió nada.

**Variable sin definir: el comportamiento es el de antes.**
- Arranqué el jar con `env -u SYNKRA_REGISTRO_PERMITIDO`. `docker.io/x@…` devuelve 201 y la lectura de vuelta con GET muestra el core.
- Las 289 pruebas anteriores al diff siguen verdes sin el valor. `RegistrarCoreTest` usa `registro.local/...` y no define la propiedad.

**Valor inválido.** `SYNKRA_REGISTRO_PERMITIDO='http://x' java -jar …` termina con exit=1 y el mensaje `synkra.registro-permitido (SYNKRA_REGISTRO_PERMITIDO) no es un registro válido: 'http://x'`. Así queda probada también la ruta por variable de entorno, no solo por propiedad. Al terminar, el puerto 18080 quedó libre (verificado con `ss -ltn`).

**El rojo de compilación.** Un rojo de compilación solo prueba que los símbolos no existían, no que las pruebas muerdan. Por eso hice mutaciones en una copia de d05dc32 en mi scratchpad, sin tocar el worktree.
- M1: `permite` siempre devuelve verdadero. Fallan 4 pruebas: `otroRegistro…`, `otroPuerto…`, `unaImagenSinRegistro…` y `sinValor…`.
- M2: se quita `toLowerCase` del valor. Falla `sinValorNoSeAplicaYElHostNoDistingueMayusculas`.
- M3: se quita la validación del arranque. Falla `unValorInvalidoImpideArrancarElContexto`.
- M4: `startsWith` en lugar de `equals`. Falla `otroPuertoOSinPuertoDan422` (el caso `localhost:50010`).

Las pruebas de R-C3 muerden. El rojo de compilación es aceptable aquí porque las mutaciones lo complementan (ver F-01 para el caso de C-33).

**Bordes de C-33 que quedaron sin cambiar.** La decisión 4 pide exactamente esto: anotarlos en la bitácora sin cambiar la expresión. La bitácora los anota y la expresión no cambió. Verifiqué la afirmación con jshell sobre la misma gramática: `mi--repo` da false, `mi__repo` da false y el puerto `99999999` da true. La anotación es correcta. No es defecto de esta tarea; la candidata C-33 sigue abierta para esa parte.

## Hallazgos

### F-01 · AMARILLO · bitacoras/U04-T13.md, «Rojo primero» · El rojo mezcla pruebas de caracterización con pruebas nuevas
La bitácora presenta un único rojo de compilación para `RegistroPermitidoTest` y `RegistrarCoreTest` juntos. Las cuatro pruebas nuevas de C-33 no pueden estar en rojo: fijan un comportamiento que ya existía. Lo comprobé copiando el `RegistrarCoreTest` de d05dc32 sobre origin/main en el scratchpad: `tests="19" skipped="0" failures="0" errors="0"`. Esto es lo esperado para la decisión 4, pero la bitácora debería decir que esas cuatro pasan sobre la base y por qué. No bloquea.

### F-02 · AMARILLO · RegistroPermitido.java:21 · Literal de truncado en el mensaje de arranque
El `100` que recorta el valor dentro del mensaje de la `IllegalStateException` es un literal. No es un punto de decisión de negocio, pero conviene nombrarlo (por ejemplo `MAX_ECO`). No bloquea.

### F-03 · AMARILLO · CatalogoServicio.java:27 · Javadoc de la clase desactualizado
La clase sigue diciendo «R-C1 y R-C2» y ahora también aplica R-C3. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva. La parte de C-33 sobre `mi--repo`, `mi__repo` y el puerto `99999999` sigue pendiente como candidata, según la decisión 4.

## Rutas de transcripciones largas
Todas están en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/`.
- Criterios: `ca1.txt`, `ca2.txt`, `ca3.txt`, `ca3-xml.txt`
- C-33 sobre la base: `base-c33.txt`
- Mutaciones: `mut-M1.txt`, `mut-M2.txt`, `mut-M3.txt`, `mut-M4.txt`
- Arranques del jar: `run-invalido.txt`, `run-ok.txt`, `run-vacio.txt`

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U04-T13.md:Rojo primero|El rojo de compilación mezcla pruebas de caracterización de C-33 que pasan sobre la base
AMARILLO|backend/src/main/java/io/synkra/catalogo/RegistroPermitido.java:21|Literal 100 de truncado en el mensaje de arranque
AMARILLO|backend/src/main/java/io/synkra/catalogo/CatalogoServicio.java:27|Javadoc de la clase no menciona R-C3
INFORME: revisiones/U04-T13/ronda-1.md
```
