# U04-T05: Propiedad de rutas seguras (PBT-03)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-04
**Depende de:** U04-T04

---

## Alcance

**Dentro** (una línea, concreta):

> `RutasSegurasPropiedades`, una prueba jqwik en el backend que genera JAR con nombres de entrada arbitrarios y hostiles, y afirma que **ninguna entrada que `RecepcionArtefacto` acepta resuelve fuera del directorio de destino**. Además, todo JAR rechazado deja el disco vacío.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar las reglas de U04-T04, salvo que la propiedad encuentre un contraejemplo real. En ese caso se corrige la comprobación de rutas, con el contraejemplo fijado como prueba de ejemplo en `RecepcionArtefactoTest`, y se dice en la bitácora.
- Subida, descriptor y rutas HTTP (U04-T06 y U04-T07).
- La configuración de jqwik del proyecto (C-25).

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-logic-model.md` (PBT-03)
- `tareas/U04-T04-recepcion-jar.md`
- `verificador/build.gradle.kts` (versión de jqwik) y `verificador/src/test/java/io/synkra/verificador/*Propiedades.java` (patrón de generadores)
- `CANDIDATAS.md` (C-25)

---

## Decisiones del orquestador

1. **Dependencia.** `backend` agrega `testImplementation("net.jqwik:jqwik:1.10.2")`, la misma versión que `verificador`. No hay ninguna otra dependencia nueva.
2. **Oráculo independiente.** Para cada entrada aceptada, la prueba resuelve `destino.resolve(nombre).normalize()` con un `destino` absoluto ficticio y afirma `startsWith(destino)`. No llama a la comprobación del código. También afirma que ninguna entrada aceptada es un enlace simbólico.
3. **Generadores.** Nombres compuestos de segmentos tomados de:
   - normales;
   - `..` y `.`;
   - vacíos (`a//b`);
   - con `\`;
   - con letra de unidad;
   - absolutos;
   - unicode;
   - NUL.

   Se mezclan con un peso mínimo del 30 % de hostiles, y algunos casos llevan además el bit de enlace simbólico. Se cuentan con `Statistics` y una puerta: al menos un 20 % de JAR aceptados y un 20 % de rechazados por ejecución, para que la propiedad no sea vacía.
4. **Intentos.** 1000 intentos por propiedad (`tries = 1000`). La semilla se registra en el reporte, como en U01 (PBT-08).

---

## Criterios de aceptación

- [ ] **CA-1**: la propiedad pasa, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*RutasSegurasPropiedades'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RutasSegurasPropiedades.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 2 (la de aceptadas dentro del destino y la de disco vacío tras un rechazo), `skipped="0" failures="0" errors="0"`. La bitácora copia las líneas de `Statistics` con los porcentajes de aceptados y rechazados.

- [ ] **CA-2**: la propiedad detecta una comprobación rota
  La bitácora registra, en una copia, tres mutaciones de la comprobación de rutas de U04-T04, cada una con la propiedad en rojo y el contraejemplo reducido:
  - no mirar `..`;
  - no normalizar `\`;
  - no mirar enlaces.

- [ ] **CA-3**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos en ningún XML.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git diff --stat origin/main...HEAD
  ```
  Esperado:
  - `backend/build.gradle.kts`, solo con jqwik;
  - `RutasSegurasPropiedades.java`;
  - si apareció un contraejemplo, su corrección en `catalogo/**` y la prueba de ejemplo;
  - las pruebas de ejemplo de C-45 en `RecepcionArtefactoTest.java`;
  - la bitácora y `revisiones/U04-T05/`.

---

## Plan de pruebas

- `RutasSegurasPropiedades` llama a `RecepcionArtefacto` con JAR generados en memoria y un `synkra.data` temporal por intento. No hace falta `@SpringBootTest`: el componente se construye a mano si su constructor lo permite. Si no lo permite, se dice en la bitácora.

**Rojo primero:** se registra la propiedad en rojo contra una copia con la comprobación de `..` quitada (CA-2) antes de dar por buena la versión en verde.

---

## Notas

- Si 1000 intentos tardan más de 60 s, se baja a 500 y se anota.
- **C-45.** También se atan en `RecepcionArtefactoTest`, como pruebas de ejemplo, los mutantes que sobrevivieron en U04-T04:
  - un JAR cuya lista de commons difiere de la del flujo (C02);
  - el mismo conjunto de nombres en otro orden entre el índice y las cabeceras locales (C03);
  - nombres duplicados frente a `distinct()` (C21).
  Además, `@Timeout` en las pruebas del límite, para que un corte roto falle en vez de colgarse. Si para algún caso no se puede construir un ZIP, se explica en la bitácora.
