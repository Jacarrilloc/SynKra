# Plan de tareas: U07 Despliegue y reversión

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian.

Raíz del proyecto. Añade el paquete `despliegue` a `backend/` y la migración `V4__despliegue.sql`.
Las tareas van en capas (plan → confirmación → vigilancia → reversión), cada una con su prueba.
Resumen en `aidlc-docs/construction/U07-despliegue/code/`.

## Contexto

- **Historias dueñas:** H-09, H-11, H-12, H-13, H-14, H-16, H-17, H-20. **Contribuye a:** H-06, H-08, H-24.
- **Depende de:** U01, U03, U04, U05, U06. **Diseño:** `../U07-despliegue/functional-design/` (R-D1 a R-D13).
- **Tabla:** `dep_plan`. **Pruebas** contra el servidor simulado de Fabric8; **ninguna tarea aplica
  nada a un clúster real** (AUTONOMIA-01).

## Tareas

### Capa 1: verificación y plan
- [ ] **U07-T01 · Migración y repositorio de planes**
  **Criterio:** `./gradlew :backend:test --tests '*PlanRepositorioTest'` pasa. · H-11

- [ ] **U07-T02 · País permitido (R-D1)**
  **Criterio:** `./gradlew :backend:test --tests '*PaisPermitidoTest'` pasa con `co`, `pe` aceptados y `mx`, `default`, `kube-system`, `PE` rechazados (422) sin ninguna llamada a U05. · H-13

- [ ] **U07-T03 · Verificación en vivo (R-D2)**
  `POST /api/verificaciones` con U01 y el catálogo de U04.
  **Criterio:** `./gradlew :backend:test --tests '*VerificacionVivoTest'` pasa con S4 (alternativas presentes) y no crea filas en `dep_plan`. · H-09, H-08

- [ ] **U07-T04 · Crear plan con resumen y cambios (R-D3, R-D4)**
  `POST /api/despliegues`.
  **Criterio:** `./gradlew :backend:test --tests '*CrearPlanTest'` pasa: válido (201 `pendiente`, resumen con digest y hashes), inválido (422, sin fila), cambios contra lo que corre (añadidos, quitados, iguales), plan idéntico permitido con "sin cambios". · H-11

- [ ] **U07-T05 · Propiedad del cálculo de cambios (PBT-03)**
  **Criterio:** `./gradlew :backend:test --tests '*CambiosPropiedades'` pasa: aplicar los cambios a lo que corre da la selección. · H-11

- [ ] **U07-T06 · Un plan sin confirmar nunca aplica (R-D13) y vence (Q6)**
  **Criterio:** `./gradlew :backend:test --tests '*PlanPendienteTest'` pasa: tras crear planes, el servidor simulado registra **cero** escrituras; con el reloj avanzado 31 min, el plan queda `vencido` y confirmar da 410. · H-17

### Capa 2: confirmación
- [ ] **U07-T07 · Confirmar (R-D5 a R-D9)**
  `POST /api/despliegues/{id}/confirmacion`: re-verifica, comprueba que no haya otro `aplicando`, aplica con U05, registra en U06 y responde 202.
  **Criterio:** `./gradlew :backend:test --tests '*ConfirmarTest'` pasa con: confirmación correcta (objetos en el simulador, fila `aplicando`), segunda confirmación (409), catálogo cambiado entre medias (422), otro despliegue en curso (409), confirmado por otro Desplegador (se guardan los dos usuarios). · H-12

### Capa 3: vigilancia
- [ ] **U07-T08 · Vigilancia asíncrona sin reintentos (R-D10)**
  Cada 5 s hasta 3 min (configurables; en pruebas, milisegundos).
  **Criterio:** `./gradlew :backend:test --tests '*VigilanciaTest'` pasa: pod listo → `listo`; plazo vencido → `fallido`; `CrashLoopBackOff` → `fallido` con contexto; en ningún caso hay una segunda escritura en el simulador. · H-14, H-16

- [ ] **U07-T09 · Retomar la vigilancia tras un reinicio (R-D12)**
  **Criterio:** `./gradlew :backend:test --tests '*RetomarVigilanciaTest'` pasa: con una fila `aplicando` al arrancar, se vigila y no se reaplica (cero escrituras). · H-14

### Capa 4: reversión
- [ ] **U07-T10 · Reversión como plan nuevo (R-D11)**
  `POST /api/despliegues/{id}/reversion`.
  **Criterio:** `./gradlew :backend:test --tests '*ReversionTest'` pasa: origen `listo` → plan `reversion` `pendiente`; origen `fallido` → 422; registro manipulado con un plugin incompatible → 422; tras confirmar, el registro apunta al origen. · H-20

### Cierre
- [ ] **U07-T11 · Plugin de otra carpeta en un plan (P4)**
  **Criterio:** `./gradlew :backend:test --tests '*CarpetaEquivocadaPlanTest'` pasa con `otro-core` + `kyc-pe` → 422. · H-06

- [ ] **U07-T12 · Fronteras del módulo**
  **Criterio:** `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `despliegue`, que solo usa `catalogo.api`, `kubernetes.api` y `registro.api`. · H-12

- [ ] **U07-T13 · Resumen**
  **Criterio:** `aidlc-docs/construction/U07-despliegue/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*despliegue*'`. · H-12

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-09 | T03 |
| H-11 | T01, T04, T05 |
| H-12 | T07, T12, T13 |
| H-13 | T02 |
| H-14 | T08, T09 |
| H-16 | T08 |
| H-17 | T06 |
| H-20 | T10 |
| H-06, H-08 (contribuye) | T11, T03 |

**Terminada cuando:** `./gradlew :backend:test --tests '*despliegue*'` pasa, incluida `PlanPendienteTest`.
