# Ronda 2 — U01-T02

VEREDICTO: VERDE

Los cinco criterios pasan cuando los corro yo, y F-01 y F-02 están corregidos.

Revisé HEAD fa75ffc. El retrabajo es `git diff c8a9d5b..HEAD` y toca tres archivos:
- `verificador/build.gradle.kts`: cambian 3 líneas, solo las versiones.
- `bitacoras/U01-T02.md`: añade la sección de la ronda 2.
- `revisiones/U01-T02/ronda-1-respuesta.md`: la respuesta al informe anterior.

El hash del archivo de la tarea sigue siendo 43fbf85a5e1bc20cdb7ac010cdb83cf0ce8160dc, sin cambios. No hay desborde de alcance.

## Criterios de aceptación, verificados por mí
Todos corridos desde /home/user/wt-U01-T02.

| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Sin framework en ejecución | `./gradlew -q :verificador:dependencies --configuration runtimeClasspath \| grep -ciE "spring\|fabric8"` | `0`, pasa |
| 2a | SnakeYAML Engine en ejecución | `... runtimeClasspath \| grep -c 'org.snakeyaml:snakeyaml-engine'` | `1`, pasa |
| 2b | jqwik fuera de ejecución | `... runtimeClasspath \| grep -c 'net.jqwik'` | `0`, pasa |
| 3 | JUnit 5 y jqwik en pruebas | `... testRuntimeClasspath \| grep -cE 'net\.jqwik:jqwik\|org\.junit\.jupiter'` | `18`, pasa |
| 4 | Existen los test fixtures | `./gradlew -q :verificador:tasks --all \| grep -E '^testFixturesJar'` | una línea `testFixturesJar - ...`, pasa |
| 5 | El proyecto construye | `./gradlew :verificador:check --rerun` | `BUILD SUCCESSFUL in 654ms`, pasa |

CA-5 termina en menos de un segundo con `test NO-SOURCE`. Igual que en la ronda 1, está explicado: la tarea prohíbe clases y su plan dice que las pruebas son los criterios. No hay prueba prometida que se haya saltado, ni XML que contar.

## Verificación de los hallazgos de la ronda 1

### F-01 (NARANJA): versiones. Cerrado.
- **Ejecución**: el `runtimeClasspath` contiene solo `org.snakeyaml:snakeyaml-engine:3.2`, sin dependencias transitivas.
- **Pruebas**: el `testRuntimeClasspath` resuelve junit-bom 5.14.4, junit-jupiter 5.14.4, junit-platform-launcher 1.14.4 y jqwik 1.10.2 con sus submódulos en 1.10.2. jqwik y el BOM quedan alineados en junit-platform 1.14.4, sin conflictos de versión.
- **Contra Maven Central**: esas versiones coinciden con lo que consulté en ronda 1 en `maven-metadata.xml`. snakeyaml-engine 3.2 y jqwik 1.10.2 son la release más reciente, y 5.14.4 es la última de la línea 5, la que pide la tarea («JUnit 5»).
- **Justificación**: la bitácora explica por qué adopta snakeyaml-engine 3.x (todavía no hay código que use su API) y por qué descarta JUnit 6. Me parece suficiente.

### F-02 (AMARILLO): evidencia de CA-5. Cerrado.
La bitácora de la ronda 2 registra el comando literal `./gradlew :verificador:check --rerun`. Su salida coincide con la mía y es coherente con el diff actual, porque la bitácora también muestra `snakeyaml-engine:3.2`.

## Hallazgos
Ninguno en pie.

## Tareas candidatas (defectos reales fuera de alcance)
- Sigue igual que en la ronda 1: cuando U01-T12 ponga la suite en `testFixtures`, necesitará JUnit y jqwik en `testFixturesApi` o en `testFixturesImplementation`.

## Estado del worktree
Limpio. `git status --short --ignored` muestra solo `.gradle/` y `verificador/build/`, que son cachés ignorados.

## Rutas de transcripciones largas
- Ninguna.

```
VEREDICTO: VERDE
INFORME: revisiones/U01-T02/ronda-2.md
```
