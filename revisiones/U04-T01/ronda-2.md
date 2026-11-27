# Ronda 2 — U04-T01

VEREDICTO: NO-VERDE

Los cuatro criterios pasan con mi propia ejecución, y los cuatro hallazgos de la ronda 1 quedan cerrados. Lo que bloquea es un NARANJA nuevo: siete decisiones del esquema siguen sin una prueba que las ate.

## Comprobaciones previas
- **Hash de la tarea:** `19573bfd02b1f5c05f20af54851f253f94af7216`. Coincide.
- **Commits:** HEAD es `a06df86`. La base de fusión con `origin/main` es `02d1e24`. Los commits nuevos son `5101418` y `a06df86`.
- **Estado del worktree:** `git status --short` salió vacío antes y después de la revisión.
- **Dónde se hicieron mutaciones y sondas:** en copias `git archive HEAD`, en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04r2/`.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (raíz del worktree) | Resultado |
|---|---|---|---|
| 1 | Repositorios sin caché ni saltos | `./gradlew :backend:test --rerun --tests '*CatalogoRepositoriosTest'` y después el `grep` del XML | pasa: `BUILD SUCCESSFUL` y `tests="15" skipped="0" failures="0" errors="0"`. El XML es de las 11:05:37 y la corrida empezó a las 11:05:36. La primera prueba tarda 0,498 s por el contexto, y las 15 corrieron. |
| 2 | Lectura de vuelta sobre el JAR | `bootJar`, `java -jar … --server.port=18080` con un `SYNKRA_DATA` nuevo, la espera `timeout 90` (salió con 0) y la línea Python del criterio | pasa: `['cat_core','cat_etiqueta','cat_plugin','cat_version_core']`, `[('1',),('2',)]`, `[('cat_plugin_sha256',)]` y `('wal',)` |
| 3 | Todo el backend | `./gradlew :backend:test --rerun` y el bucle sobre los XML | pasa: `BUILD SUCCESSFUL`. Los 9 XML salen con skipped, failures y errors en 0: Fronteras 3, SynkraApp 2, Catalogo 15, BaseDeDatos 4, Dialecto 6, ErrorApi 13, RepositorioSqlite 4, RutaDatos 1, Salud 3. |
| 4 | Diff acotado | `git diff --stat origin/main...HEAD` | pasa: 17 archivos, todos dentro de la lista. No hay cambios en `verificador/`, en la raíz de Gradle ni en `settings.gradle.kts`. |

## Cierre de los hallazgos de la ronda 1
- **F-01 (ROJO): cerrado.**
  - Los cuatro repositorios extienden `Repository<T, String>` y declaran solo `findAll` y consultas derivadas por clave completa o por campo de búsqueda. No queda ningún `save`, `delete*`, `findById` ni `existsById`.
  - Volví a `ListCrudRepository` en cada repositorio por separado (M16a a M16d) y las cuatro mutaciones hacen fallar `losRepositoriosNoOfreceOperacionesCrud`.
  - Hay un agujero en esa prueba (F-06).
- **F-02 (NARANJA): cerrado para las nueve mutaciones pedidas.** Volví a correr M02, M03, M05c, M06a, M06b, M08, M09, M11 y M12, y las nueve dan ahora `BUILD FAILED`, cada una en su prueba. M06a, la que el codificador no corrió, falla en `dosPluginsConElMismoSha256SeAceptanYElIndiceNoEsUnico`. Quedan siete mutaciones vecinas que sobreviven (F-05).
- **F-03 (AMARILLO): cerrado.** La prueba usa `startsWith("1","2")`.
- **F-04 (AMARILLO): cerrado.** CA-4 tiene ahora comando y salida en la bitácora.
  - Esa salida se tomó en `5101418`: muestra 93 líneas de bitácora y 595 inserciones, frente a las 115 y 617 de HEAD.
  - La lista de archivos de código es idéntica a la de HEAD. La diferencia es inevitable, porque el commit de la bitácora se cambia a sí mismo, así que no la cuento como evidencia vieja.
- **Desviación declarada (el delete RESTRICT llega como `SQLITE_CONSTRAINT_TRIGGER`): confirmada.** Lo sondeé con `sqlite3` de Python (SQLite 3.53.1), borrando un padre con hijos:
  - con `ON DELETE RESTRICT` sale `SQLITE_CONSTRAINT_TRIGGER`;
  - sin cláusula (NO ACTION) sale `SQLITE_CONSTRAINT_FOREIGNKEY`;
  - con `CASCADE` el borrado se completa.

  En los dos primeros casos el mensaje es `FOREIGN KEY constraint failed`. Por eso la prueba no distingue RESTRICT de NO ACTION (F-05).

## Hallazgos

### F-05 · NARANJA · `CatalogoRepositoriosTest` y `V2__catalogo.sql` · F-02 residual: siete decisiones del esquema siguen sin una prueba que las ate (barrido de clase)
Estas mutaciones pasan `CatalogoRepositoriosTest` y `BaseDeDatosTest` con `BUILD SUCCESSFUL`.

**RESTRICT.** La decisión 1 dice «todas las claves foráneas con `ON DELETE RESTRICT`». Hoy solo se prueba una de las tres, y con un mensaje que no distingue RESTRICT de NO ACTION:
- M09b: quitar los tres `ON DELETE RESTRICT`, que deja NO ACTION. La prueba solo afirma `"FOREIGN KEY constraint failed"`, y esa cadena sale igual con NO ACTION.
- M09c: `CASCADE` solo en `cat_plugin.core`.
- M09d: `CASCADE` solo en la FK compuesta de `cat_etiqueta`.

**NOT NULL.** La decisión 1 pide NOT NULL explícito en estas cuatro columnas. Esta clase debí barrerla completa en la ronda 1: allí solo pedí `imagen` y `subido_en` como TEXT.
- M11b: `subido_en INTEGER`, anulable.
- M12b: `registrado_en INTEGER`, anulable.
- M12c: `registrado_por` anulable.
- M12d: `subido_por` anulable.

**Lo que se exige:**
- Para RESTRICT, cualquiera de estas dos vías:
  - Afirmar `SQLITE_CONSTRAINT_TRIGGER`, que según la sonda separa RESTRICT de NO ACTION, y borrar también un core con un plugin hijo y un plugin con una etiqueta hija.
  - Leer `pragma_foreign_key_list` de las tres tablas y afirmar `on_delete='RESTRICT'`.
- Para NOT NULL: rechazar un nulo en cada una de las cuatro columnas, o afirmar `notnull=1` desde `pragma_table_info`.

### F-06 · AMARILLO · `CatalogoRepositoriosTest.losRepositoriosNoOfreceOperacionesCrud` · la prueba solo mira la herencia de `CrudRepository`
M17 declara a mano `<S extends VersionCore> S save(S e)` y `void deleteById(String id)` dentro de `VersionCoreRepositorio`, que sigue extendiendo `Repository`. Compila, y la suite pasa con `BUILD SUCCESSFUL`. Spring Data atiende esos métodos con la implementación base, así que la vía de F-01 puede volver sin que nada falle. Sugerencia: recorrer `getMethods()` de los cuatro repositorios y rechazar `save*`, `delete*`, `findById` y `existsById`.

### F-07 · AMARILLO · `CoreRepositorio`, `VersionCoreRepositorio` y `EtiquetaRepositorio` · `findAll` declarado sin ejercer
Solo `plugins.findAll()` aparece en las pruebas. Es una consulta trivial y no bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Un traductor de `SQLITE_CONSTRAINT_*` a excepciones de Spring para U04-T02. Debe incluir `SQLITE_CONSTRAINT_TRIGGER`, que es el código con que llega una violación de RESTRICT. Sigue en pie desde la ronda 1.

## Rutas de transcripciones largas
Todas en `/tmp/claude-0/-home-user/3599ea51-3a46-5b6e-ad98-289480e03b5e/scratchpad/u04r2/`:
- `ca1.log`, `ca3.log`, `ca2-jar.log` y `ca2-app.log`
- `mut-originales.txt` (las nueve de la ronda 1) y `mut-nuevas.txt` (M16a a M16d, M17, M09b a M09d, M11b, M12b a M12d)
- `mut-M*.log`, uno por mutación
- `mutar.sh` (el script de mutaciones), `mut/` (la copia mutada) y `orig/` (la copia limpia para el diff)

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/test/java/io/synkra/catalogo/CatalogoRepositoriosTest.java (V2__catalogo.sql)|F-02 residual: RESTRICT en 2 de 3 FK sin prueba y la existente no distingue NO ACTION; NOT NULL de registrado_en/subido_en/registrado_por/subido_por sin prueba (M09b, M09c, M09d, M11b, M12b, M12c, M12d sobreviven)
AMARILLO|backend/src/test/java/io/synkra/catalogo/CatalogoRepositoriosTest.java:losRepositoriosNoOfreceOperacionesCrud|solo mira la herencia de CrudRepository: save/deleteById declarados a mano pasan (M17)
AMARILLO|backend/src/main/java/io/synkra/catalogo/{Core,VersionCore,Etiqueta}Repositorio.java|findAll declarado sin ejercer
INFORME: revisiones/U04-T01/ronda-2.md
```
