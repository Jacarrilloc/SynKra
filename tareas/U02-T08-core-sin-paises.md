# U02-T08: el core no conoce países (P3)

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T07

---

## Alcance

**Dentro** (una línea, concreta):

> Una prueba `CoreSinPaisesTest` en `core/src/test` que recorre `core/src/main` y falla si encuentra el nombre o el código de un país, más el mismo control como comando de shell en `scripts/control-core-sin-paises.sh`; si hoy algo nombra un país, se quita.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Controlar `plugins/**` (los plugins sí son de país) o `backend/` (el backend sí conoce países: namespaces `synkra-<país>`).
- Cambiar el comportamiento del core.
- Cambios en `verificador/` y `plugin-api/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T08)
- `aidlc-docs/inception/application-design/components.md` (C03: «No hace: no tiene lógica de país (P3)»)
- `core/src/main/**` y `scripts/control-suite.sh` (U01-T13, estilo de los scripts de control)

---

## Decisiones del orquestador

1. **Patrón.** Sin distinguir mayúsculas: `colombia`, `peru`, `perú`, y los códigos `co` y `pe` como **palabra entera entre comillas** (`"co"`, `"pe"`, `'co'`, `'pe'`) o como valor de una etiqueta (`pais: co`, `pais=pe`). El patrón vive en una sola constante de la prueba, y el script usa la misma expresión (`grep -rniE`).
2. **Falsos positivos.** No se añaden excepciones por archivo. Si una palabra del core dispara el patrón sin ser un país (por ejemplo, un `"co"` en otro sentido), se cambia la palabra y la bitácora lo dice.
3. **Alcance de la búsqueda.** Código y recursos de `core/src/main` (Java, `.properties`, `.yaml`, `.yml`). Los nombres de archivo también cuentan.

---

## Criterios de aceptación

- [ ] **CA-1**: el control pasa sobre el core
  ```bash
  ./gradlew :core:test --rerun --tests '*CoreSinPaisesTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*CoreSinPaisesTest.xml
  grep -rniE "colombia|per[uú]|\"co\"|\"pe\"" core/src/main; echo "grep=$?"
  scripts/control-core-sin-paises.sh; echo "salida=$?"
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos; `grep=1` (nada); `salida=0`.

- [ ] **CA-2**: el control muerde
  En una copia (sin `git stash`), con una constante `"pe"` añadida a una clase de `core/src/main`, y en otra con un archivo `core/src/main/resources/colombia.yaml`: la prueba y el script fallan, y el mensaje nombra el archivo y la línea. Se pegan las dos salidas.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :verificador:test --rerun :verificador:check :plugin-api:test --rerun :plugin-api:check :core:test --rerun :core:check
  for f in core/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `core/src/test/**`, `scripts/control-core-sin-paises.sh` (ejecutable, `100755`), lo que haya que quitar de `core/src/main` por el control, la bitácora y `revisiones/U02-T08/`. Se anota el sha.
