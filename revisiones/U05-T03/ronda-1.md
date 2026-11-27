# Ronda 1 — U05-T03

VEREDICTO: NO-VERDE

Base `36f0cd3`, rama `m6r/U05-T03` en `9859479` (código en `94f5cc4`). El hash de la tarea coincide: `1d7a200879ff2bdd2e4e19b893279beafd4831c5`. Corrí todo con `HOME`, `KUBECONFIG` y `-Duser.home` apuntando a archivos falsos de mi scratchpad, y `GRADLE_USER_HOME=~/.gradle` solo para reutilizar los toolchains. No usé `kubectl` ni leí `~/.kube/config`. Al terminar, `git status --short` sale vacío: el worktree está limpio.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | YAML esperado, ≥6 pruebas, sin peticiones | `./gradlew :backend:test --rerun --tests '*ConstruirObjetosPaisTest'` + grep del XML | pasa: `BUILD SUCCESSFUL in 10s`, `tests="7" skipped="0" failures="0" errors="0"`. Las pruebas tardan entre 0,056 y 1,18 s cada una, así que se ejecutaron y no se saltaron |
| 2 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` + bucle de grep | pasa: `BUILD SUCCESSFUL in 23s`, 25 XML, ninguno con `skipped`, `failures` o `errors` distinto de 0 |
| 3 | Diff acotado | `git diff --stat 36f0cd3...m6r/U05-T03` | pasa: 8 archivos y 591 inserciones, todos en `kubernetes/**` (main, test y `src/test/resources/kubernetes/`) más la bitácora |

## Comparación con U08 (`infrastructure-design.md`), punto por punto
| Punto de U08 | En el diff | Estado |
|---|---|---|
| Recursos del core: 256-512 Mi y 250-500m | requests y limits de memoria y cpu | cumple |
| Recursos del init: 32-64 Mi y 50-100m | igual | cumple |
| `emptyDir` en `/plugins` y ConfigMap en `/config` | ambos volúmenes, montados en los dos contenedores | cumple |
| SA `default` con `automountServiceAccountToken: false` | en el spec del pod | cumple |
| Imagen del core por digest, desde el registro local | `PlanPais` exige `@sha256:<64hex>` y el Deployment usa exactamente la del plan | cumple |
| Imágenes desde `localhost:5001` (diagrama de despliegue, plan U08 l.44) | init: `synkra-init-descarga`, sin registro ni digest | **no cumple: F-01** |
| Readiness del core | `httpGet /actuator/health/readiness` en el puerto `http` | cumple |
| Sin root ni escalada | `runAsNonRoot: true` y `allowPrivilegeEscalation: false` en ambos contenedores | cumple, con el riesgo F-04 |
| URL del init: `http://synkra-backend.synkra.svc:8080` | `SYNKRA_URL` con ese valor exacto | cumple |

## Las decisiones que el codificador tomó por su cuenta
- **Comando `/usr/local/bin/descargar.sh`.** Es razonable, porque la tarea pide nombrar el comando. Pero crea un contrato con T07 y T08 que hoy solo está escrito en esta bitácora (F-05).
- **Puerto `http` 8080.** Correcto: coincide con `core/src/main/resources/application.properties` (`server.port=8080`) y con los criterios de U02.
- **ConfigMap montado también en el init.** Correcto y necesario: el script de T07 lee `/config/artefactos.txt`.
- **`PlanPais` rechaza los separadores de ruta.** Es buena defensa y es compatible con el patrón de nombres de U01 y con semver. Además sostiene el formato del ConfigMap, que se arma concatenando cadenas. Pero ninguna prueba lo cubre (F-02).
- **`runAsNonRoot` sin `runAsUser`.** Es un riesgo real para T08 y para la imagen del core (F-04).

## El YAML esperado está escrito a mano, no generado
En una copia volqué `Serialization.asYaml` de los tres objetos a `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/log/generado.yaml`. Lo que genera Fabric8 no se parece al archivo del diff:
- Fabric8 pone comillas en todos los escalares, ordena las claves alfabéticamente (`containers` antes que `initContainers`, `labels` antes que `name`), escribe las listas sin sangría y el `command` en estilo bloque.
- El archivo esperado no lleva comillas, sigue el orden de los manifiestos (`kind`/`metadata`/`spec`), escribe `command: ["/usr/local/bin/descargar.sh"]` en estilo flujo y abre con un comentario.

`diff` entre los dos da 182 líneas. El rojo de la bitácora (18 errores de compilación) es coherente con que la prueba se escribió antes que el código. Git no lo puede probar porque todo está en un solo commit.

## Mutaciones, en una copia (`git archive` al scratchpad)
| Mutación | Resultado |
|---|---|
| Core por tag (`@sha256:...` → `:latest` en el constructor) | atrapada (2 fallos: YAML e imagen exacta) |
| `PlanPais` acepta una imagen sin digest | atrapada (`planRechazaImagenSinDigest`) |
| Sin `automountServiceAccountToken(false)` | atrapada (YAML) |
| Sin límite de cpu | atrapada (YAML) |
| Límite de memoria del init en 32Mi | atrapada (YAML) |
| Sin `sorted` en el ConfigMap | atrapada |
| Init sin el montaje de `/config` | atrapada |
| Sin readiness | atrapada |
| `runAsNonRoot(false)` | atrapada |
| URL del catálogo cambiada | atrapada |
| Claves del YAML esperado reordenadas | sigue pasando, como debe: la comparación no depende del orden de claves |
| `construir` sin pasar por `namespaceDe` | **sobrevive** (F-03) |
| Patrón `SEGURO` de `PlanPais` cambiado por `.+` | **sobrevive** (F-02) |

Registros en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/log/mut-*.txt`.

## Hallazgos

### F-01 · NARANJA · backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java:34 · La imagen del init no sale del registro local
`IMAGEN_INIT = "synkra-init-descarga"` no lleva registro, tag ni digest. En el nodo se resuelve como `docker.io/library/synkra-init-descarga:latest`, con `imagePullPolicy: Always` implícito por ser `:latest`.

U08 dice otra cosa:
- `make install` sube la imagen como `localhost:5001/synkra-init-descarga` (plan de código U08, l.44).
- El diagrama de despliegue dice que los pods del país sacan su imagen del registro local por digest.
- H-31 limita el registro permitido.

Así como está, todo pod de país quedaría en `ErrImagePull` en `kind`. Tampoco sirve corregirlo con el overlay de Kustomize, porque el backend crea este objeto en tiempo de ejecución. Es además un valor de entorno metido como literal (invariante 8).

La decisión 3 de la tarea nombra la imagen solo como `synkra-init-descarga`, así que el orquestador debe precisar la referencia completa: registro y digest o tag, fija o configurable, con un valor por defecto `localhost:5001/...`. El YAML esperado tiene que cambiar con ella.

### F-02 · NARANJA · backend/src/main/java/io/synkra/kubernetes/api/PlanPais.java:12,28,31 · La validación de nombre y versión de plugin no tiene prueba
Si cambio el patrón `SEGURO` por `.+`, las 7 pruebas pasan (`mut-M10-plugin-con-barra.txt`).

Esa validación es lo único que impide que un nombre o versión con `/`, `..`, salto de línea o `: ` corrompa `artefactos.txt` (una línea por plugin, que el init convierte en ruta bajo `/plugins`) o inyecte claves en `plugins-activos.yaml`, que se arma concatenando cadenas.

Hace falta al menos un caso rechazado para `../x`, `a/b`, un nombre con `\n` y una versión con espacio, y un caso aceptado para un semver con `-rc.1+b`.

### F-03 · AMARILLO · ConstruirObjetosPaisTest.java:120-122 · Que `construir` pase por la guardia no es observable
La prueba de la guardia llama a `AdaptadorFabric8.namespaceDe` directamente, y `construir(null)` comprueba un NPE, no la guardia. Si quito la llamada a la guardia dentro de `construir`, nada falla (`mut-M4-sin-guardia.txt`).

Es inherente al diseño: `PlanPais` ya garantiza `[a-z]{2}`, así que con un plan válido la guardia es inalcanzable. Cumple la letra del CA-1. Basta un comentario que lo diga, o una costura para observar la llamada.

### F-04 · AMARILLO · ConstructorObjetosPais.java:111,124 · `runAsNonRoot` sin `runAsUser`
Si la imagen declara un `USER` no numérico, el kubelet no puede comprobar que no es root y rechaza el contenedor con `CreateContainerConfigError`. Los criterios de U05-T08 y U08 (`docker inspect -f '{{.Config.User}}'` distinto de vacío, `root` y `0`) aceptan un usuario con nombre, así que el pod de T03 podría no arrancar nunca. Ver la tarea candidata.

### F-05 · AMARILLO · ConstructorObjetosPais.java:35 · El comando del init es un contrato que solo vive en la bitácora
T07 y T08 tienen que instalar el script exactamente en `/usr/local/bin/descargar.sh`. Ninguna tarea lo dice. Otra opción es omitir `command` y usar el `ENTRYPOINT` de la imagen.

### F-06 · AMARILLO · api/ObjetosPais.java · El tipo público expone tipos de Fabric8
`ObjetosPais` está en `kubernetes.api` y expone `io.fabric8...Deployment`, `ConfigMap` y `Service`. Hoy el grep de T09 pasa porque el archivo vive bajo `/kubernetes/`. Pero U07 no podrá tocar esos campos sin importar `io.fabric8` fuera de `kubernetes`, y entonces el criterio de T09 fallaría.

## Tareas candidatas (defectos reales fuera de alcance)
- U05-T08 y U08: exigir un `USER` **numérico** distinto de 0 en las imágenes del init, del backend y del core de ejemplo, o bien fijar aquí un `runAsUser` numérico como constante. Hay que decidir en qué capa, porque hoy el criterio de T08 deja pasar una imagen que el pod de T03 rechazaría.
- U05-T07 y T08: fijar por escrito la ruta del script (`/usr/local/bin/descargar.sh`) que el Deployment ya invoca.
- U05-T09 y U07: decidir si `ObjetosPais` debe exponer tipos de Fabric8 en `kubernetes.api`.

## Rutas de transcripciones largas
Todas en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/5b13ba94-befc-454f-88e7-3182922701e5/scratchpad/log/`:
- `ca1.txt`
- `ca2.txt`
- `ca2-xml.txt`
- `generado.yaml`
- `mut-*.txt`

```
VEREDICTO: NO-VERDE
NARANJA|backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java:34|La imagen del init no sale del registro local (ErrImagePull en kind; contradice U08 y H-31)
NARANJA|backend/src/main/java/io/synkra/kubernetes/api/PlanPais.java:12|La validación de nombre y versión de plugin no tiene prueba (la mutación sobrevive)
INFORME: revisiones/U05-T03/ronda-1.md
```
