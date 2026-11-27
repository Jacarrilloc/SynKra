# U04-T11: Fronteras del módulo

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-01
**Depende de:** U04-T09 y U04-T10

---

## Alcance

**Dentro** (una línea, concreta):

> Afirmar, con `FronterasModulosTest` (Modulith) e `InterfazCatalogoTest` (ArchUnit), que `catalogo` es un módulo, que solo `catalogo.api` es público y que ningún otro módulo usa sus tipos internos. Además, se decide C-14 y C-36.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambios de comportamiento en `catalogo` o en `plataforma`. Si una regla nueva falla, se mueve el tipo o se cambia su visibilidad, sin cambiar su lógica.
- Módulos que todavía no existen (U06 y U07).

---

## Archivos de contexto

- `backend/src/test/java/io/synkra/` (`FronterasModulosTest`, `catalogo/InterfazCatalogoTest`)
- `backend/src/main/java/io/synkra/catalogo/package-info.java` y `api/package-info.java`
- `CANDIDATAS.md` (C-14, C-36)

---

## Decisiones del orquestador

1. **Reglas que se agregan.**
   - Ningún tipo de `catalogo` (fuera de `api`) es `public`. Ya lo prueba `InterfazCatalogoTest`; se mantiene.
   - `catalogo.api` no depende de tipos internos de `catalogo`.
   - `plataforma` no depende de `catalogo`.
   - `ApplicationModules.of(SynkraApp.class).verify()` pasa.
   - La documentación de Modulith (`Documenter`) se genera en `build/` y no se versiona.
2. **C-36.** `io.synkra.verificador` se declara como módulo compartido (`@ApplicationModule(type = OPEN)` o `sharedModules`, lo que permita Modulith 1.4.14 sin tocar `verificador/`). Si exige tocar `verificador/`, se anota en la bitácora y se deja como está.
3. **C-14.** Se cierra así: los fixtures de `io.synkra.plataforma.prueba` (`ControladorDePrueba`, `ElementoRepositorio`) dejan de entrar en el contexto de todos los `@SpringBootTest` y solo los cargan las pruebas que los usan, con `@Import` o una configuración de prueba explícita. Una prueba afirma que un `@SpringBootTest` cualquiera no tiene el bean `ControladorDePrueba`. Si mover los fixtures rompe `ErrorApiTest` o `ValidacionErroresTest`, se ajusta solo cómo cargan los fixtures. `CANDIDATAS.md` marca C-14 como cerrada.

---

## Criterios de aceptación

- [ ] **CA-1**: las fronteras pasan, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*InterfazCatalogoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml backend/build/test-results/test/TEST-*InterfazCatalogoTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos, con una prueba por cada regla de la decisión 1.

- [ ] **CA-2**: las reglas detectan una violación
  La bitácora registra, en una copia, tres mutaciones con la prueba en rojo:
  - un tipo interno de `catalogo` pasa a `public`;
  - `catalogo.api` importa un tipo interno;
  - `plataforma` importa un tipo de `catalogo`.

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
  Esperado: las pruebas de fronteras, los fixtures de `plataforma.prueba` y las pruebas que los cargan (C-14), `package-info.java` si hace falta, `CANDIDATAS.md` si se cierra C-14 o C-36, la bitácora y `revisiones/U04-T11/`.
