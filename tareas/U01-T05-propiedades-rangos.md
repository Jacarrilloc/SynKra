# U01-T05: Propiedades de rangos (PBT-03, PBT-07, PBT-08)

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T04

---

## Alcance

**Dentro** (una línea, concreta):

> La clase de pruebas `RangoVersionPropiedades` (jqwik) con sus generadores de `Version` y de `RangoVersion` de las cuatro formas de R-V2, y dos propiedades: «`N.x` incluye `v` ⇔ `v.mayor == N`» (PBT-03) y «`parsear(r.texto())` da el mismo intervalo» (PBT-02, ida y vuelta), cada una con 1000 intentos y la semilla en el reporte (PBT-08).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios en `Version`, `RangoVersion`, `Resultado`, `Motivo` o `CodigoMotivo`. Si una propiedad encuentra un defecto, se registra en la bitácora con el contraejemplo reducido y se para la tarea: el orquestador decide si se corrige aquí.
- Fijar una semilla constante en el código (`@Property(seed = ...)`): la semilla cambia en cada ejecución y solo se **registra**.
- Propiedades del descriptor (U01-T07) y del verificador (U01-T11).
- Generadores en `testFixtures`: viven en `src/test` hasta que otra tarea los necesite.
- Cambios en `build.gradle.kts`: jqwik 1.10.2 ya está declarado.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-logic-model.md` (tabla «Propiedades (PBT parcial)»)
- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V2)
- `verificador/src/main/java/io/synkra/verificador/RangoVersion.java`
- `verificador/src/test/java/io/synkra/verificador/RangoVersionTest.java` (casos de ejemplo ya cubiertos)

---

## Criterios de aceptación

- [ ] **CA-1**: las propiedades pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*RangoVersionPropiedades'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: 1000 intentos por propiedad y la semilla registrada
  ```bash
  grep -o -E 'tries = [0-9]+|checks = [0-9]+|seed = [-0-9]+' verificador/build/test-results/test/TEST-*RangoVersionPropiedades.xml
  ```
  Esperado: al menos 2 líneas `tries = 1000`, al menos 2 líneas `checks = 1000` (ningún intento
  descartado) y una línea `seed = ` por propiedad.

- [ ] **CA-3**: la propiedad de `N.x` detecta un rango mal calculado. Con el máximo de `N.x` cambiado
  a mano a `N+2` en una copia de trabajo, CA-1 falla y el reporte muestra un contraejemplo reducido.
  El cambio se revierte y se pega en la bitácora el par falla-pasa.
  ```bash
  git diff --stat -- verificador/src/main
  ```
  Esperado: vacío al terminar (el cambio de prueba no queda en el commit).

- [ ] **CA-4**: todo el verificador sigue pasando, sin caché
  ```bash
  ./gradlew :verificador:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- Generadores (PBT-07): `N`, `mayor`, `menor` y `parche` en `0..50`; rangos de las cuatro formas de
  R-V2 (`N.x`, `N.M.x`, `N.M.P`, `>=A <B` con `A < B` y `>=A`), con espacios U+0020 variables donde
  R-V2 los permite.
- Las dos propiedades son `@Property(tries = 1000)`. El generador no descarta valores
  (`checks = tries`), así que nada de `Assume` ni filtros que tiren intentos.

**Rojo primero:** CA-3 es el par falla-pasa de esta tarea, porque las propiedades se escriben sobre
código que ya existe.

---

## Notas

- Paquete: `io.synkra.verificador`.
- jqwik escribe `tries`, `checks` y `seed` en el `system-out` del XML de Gradle; se comprobó en esta
  máquina antes de escribir la tarea.
- `.jqwik-database` ya está ignorado (C-02).
