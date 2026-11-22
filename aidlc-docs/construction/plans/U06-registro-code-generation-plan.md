# Plan de tareas: U06 Registro y vista general

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian.

Raíz del proyecto. Añade el paquete `registro` a `backend/` y la migración `V3__registro.sql`.
Resumen en `aidlc-docs/construction/U06-registro/code/`.

## Contexto

- **Historias dueñas:** H-18, H-19. **Contribuye a:** H-11 (lo que corre hoy), H-16, H-20.
- **Depende de:** U03, U05 (`leerPais`). **Diseño:** `../U06-registro/functional-design/` (R-R1 a R-R8).
- **Tablas:** `reg_despliegue`, `reg_plugin`, `reg_contexto`. **Expone:** `RegistroApi`.

## Tareas

- [ ] **U06-T01 · Migración y repositorios**
  **Criterio:** `./gradlew :backend:test --tests '*RegistroRepositoriosTest'` pasa y `sqlite3 $SYNKRA_DATA/synkra.db '.tables'` lista las tres `reg_*`. · H-18

- [ ] **U06-T02 · `registrar` y `actualizarResultado` con sus transiciones (R-R1 a R-R3)**
  **Criterio:** `./gradlew :backend:test --tests '*TransicionesRegistroTest'` pasa: `aplicando→listo` y `aplicando→fallido` permitidas; `listo→fallido` y `fallido→listo` rechazadas; `fallido` sin contexto rechazado. · H-18

- [ ] **U06-T03 · Contexto truncado (R-R4)**
  **Criterio:** `./gradlew :backend:test --tests '*ContextoFalloTest'` pasa: log de 1000 líneas guarda 200; texto de 100 KB guarda 64 KB con `[truncado]`. · H-16

- [ ] **U06-T04 · Historial por país y detalle (R-R5)**
  `GET /api/paises/{pais}/despliegues` y `GET /api/despliegues/{id}`.
  **Criterio:** `./gradlew :backend:test --tests '*HistorialTest'` pasa y `curl -s localhost:8080/api/paises/pe/despliegues | jq '.[0].confirmado_en'` es el más reciente. · H-18

- [ ] **U06-T05 · Vista general con concordancia (R-R6, R-R7)**
  `GET /api/paises` sobre los países de `SYNKRA_PAISES`, con U05 `leerPais`.
  **Criterio:** `./gradlew :backend:test --tests '*VistaGeneralTest'` pasa con: país igual al registro (`concuerda=true`), ConfigMap con otro hash (`false`), imagen con otro digest (`false`), país sin nada (`true`). · H-19

- [ ] **U06-T06 · Sobrevive al reinicio (R-R8)**
  **Criterio:** `./gradlew :backend:test --tests '*RegistroReinicioTest'` pasa: se registra, se cierra el contexto de Spring, se abre otro sobre el mismo archivo SQLite y el historial es igual. · H-18

- [ ] **U06-T07 · Fronteras del módulo**
  **Criterio:** `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `registro`. · H-18

- [ ] **U06-T08 · Resumen**
  **Criterio:** `aidlc-docs/construction/U06-registro/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*registro*'`. · H-18

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-18 | T01, T02, T04, T06, T07, T08 |
| H-19 | T05 |
| H-16 (contribuye) | T03 |

**Terminada cuando:** `./gradlew :backend:test --tests '*registro*'` pasa.
