# U03-T03: Formato de error único

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-24 (contribuye)
**Depende de:** U03-T02

---

## Alcance

**Dentro** (una línea, concreta):

> El cuerpo `ErrorApi {codigo, mensaje, detalles:[{campo, motivo}]}`, la excepción `ExcepcionApi` (estado HTTP + `ErrorApi`) que lanzan los controladores, y el manejador global que la convierte en la respuesta para 400, 404, 409, 413 y 422.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Traducir el `Resultado` del verificador a `ExcepcionApi`: lo hace cada módulo en su controlador (U04 en adelante). `backend` no depende de `:verificador` en esta tarea.
- Errores de seguridad 401 y 403 (U10).
- Controladores de negocio. El único controlador nuevo es el de prueba, en `src/test`.
- OpenAPI (U03-T08).

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/user-stories/stories.md` (H-24, segundo criterio: mismo formato para todo error)
- `aidlc-docs/inception/application-design/component-methods.md` (qué es un `Motivo`)

---

## Criterios de aceptación

- [ ] **CA-1**: la prueba del formato pasa, sin caché
  ```bash
  ./gradlew :backend:test --rerun --tests '*ErrorApiTest'
  ```
  Esperado: `BUILD SUCCESSFUL`.

- [ ] **CA-2**: lectura de vuelta del reporte: un caso por código y ninguno saltado
  ```bash
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ErrorApiTest.xml
  ```
  Esperado: `tests` ≥ 6, `skipped="0" failures="0" errors="0"`. La prueba usa un controlador de prueba que provoca 400, 404, 409, 413 y 422, y afirma para cada uno el estado y que el cuerpo tiene `codigo`, `mensaje` y `detalles`. Un sexto caso: un JSON mal formado da 400 con el mismo formato, no la página de error por defecto de Spring.

- [ ] **CA-3**: el resto del backend sigue pasando
  ```bash
  ./gradlew :backend:check
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- `ErrorApiTest` con `@WebMvcTest` y un `ControladorDePrueba` en `src/test`.
- Prueba negativa: una excepción no prevista da 500 con el mismo formato y **sin** la traza en el cuerpo.

**Rojo primero:** escribe `ErrorApiTest` primero y registra el fallo.

---

## Notas

- Paquete: `io.synkra.plataforma`.
- `codigo` es una cadena en mayúsculas (`NO_ENCONTRADO`, `CONFLICTO`…); los códigos de negocio los
  definirá cada módulo.
- El manejador también traduce las excepciones propias de Spring (cuerpo ilegible, tamaño excedido)
  al mismo formato.
