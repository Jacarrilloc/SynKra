# U01-T04: `RangoVersion` (R-V2) y sus pruebas

**Unidad:** U01: `Verificador y suite`
**Historias que implementa:** H-07
**Depende de:** U01-T03

---

## Alcance

**Dentro** (una línea, concreta):

> El record `RangoVersion` (`minimo`, `maximo` opcional, `texto`), `parsear` de las cuatro formas de R-V2 a un intervalo `[minimo, maximo)` e `incluye(Version)`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Propiedades de jqwik sobre rangos: son de U01-T05.
- Formas de rango que R-V2 no lista (`^2.1`, `~2.1`, `2.*`, `||`): se rechazan con `RANGO_INVALIDO`, no se implementan.
- Cambios en `Version`, `Resultado`, `Motivo` o `CodigoMotivo`, salvo un defecto que impida esta tarea (se reporta en la bitácora).
- `Descriptor`, `Verificador` y el resto de U01.

---

## Archivos de contexto

- `aidlc-docs/construction/U01-verificador/functional-design/business-rules.md` (R-V2, con su tabla)
- `aidlc-docs/construction/U01-verificador/functional-design/domain-entities.md` (`RangoVersion`)
- `aidlc-docs/construction/plans/U01-verificador-code-generation-plan.md`

---

## Criterios de aceptación

- [ ] **CA-1**: las pruebas de `RangoVersion` pasan, sin caché
  ```bash
  ./gradlew :verificador:test --rerun --tests '*RangoVersionTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: cubre la tabla de R-V2 y no se saltó nada
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' verificador/build/test-results/test/TEST-*RangoVersionTest.xml
  ```
  Esperado: `tests` ≥ 10, `skipped="0" failures="0" errors="0"`. Casos mínimos: `2.x` → `[2.0.0, 3.0.0)`; `2.1.x` → `[2.1.0, 2.2.0)`; `2.1.0` → `[2.1.0, 2.1.1)`; `>=2.1.0 <3.0.0` y `>= 2.1.0  <  3.0.0` → `[2.1.0, 3.0.0)`; `>=2.1.0` → `[2.1.0, ∞)`; `>=3.0.0 <2.0.0`, `2.x.x` y `^2.1` → `RANGO_INVALIDO`; `2.x` incluye `2.0.0` y `2.99.0` y no incluye `3.0.0` ni `1.9.9`.

- [ ] **CA-3**: todo el verificador sigue pasando
  ```bash
  ./gradlew :verificador:check
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- `RangoVersionTest` parametrizada con la tabla de R-V2 y los inválidos.
- Prueba de bordes de `incluye`: el mínimo entra, el máximo no.

**Rojo primero:** prueba antes que código; registra el fallo de CA-1 y luego el mismo comando
pasando.

---

## Notas

- Paquete: `io.synkra.verificador`.
- `RangoVersion.parsear` devuelve `Resultado<RangoVersion>`; un rango inválido nunca lanza
  excepción.
