# Plan de Code Generation — U-scaffold

**Estado:** Part 1 (Planning). Este plan es la fuente única de verdad para la Parte 2. En este
trabajo **no se ejecuta la Parte 2**: no se escribe ningún archivo de código ni de configuración.
**Etapas de diseño previas:** ninguna. Functional Design, NFR Requirements, NFR Design e
Infrastructure Design se omiten para esta unidad (sin lógica de negocio ni infraestructura).

## Contexto de la unidad

| Campo | Valor |
|---|---|
| Propósito | Dejar listo el monorepo: `pom.xml` raíz, módulos esqueleto, directorios, `.gitignore`, objetivos `build` y `test` del `Makefile`, herramientas de entorno y esqueleto del `README.md` |
| Historias | Ninguna propia (Q6-B). Sostiene a las 37 |
| Requisitos que apoya | NFR-11 (entorno Linux/WSL2), NFR-02 y NFR-04 (estructura), NFR-08 (suite con `LLM_PROVIDER=fake`) |
| Depende de | Nada |
| Lo usan | `U-core`, `U-plugins`, `U-agent`, `U-deploy` |
| Raíz del workspace | `C:\Users\julia\OneDrive\Documents\Universidad\2026-3\Topicos Avnzados en Informatica\SynKra` (código en la raíz, nunca en `aidlc-docs/`) |
| Entorno de ejecución | Linux o WSL2 con `make setup` ya corrido (los comandos de verificación se ejecutan allí) |

## Interfaces y contratos que entrega a las otras unidades

| Entrega | Consumidor | Contrato |
|---|---|---|
| `pom.xml` raíz con gestión de versiones y de plugins Maven | Todas | Cada módulo hereda de `com.synkra:synkra` y no declara versiones propias de dependencias comunes |
| Módulos esqueleto `core`, `plugins`, `plugins/co`, `plugins/pe`, `agent` | U-core, U-plugins, U-agent | Cada unidad reemplaza el contenido de su propio `pom.xml`; nadie edita el de otra |
| Variable `LLM_PROVIDER=fake` en las pruebas | Todas | Surefire la define para toda la suite (AUTONOMIA-02) |
| Objetivos `make build` y `make test` | Todas | `build` empaqueta sin pruebas; `test` corre la suite con `fake` |
| `.gitignore` | Todas | Ignora artefactos de build y estado local del agente; **no** ignora `cambios/` ni `informes/` |

## Decisiones propias de este plan (confirma al aprobar)

1. **Módulos esqueleto en vez de directorios vacíos.** `unit-of-work.md` hablaba de
   "directorios vacíos". Un `pom.xml` raíz que declara módulos sin `pom.xml` propio falla en
   `mvn validate`. Este plan crea el `pom.xml` mínimo de cada módulo y deja que su unidad dueña
   lo reemplace. Así ninguna unidad edita el `pom.xml` raíz para añadir su módulo, y las unidades
   en paralelo no chocan. `unit-of-work.md` se enmienda en consecuencia.
2. **Versión de Spring Boot.** No se fija aquí. La tarea 2 consulta la última versión estable
   en el momento de la generación y la registra en el `pom.xml` raíz. Si prefieres una versión
   concreta, indícala al aprobar.
3. **Herramientas de entorno.** `doctor.sh` y `setup-linux.sh` no cubren `python3` (lo necesita
   `eval/score.py`) ni `gh` (lo necesitan `propose` y los comandos de verificación). Este plan
   los añade. `argocd` lo añade `U-deploy`.

## Pasos de generación

Cada paso termina con el comando que demuestra que está hecho (AUTONOMIA-03). Los comandos se
ejecutan en la raíz del repositorio.

- [ ] **Paso 1 — `.gitignore`.**
  Crear `.gitignore` en la raíz: `target/`, `.idea/`, `*.iml`, `.vscode/`, `.env*`,
  `agent/state/`, `agent/logs/`. No ignorar `cambios/` ni `informes/`.
  *Verificación:* `git check-ignore -q core/target && git check-ignore -q agent/logs/tokens.log && ! git check-ignore -q informes/2026-001.md && echo OK` imprime `OK`.

- [ ] **Paso 2 — `pom.xml` raíz.**
  Crear `pom.xml`: `groupId` `com.synkra`, `artifactId` `synkra`, versión `0.1.0-SNAPSHOT`, packaging `pom`;
  módulos `core`, `plugins`, `agent`; propiedades `maven.compiler.release=21` y codificación UTF-8;
  `dependencyManagement` que importa el BOM de Spring Boot (versión estable más reciente al generar, registrada en una propiedad) y el de JUnit;
  `pluginManagement` de compiler, surefire y enforcer (Java `[21,)` y Maven `[3.9,)`).
  *Verificación:* `mvn -q validate` termina en `0` y `mvn -q help:evaluate -Dexpression=maven.compiler.release -DforceStdout` imprime `21`.

- [ ] **Paso 3 — Variable `LLM_PROVIDER=fake` en la suite.**
  En el `pluginManagement` de surefire, definir `LLM_PROVIDER=fake` como variable de entorno de todas las pruebas.
  *Verificación:* `mvn -q help:effective-pom -Doutput=/tmp/effective-pom.xml && grep -c "LLM_PROVIDER" /tmp/effective-pom.xml` imprime un número mayor o igual que `1`.

- [ ] **Paso 4 — Módulos esqueleto.**
  Crear el `pom.xml` mínimo (solo `parent`, `artifactId`, `packaging`) de `core` (jar), `plugins` (pom, agregador con módulos `co` y `pe`), `plugins/co` (jar), `plugins/pe` (jar) y `agent` (jar). Cada uno lleva un comentario "esqueleto: lo reemplaza U-xxx".
  *Verificación:* `mvn -q package -DskipTests` termina en `0` y `ls core/target/*.jar plugins/co/target/*.jar plugins/pe/target/*.jar agent/target/*.jar` lista cuatro archivos.

- [ ] **Paso 5 — Directorios de trabajo.**
  Crear `eval/`, `deploy/`, `images/`, `scripts/` (ya existe), `cambios/` e `informes/`, cada uno con un `.gitkeep` donde esté vacío. `plugins/co/changes/` y `plugins/pe/changes/` también.
  *Verificación:* `for d in eval deploy images cambios informes plugins/co/changes plugins/pe/changes; do test -d "$d" || echo FALTA $d; done` no imprime nada.

- [ ] **Paso 6 — Objetivos `build` y `test` del `Makefile`.**
  Añadir `build` (`mvn -q package -DskipTests`) y `test` (`LLM_PROVIDER=fake mvn -q test`) al `Makefile`, con su comentario `##` para `make help`, y añadirlos a `.PHONY`. No tocar los objetivos existentes.
  *Verificación:* `make help | grep -E "^ +(build|test) "` imprime dos líneas y `make build && make test` termina en `0`.

- [ ] **Paso 7 — Herramientas de entorno (`python3` y `gh`).**
  Añadir a `scripts/doctor.sh` las comprobaciones de `python3` y `gh`, y a `scripts/setup-linux.sh` su instalación (`python3`, `gh`). Mantener finales de línea LF.
  *Verificación:* `./scripts/doctor.sh | grep -E "python3|gh"` imprime dos líneas con `[ok]`.

- [ ] **Paso 8 — Esqueleto del `README.md`.**
  Actualizar la sección "Estructura del Repositorio" con los directorios nuevos (`core/`, `plugins/`, `agent/`, `eval/`, `deploy/`, `images/`, `cambios/`, `informes/`), añadir cómo compilar y probar (`make build`, `make test`) y dejar el marcador `<!-- SECCION: perimetro (U-agent) -->` donde `U-agent` escribirá la declaración del trade-off. No escribir esa declaración aquí.
  *Verificación:* `grep -c "SECCION: perimetro" README.md` imprime `1` y `grep -E "make build|make test" README.md | wc -l` imprime al menos `2`.

- [ ] **Paso 9 — Resumen de la unidad.**
  Escribir `aidlc-docs/construction/U-scaffold/code/summary.md` (solo Markdown) con la lista de archivos creados y modificados.
  *Verificación:* `test -s aidlc-docs/construction/U-scaffold/code/summary.md && echo OK` imprime `OK`.

- [ ] **Paso 10 — Comprobación final de la unidad.**
  Ejecutar la comprobación integrada y revisar que solo cambiaron las rutas previstas.
  *Verificación:* `make doctor && make build && make test && git status --porcelain | grep -vE "^\?\? (\.gitignore|pom\.xml|core/|plugins/|agent/|eval/|deploy/|images/|cambios/|informes/|aidlc-docs/)|^ M (Makefile|README.md|scripts/)"` termina en `0` y la última parte no imprime nada.

## Cobertura de historias

U-scaffold no tiene historias propias. Sus pasos habilitan las verificaciones de las demás:

| Paso | Habilita |
|---|---|
| 2, 4 | US-2.10 (build independiente de plugins) |
| 3 | US-4.4 (pruebas sin llamar a la API) |
| 6 | Todas las verificaciones con `make build` y `make test` |
| 7 | US-1.7 y US-4.5 (`gh`, `python3`) |
| 8 | US-4.6 (marcador del README) |

## Cumplimiento de reglas

| Regla | Estado |
|---|---|
| AUTONOMIA-01 | Cumple. Ningún paso hace `merge`, `push` a `main`, `kubectl apply` ni `helm install` |
| AUTONOMIA-02 | Cumple. El paso 3 fija `LLM_PROVIDER=fake` en toda la suite; ningún paso llama al modelo |
| AUTONOMIA-03 | Cumple. Los diez pasos nombran su comando de verificación |

## Resumen del alcance

10 pasos; 1 `pom.xml` raíz, 5 `pom.xml` esqueleto, 1 `.gitignore`, 2 archivos de entorno
modificados, 1 `Makefile` y 1 `README.md` modificados, y 8 directorios con `.gitkeep`.
Sin código Java, sin manifiestos de Kubernetes.
