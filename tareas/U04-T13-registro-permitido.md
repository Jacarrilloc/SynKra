# U04-T13: *(Should)* Registro de imágenes permitido (R-C3)

**Unidad:** U04: `Catálogo`
**Historias que implementa:** H-31 (Should)
**Depende de:** U04-T03

---

## Alcance

**Dentro** (una línea, concreta):

> `synkra.registro-permitido` (`SYNKRA_REGISTRO_PERMITIDO`, vacío por defecto). Si tiene valor, `registrarCore` rechaza con 422 `campo = "imagen"` toda imagen cuyo registro no sea exactamente ese. Si está vacío, no se aplica. Además, los bordes sin atar de C-33.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar la gramática de imagen de U04-T02, salvo atar sus bordes (C-33).
- Listas de varios registros y comodines.
- Comprobar que la imagen existe en el registro.

---

## Archivos de contexto

- `aidlc-docs/construction/U04-catalogo/functional-design/business-rules.md` (R-C3)
- `backend/src/main/java/io/synkra/catalogo/` (`CatalogoServicio`, `MotivosCatalogo`)
- `CANDIDATAS.md` (C-33)

---

## Decisiones del orquestador

1. **Comparación.** El registro es la parte antes de la primera `/`, tal como lo separa la expresión de U04-T02, con puerto incluido. La comparación es exacta y sin distinción de mayúsculas en el host (`LOCALHOST:5001` es igual a `localhost:5001`). Una imagen sin registro explícito (`synkra-core@sha256:…`) se rechaza cuando el valor está puesto.
2. **Orden.** Primero la forma de la imagen (R-C1) y después el registro (R-C3). Si la forma es mala, no se agrega el motivo de registro.
3. **Valor inválido.** Un valor de configuración que no es un registro válido según la misma expresión hace fallar el arranque con un mensaje claro, no cada petición.
4. **C-33.** Pruebas de ejemplo en `RegistrarCoreTest`: 255 y 256 caracteres de imagen, y tags de 128 y 129. `mi--repo` y el puerto `99999999` se dejan anotados en la bitácora sin cambiar la expresión.

---

## Criterios de aceptación

- [ ] **CA-1**: R-C3, sin caché y sin saltos
  ```bash
  ./gradlew :backend:test --rerun --tests '*RegistroPermitidoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RegistroPermitidoTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 6, `skipped="0" failures="0" errors="0"`. Las pruebas cubren:
  - con `localhost:5001`, `docker.io/x@sha256:…` da 422 `campo = "imagen"`;
  - `localhost:5001/x@…` se acepta, igual que `LOCALHOST:5001/x@…`;
  - `localhost:5002/x@…` y `localhost/x@…` dan 422;
  - una imagen sin registro da 422;
  - sin el valor, `docker.io/x@…` se acepta;
  - un valor inválido impide arrancar el contexto.

- [ ] **CA-2**: los bordes de C-33
  ```bash
  ./gradlew :backend:test --rerun --tests '*RegistrarCoreTest'
  ```
  Esperado: `BUILD SUCCESSFUL`, con las cuatro pruebas nuevas de la decisión 4.

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
  Esperado: `catalogo/**`, `application.properties`, las pruebas, la bitácora y `revisiones/U04-T13/`.
