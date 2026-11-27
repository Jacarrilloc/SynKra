# Ronda 3 — U04-T01

VEREDICTO: VERDE

Los cuatro criterios pasan con mi propia ejecución y los hallazgos de las rondas 1 y 2 quedan cerrados. Las 29 mutaciones que corrí en copias hacen fallar la suite. No encontré ningún ROJO ni NARANJA.

## Comprobaciones previas
- **Hash de la tarea:** `19573bfd02b1f5c05f20af54851f253f94af7216`. Coincide.
- **HEAD y base:** HEAD es `5bfbebd`. La base de fusión con `origin/main` es `02d1e24`.
- **Estado del worktree:** `git status --short` salió vacío antes y después de la revisión.
- **Dónde se probó:** las mutaciones corrieron en copias `git archive HEAD`, en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04r2/`. La copia limpia de referencia se regeneró a partir de este HEAD.
- **Qué cambió en el código desde la ronda 2:** solo `CatalogoRepositoriosTest.java`, con 46 líneas. El código de producción y la migración no se tocaron.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | Repositorios sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*CatalogoRepositoriosTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL` y `tests="19" skipped="0" failures="0" errors="0"`. El XML es de las 11:12:26 y la corrida empezó a las 11:12:25. |
| 2 | Lectura de vuelta sobre el JAR | `bootJar`, `java -jar … --server.port=18080` con un `SYNKRA_DATA` nuevo, la espera `timeout 90` (salió con 0) y la línea Python del criterio | pasa: `['cat_core','cat_etiqueta','cat_plugin','cat_version_core']`, `[('1',),('2',)]`, `[('cat_plugin_sha256',)]` y `('wal',)` |
| 3 | Todo el backend | `./gradlew :backend:test --rerun` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Los 9 XML salen con skipped, failures y errors en 0 (Fronteras 3, SynkraApp 2, Catalogo 19, BaseDeDatos 4, Dialecto 6, ErrorApi 13, RepositorioSqlite 4, RutaDatos 1, Salud 3). Las marcas de tiempo son de esta corrida. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 19 archivos, todos dentro de la lista. No hay cambios en `verificador/`, en la raíz de Gradle ni en `settings.gradle.kts`. |

## Cierre de los hallazgos de la ronda 2
- **F-05 (NARANJA): cerrado.** Las siete mutaciones que sobrevivían fallan ahora con `BUILD FAILED`:
  - M09b, sin `ON DELETE RESTRICT`: fallan 3 pruebas.
  - M09c, con `CASCADE` en `cat_plugin.core`: falla `lasTresFkDeclaranOnDeleteRestrict`.
  - M09d, con `CASCADE` en la FK compuesta de `cat_etiqueta`: fallan 2 pruebas.
  - M11b, M12b, M12c y M12d, cada una con una columna anulable: cada una hace fallar `lasColumnasDeAuditoriaSonNotNull`.
  - La afirmación de `SQLITE_CONSTRAINT_TRIGGER` ya distingue RESTRICT de NO ACTION, que era lo que mostraba la sonda de la ronda 2.
- **F-06 (AMARILLO): cerrado.** M17 declara a mano `save` y `deleteById` en `VersionCoreRepositorio`, y ahora hace fallar `losRepositoriosNoOfreceOperacionesCrud`. M16a a M16d, que vuelven a `ListCrudRepository`, siguen fallando.
- **F-07 (AMARILLO): cerrado.** `findAllDeLosRepositoriosDevuelveLoGuardado` ejerce `findAll` de Core, VersionCore y Etiqueta. M07 (`save` en vez de `insert`) la hace fallar.

## Regresión de las mutaciones de las rondas 1 y 2
Las 29 fallan, todas con `BUILD FAILED`:
- **Ronda 1, las nueve que sobrevivían:** M02, M03, M05c, M06a, M06b, M08, M09, M11 y M12.
- **Ronda 1, las que ya fallaban:** M01, M04, M05a, M05b, M07, M10, M13 y M14.
- **Ronda 2:** M09b, M09c, M09d, M11b, M12b, M12c, M12d, M16a a M16d y M17.

M07 la reescribí con otro `sed` sobre `plantilla.insert(...)`, con la misma intención que en la ronda 1. Falla en 6 pruebas.

## Hallazgos
Ninguno en ROJO, NARANJA ni AMARILLO.

Dos notas que no son hallazgos:
- En la ronda 3, la bitácora no registra el rojo previo de las pruebas nuevas. Mis mutaciones cumplen ese papel: las 4 pruebas nuevas fallan cuando se quita la decisión que vigilan.
- La evidencia de CA-4 en la bitácora se tomó en `ff09ec2`, antes del commit de la bitácora. La lista de archivos coincide con la de HEAD y solo difiere el conteo de la bitácora. Es inevitable.

## Tareas candidatas (defectos reales fuera de alcance)
- Un traductor de `SQLITE_CONSTRAINT_*` a excepciones de Spring para U04-T02. Debe incluir `SQLITE_CONSTRAINT_TRIGGER`, que es el código con que llega una violación de RESTRICT. Sigue en pie desde la ronda 1.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04r2/`:
- `r3-ca1.log`, `r3-ca3.log`, `r3-jar.log` y `r3-app.log`
- `mut-r3.txt` (21 mutaciones) y `mut-r3b.txt` (las 8 de la ronda 1 que ya fallaban)
- `mut-M*.log`, uno por mutación
- `mutar.sh` (el script de mutaciones), `mut/` (la copia mutada) y `orig/` (la copia limpia de HEAD `5bfbebd`)

```
VEREDICTO: VERDE
INFORME: revisiones/U04-T01/ronda-3.md
```
