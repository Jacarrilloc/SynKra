# U08-E: resumen de U08 y guía para instalar y usar SynKra

**Unidad:** U08: `Instalación y límite`
**Tareas del plan que agrupa:** U08-T12 (resumen), más la guía de uso que pidieron el equipo y el responsable del proyecto al cerrar U08
**Historias que implementa:** H-26 (resumen con salidas reales); contribuye a H-24
**Depende de:** U08-A a U08-D (fusionadas en `main`: #111, #112, #113, #116) y la colección de Postman (#109). Sale de `4881428` (`main`).
**Revisor:** `sonnet` (tarea de documentación).

---

## Alcance

**Dentro** (una línea, concreta):

> `aidlc-docs/construction/U08-instalacion-limite/code/summary.md` (resumen de la unidad con las salidas reales de `make install`, `make verificar-limite` y `./scripts/e2e-api.sh`) y `GUIA.md` en la raíz del repo (cómo instalar SynKra en `kind`, entrar, usar cada función con `curl` y con la colección de Postman, correr el recorrido de punta a punta y desinstalar), con las salidas reales de una corrida fresca en la máquina de David.

**Fuera**:

- Ningún cambio de código, de `Makefile`, `deploy/`, `scripts/`, `postman/`, `front/` ni del backend. Si la guía descubre un defecto, se anota como propuesta de candidata (sin número) y se describe tal cual en la guía; no se arregla aquí.
- El front Blazor en `kind` (U09-D aún no aplica su manifiesto): la guía lo menciona como «pendiente de U09-D» y explica cómo correrlo en local con `scripts/dotnet-docker.sh` si la tarea U09-A lo documenta; no inventa pasos que no existan.
- `/etc/hosts`: la guía puede **indicar** la línea `127.0.0.1 synkra.local` para quien quiera usar el navegador, pero ningún comando de esta tarea la escribe.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md` (U08-T12) y `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/`
- `tareas/U08-A-cluster-guardia-rbac.md`, `U08-B-backend-ingress-imagenes.md`, `U08-C-install-limite.md`, `U08-D-e2e-api.md`, y `revisiones/U08-*` y `bitacoras/U08-*`
- `Makefile`, `scripts/` (en especial `e2e-api.sh`, `verificar-limite.sh`, `imagenes.sh`, `desplegar.sh`), `deploy/`
- `postman/README.md` y la colección
- La API del backend: rutas (`RutasCores`, `RutasPlugins`, `RutasVerificacion`, `RutasPlanes`, `RutasConfirmacion`, reversión, `RutasRegistro`, `RutasVistaGeneral`), `CodigosDespliegue`/`MotivosDespliegue`, `X-Synkra-Usuario`
- Plantillas de resumen: `tareas/U06-D-resumen.md` y `tareas/U07-G-fronteras-resumen.md` (tabla «Cifras y su fuente», copias literales); resúmenes ya fusionados en `aidlc-docs/construction/U0*/code/summary.md`
- `CANDIDATAS.md` (candidatas de U08: C-95, C-98, C-101, C-103, C-130 a C-132, C-136 a C-140, C-143, C-144)

---

## Decisiones del orquestador

1. **Una corrida fresca, en orden y en esta máquina.** Con el kubeconfig del worktree: `make cluster-delete`, `make install`, `make verificar-limite`, `./scripts/e2e-api.sh`. Las salidas completas van a `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/` (`install.txt`, `limite.txt`, `e2e.txt`), con cabecera de fecha, sha, comando, arquitectura del servidor Docker y `JAVA_HOME`. El resumen y la guía citan de ahí, sin reescribir. Al terminar, el clúster queda **arriba** con SynKra instalado y el kubeconfig del worktree.
2. **Secciones del resumen** (en este orden): Qué se construyó, Cómo se prueba, Salidas reales, Desviaciones del diseño, Trazabilidad, Tareas y rondas, Candidatas abiertas, Cifras y su fuente. Las desviaciones salen de las bitácoras y revisiones de U08-A a U08-D (p. ej. sin `yq` ni `kubeconform`; `--provenance=false --sbom=false`; `make` sale con 2; `aplicar-base` con `-f`; `imagePullPolicy: Always` en el overlay; Ingress NGINX `controller-v1.15.1` retirado; C-138 corregido en U08-D; `kube-root-ca.crt` en `synkra-pe`; imágenes ordenadas en CU4), cada una con su fuente.
3. **Secciones de `GUIA.md`** (en este orden): Requisitos (herramientas y versiones de esta máquina; el `sysctl` de inotify si hay otros clústeres `kind`; puertos 80, 443 y 5001), Instalar (`make install`, qué hace cada paso, cuánto tarda en la corrida fresca), Comprobar el límite (`make verificar-limite` y qué significan las seis respuestas), Entrar (`curl --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt`; la línea de `/etc/hosts` como opción para el navegador), Usar cada función (subir un core, subir plugins, ver el catálogo, verificar una combinación, crear un plan y ver su resumen y cambios, confirmar, ver la vista general, el historial y el detalle, cambiar plugins, una combinación incompatible rechazada, un fallo y la vuelta atrás; cada una con su `curl` real, la respuesta real recortada sin inventar campos, y la petición equivalente de la colección de Postman por su nombre), El recorrido completo (`./scripts/e2e-api.sh` y qué esperar de CU1 a CU5), Errores comunes (los 4xx del formato único y qué significan), Desinstalar (`make cluster-delete`; qué no toca: `~/.kube/config` ni otros clústeres).
4. **Los `curl` de la guía se corren de verdad.** Un guion `bitacoras/U08-E-evidencia/guia.sh` ejecuta cada bloque de `GUIA.md` marcado como ejecutable (por ejemplo, bloques ```bash que siguen a un comentario `<!-- ejecutable -->`) contra el clúster de la corrida fresca y guarda su salida en `guia.txt`. Los fragmentos de respuesta que muestra la guía deben estar, literales, en `guia.txt` o en `e2e.txt`.
5. **Sin nombres de personas ni rutas con el login** en `aidlc-docs` ni en `GUIA.md` (C-30, C-128): `$HOME`, «el responsable del proyecto».
6. **Usuario de los ejemplos:** `X-Synkra-Usuario: demo`; país `co` y, si hace falta un segundo país, `pe`.

---

## Criterios de aceptación

- [ ] **CA-1**: la corrida fresca termina en verde
  ```bash
  bash bitacoras/U08-E-evidencia/corrida.sh   # cluster-delete, install, verificar-limite, e2e; escribe code/evidencia/*.txt
  grep -c '^salida=0$' aidlc-docs/construction/U08-instalacion-limite/code/evidencia/{install,limite,e2e}.txt
  grep -F 'OK e2e' aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt
  ```
  Esperado: `1` en cada archivo; la línea `OK e2e: …`; cabeceras con fecha, sha y comando.
- [ ] **CA-2**: secciones del resumen y de la guía
  ```bash
  for s in 'Qué se construyó' 'Cómo se prueba' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas y rondas' 'Candidatas abiertas' 'Cifras y su fuente'; do grep -c "^## $s" aidlc-docs/construction/U08-instalacion-limite/code/summary.md; done
  for s in 'Requisitos' 'Instalar' 'Comprobar el límite' 'Entrar' 'Usar cada función' 'El recorrido completo' 'Errores comunes' 'Desinstalar'; do grep -c "^## $s" GUIA.md; done
  grep -cE 'David|Julian|casdav|/home/' GUIA.md aidlc-docs/construction/U08-instalacion-limite/code/summary.md
  ```
  Esperado: `1` por cada sección, en orden; `0` y `0` en el último.
- [ ] **CA-3**: la guía se ejecuta
  ```bash
  bash bitacoras/U08-E-evidencia/guia.sh; echo "guia=$?"
  grep -c '^FALLA' bitacoras/U08-E-evidencia/guia.txt
  ```
  Esperado: `guia=0`; `0`. Cada bloque `<!-- ejecutable -->` de `GUIA.md` corrió y su salida está en `guia.txt`.
- [ ] **CA-4**: cifras y fragmentos con fuente
  ```bash
  bash bitacoras/U08-E-evidencia/cifras.sh   # recorre «Cifras y su fuente» y cada fragmento de respuesta de GUIA.md con grep -F contra su archivo
  ```
  Esperado: ninguna línea `FALTA`; al menos un `ok` por cada `revisiones/U08-A` a `U08-D`.
- [ ] **CA-5**: no se tocó código
  ```bash
  git diff --name-only 4881428...HEAD | grep -vE '^(GUIA\.md|aidlc-docs/construction/U08-instalacion-limite/code/|bitacoras/U08-E(\.md|-evidencia/))'; echo "fuera=$?"
  ```
  Esperado: `fuera=1`. Se toma justo antes de su propio commit.

## Definición de lista

Corrida fresca en verde y su evidencia en `code/evidencia/`; `summary.md` y `GUIA.md` con todas sus secciones; los bloques ejecutables de la guía corridos en `guia.txt`; cada cifra y fragmento con fuente; diff acotado; clúster arriba con SynKra instalado.
