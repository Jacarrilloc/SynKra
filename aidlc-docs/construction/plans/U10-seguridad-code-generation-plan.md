# Plan de tareas: U10 Seguridad (Should)

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian. Unidad **Should**: es la primera que se corta si falta tiempo (R9). Sin Functional Design: la matriz de roles es la del PRD §9.2.

Raíz del proyecto. Añade el módulo `seguridad` a `backend/` y su migración de Flyway. Resumen en
`aidlc-docs/construction/U10-seguridad/code/`.

## Contexto

- **Historia dueña:** H-30.
- **Depende de:** U04, U06 y U07 (los endpoints que protege).
- **Mientras no exista:** el backend corre sin autenticación solo en el `kind` local; el script de
  punta a punta de U08 acepta credenciales opcionales para que siga sirviendo después.

## Tareas

- [ ] **U10-T01 · Tabla de usuarios y roles**
  Migración `seg_usuario` y `seg_usuario_rol` (Operador, Publicador, Desplegador, Lector); contraseñas con BCrypt; usuario Operador inicial desde un Secret.
  **Criterio:** `./gradlew :backend:test --tests '*UsuariosRepositorioTest'` pasa y ninguna contraseña queda en claro en `synkra.db`. · H-30

- [ ] **U10-T02 · HTTP Basic y 401**
  **Criterio:** `./gradlew :backend:test --tests '*AutenticacionTest'` pasa; `curl -s -o /dev/null -w '%{http_code}' $API/despliegues` = `401`. · H-30

- [ ] **U10-T03 · Matriz de roles del PRD §9.2**
  Prueba parametrizada: cada endpoint por cada rol, con el código esperado (Lector crea plan → 403; Publicador confirma → 403; Desplegador sube JAR → 403; revertir exige Desplegador).
  **Criterio:** `./gradlew :backend:test --tests '*MatrizRolesTest'` pasa con todas las celdas de la tabla. · H-30

- [ ] **U10-T04 · Excepción del init container**
  `GET /api/artefactos/<sha>` sigue sin credenciales, pero solo desde la red del clúster y solo por hash.
  **Criterio:** `./gradlew :backend:test --tests '*ArtefactosSinCredencialesTest'` pasa; el resto de `/api` exige credenciales. · H-30, H-15

- [ ] **U10-T05 · Gestión de usuarios (Operador)**
  **Criterio:** `curl -u operador:… -X POST $API/usuarios` crea un Lector y `curl -u lector:… -X POST $API/usuarios` = `403`. · H-30

- [ ] **U10-T06 · Registro con autor**
  Cada despliegue y cada reversión guarda el usuario que confirmó.
  **Criterio:** `./gradlew :backend:test --tests '*RegistroAutorTest'` pasa y `curl $API/despliegues/<id>` incluye `autor`. · H-30, H-18

- [ ] **U10-T07 · Resumen**
  **Criterio:** `aidlc-docs/construction/U10-seguridad/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*seguridad*'` y los `curl` de H-30. · H-30

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-30 | T01 a T07 |
| H-15 (contribuye) | T04 |
| H-18 (contribuye) | T06 |

**Terminada cuando:** `./gradlew :backend:test --tests '*seguridad*'` pasa y `./scripts/e2e-api.sh` sigue terminando con 0 usando credenciales.
