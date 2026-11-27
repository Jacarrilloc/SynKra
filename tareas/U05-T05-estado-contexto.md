# U05-T05: `estado` y `contextoFallo`

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-14, H-16 (contribuye)
**Depende de:** U05-T04

---

## Alcance

**Dentro** (una línea, concreta):

> Dos métodos de lectura en `AdaptadorKubernetes`: `estado(String namespace)`, que devuelve réplicas deseadas y listas, reinicios y una fase (`LISTO`, `EN_CURSO` o `FALLIDO` con su razón) del Deployment `synkra-core` y sus pods; y `contextoFallo(String namespace)`, que devuelve las últimas 200 líneas del log del contenedor `core`, el log del init `descarga` y los eventos del pod. Los dos pasan antes por la guardia.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Esperar, sondear cada 5 s o aplicar el plazo de 3 minutos (S5 es de U07). `estado` es una sola lectura.
- Truncar los textos a 64 KB o guardarlos en SQLite (U06).
- `leerPais` (U05-T06), el script y la imagen del init (U05-T07 y T08).
- Escribir, borrar o reiniciar cualquier objeto. Solo `get`, `list` y `pods/log`.
- Cambios fuera de `kubernetes/**`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T05)
- `aidlc-docs/inception/application-design/component-methods.md` (C09 `estado` y `contextoFallo`) y `services.md` (S5)
- `aidlc-docs/inception/user-stories/stories.md` (H-14 y H-16)
- `aidlc-docs/construction/U06-registro/functional-design/domain-entities.md` (`ContextoFallo`: 200 líneas del core)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (verbos del `Role`: Pods, Events y `pods/log` con get, list, watch)
- `backend/src/main/java/io/synkra/kubernetes/` (adaptador, guardia, constructor y `aplicar` de T01 a T04)
- `revisiones/U05-T04/ronda-1.md` (F-01 y F-02: lecciones que valen aquí) y `CANDIDATAS.md` (C-73)

---

## Decisiones del orquestador

1. **Tipos propios.** `EstadoPais` y `ContextoFalloK8s` son records en `kubernetes.api`, sin tipos de Fabric8 (en la línea de C-73). La fase es un enum `FaseDespliegue { LISTO, EN_CURSO, FALLIDO }`; con `FALLIDO` va una razón de texto tal como la da Kubernetes (`CrashLoopBackOff`, `Init:Error`, `Init:CrashLoopBackOff`, `ImagePullBackOff`, `ErrImagePull`).
2. **Reglas de la fase.** `LISTO` si réplicas listas = réplicas deseadas y > 0. `FALLIDO` si algún pod del Deployment tiene un contenedor (o init) en `waiting` con una de esas razones, o un init `terminated` con código distinto de 0 (`Init:Error`). En otro caso, `EN_CURSO`. Si el Deployment no existe, `estado` lanza una excepción propia del módulo que nombra el namespace, sin inventar un estado.
3. **Pods del país.** Se buscan con el selector del Deployment (`app.kubernetes.io/name=synkra-core`), solo en el namespace dado. Los nombres de contenedores (`core`, `descarga`), del Deployment y el número de líneas (200) salen de constantes con nombre, no de literales sueltos.
4. **Log.** `tailingLines(200)` del contenedor `core`; si el contenedor reinició, se toma el log del intento anterior (`previous`) cuando el actual está vacío. El log del init se lee entero (U06 trunca). Si un log no existe todavía (pod en `Pending`), el campo queda vacío y no es un error.
5. **Eventos.** Los `Events` del namespace cuyo `involvedObject` es uno de los pods del país, ordenados por tiempo, como texto `tipo razón: mensaje`.
6. **Sin cliente.** Si `disponible()` es falso, los dos métodos lanzan un error propio que lo dice, sin peticiones (F-02 de U05-T04). Solo se envuelven en excepción del módulo las `KubernetesClientException`.

---

## Criterios de aceptación

- [ ] **CA-1**: estado y contexto contra el servidor simulado (lectura de vuelta)
  ```bash
  ./gradlew :backend:test --rerun --tests '*EstadoYContextoTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*EstadoYContextoTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 8, sin saltos ni fallos. Casos, con objetos sembrados en el servidor simulado (modo CRUD o expectativas, lo que dice la bitácora):
  - pod listo: `LISTO`, réplicas 1/1, reinicios 0;
  - pod en `CrashLoopBackOff` con 4 reinicios: `FALLIDO`, razón `CrashLoopBackOff`, reinicios 4, y `contextoFallo` trae exactamente las últimas 200 líneas de un log de 1000 (se comprueba la primera y la última línea devueltas);
  - init terminado con código 1: `FALLIDO`, razón `Init:Error`, y el log del init está en el contexto;
  - pod `Pending` sin logs: `EN_CURSO` y `contextoFallo` sin error, con logs vacíos;
  - Deployment ausente: excepción propia que nombra el namespace;
  - eventos de otro pod del mismo namespace no aparecen en el contexto;
  - `default`, `kube-system` y `synkra` se rechazan con cero peticiones registradas;
  - sin cliente: error propio, cero peticiones;
  - todas las peticiones registradas son `GET`: ninguna `PATCH`, `POST`, `PUT` ni `DELETE`, y todas van a `/namespaces/synkra-pe/`.

- [ ] **CA-2**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos. Se pega la salida tal cual.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `kubernetes/**`, la bitácora y `revisiones/U05-T05/`. Se anota el sha. La base es la que da el despacho (la punta de la rama de tareas), no `origin/main`.

---

## Plan de pruebas

- `EstadoYContextoTest` con `KubernetesMockServer`. Los pods y eventos se siembran con su `status` (no hay kubelet en el simulado). El log se sirve con una expectativa sobre `/pods/<pod>/log?...tailLines=200`; la prueba comprueba ese parámetro en la petición registrada.

**Rojo primero:** la prueba antes que el código, en una copia o con la prueba sola, sin `git stash`.

---

## Notas

- Ninguna prueba usa un clúster: solo el servidor simulado, con `kubeconfig`, `KUBECONFIG`, `HOME` y `user.home` falsos.
