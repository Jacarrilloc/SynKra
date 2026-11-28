# Ronda 1 — U08-E

VEREDICTO: VERDE

Hash de la tarea verificado: `git hash-object` da e4db4256…, igual al pedido. Corrí CA-3 completo, que ya recrea el clúster, y no repetí CA-1 (la salida de CA-1 la verifiqué leyendo la evidencia). Rama m6r/U08-E, punta cee3b96.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Corrida fresca en verde | No repetí `corrida.sh`. Leí la evidencia confirmada: `grep -c '^salida=0$'` da 1/1/1 en install, limite y e2e; `OK e2e: CU1 a CU5, 10 respuestas 4xx…` presente; cabeceras con fecha, sha c1ab806, comando, arquitectura x86_64 y JAVA_HOME. | pasa (evidencia leída, no re-ejecutada) |
| 2 | Secciones y sin personas ni login | Los `grep -c "^## …"` del criterio dan 1 en las 8 de `summary.md` y en las 8 de `GUIA.md`, en orden. `grep -cE 'David\|Julian\|casdav\|/home/'` da 0 en `GUIA.md` y 0 en `summary.md`. | pasa |
| 3 | La guía se ejecuta | `JAVA_HOME=… bash bitacoras/U08-E-evidencia/guia.sh; echo guia=$?` (borra y recrea el clúster, 5 min 16 s). Resultado `guia=0`, 27 bloques, `grep -c '^FALLA'` = 0, `bloques=27 fallados=0`. `GUIA.md` tiene 27 marcas `<!-- ejecutable -->`. | pasa |
| 4 | Cifras y fragmentos con fuente | `bash bitacoras/U08-E-evidencia/cifras.sh` sale con 0. Filas de tabla: 23. Citas: 41. Líneas de fragmentos comprobadas: 203. `ok=267 falta=0`. Líneas `ok` por revisión: U08-A 3, U08-B 6, U08-C 7, U08-D 5. | pasa |
| 5 | No se tocó código | `git diff --name-only 4881428...HEAD \| grep -vE '^(GUIA\.md\|…)'; echo fuera=$?` da `fuera=1`. `git diff --stat 4881428 c1ab806 -- Makefile scripts deploy backend core front postman` sale vacío. | pasa |

Comprobaciones adicionales:
- **Postman:** los 26 nombres de petición y de carpeta citados en `GUIA.md` existen en `postman/*.json` (1 aparición cada uno).
- **Makefile:** existen `setup`, `doctor`, `install`, `verificar-limite` y `cluster-delete`.
- **SYNKRA_PAISES:** el valor por defecto `co,pe` está en `application.properties`.
- **Dockerfiles y kind:** Temurin/Alpine y `kindest/node` aparecen en los Dockerfiles y en `deploy/kind`.
- **Etiqueta del registro:** la etiqueta `io.synkra.registro` de `make cluster-delete` está en el `Makefile`.
- **Tiempos:** el CU2 de 22 s, el CU5 de 188 s, el total del e2e de 229 s ("unos 4 minutos") y los 185 s de espera ("unos 3 minutos") coinciden con `e2e.txt`.
- **Plan:** el vencimiento a 30 minutos (`creadoEn` 09:39 y `venceEn` 10:09) coincide con la guía.
- **Esperas:** no hay `sleep` fijo en los criterios. La función `esperar` de la guía sondea con tope de 300 s y condición.

Leí `GUIA.md` entera como una persona nueva. Cada paso tiene su comando y lo que debe verse. Las secciones salen en el orden de la tarea, y no encontré datos inventados ni pasos que contradigan la corrida. El front pendiente de U09-D y `/etc/hosts` están tratados como pide el alcance: se indican, no se escriben.

Sobre las tres decisiones del codificador:
- **`guia.sh` con `cluster-delete` e `install`:** correcta. Hace reales los 201 y los ids, y mi corrida lo confirma.
- **Fragmentos congelados en `guia-publicada.txt`:** aceptable. Los ids, tiempos y hashes de pod cambian en cada corrida. Mi `guia.txt` fresco difiere del publicado solo en eso (puerto, orden de pasos de Docker, ids) y la guía lo avisa en su apertura. Las líneas de los 27 bloques son idénticas en forma.
- **`$HOME` en lugar del login:** correcta. Ningún `/home/` queda en `GUIA.md`, `summary.md` ni `evidencia/`.

## Hallazgos
Ningún ROJO ni NARANJA.

### F-01 · AMARILLO · summary.md, «Cifras y su fuente» · dos filas con verificación débil
- «Pruebas nuevas por C-138 = 3» solo busca un nombre de prueba en `bitacoras/U08-D.md`, no el 3.
- «C-130 / C-144 = aceptada» busca solo la cadena `| C-130` en `CANDIDATAS.md`, que no prueba el estado.
- `cifras.sh` en el paso 3 acepta una línea de fragmento si aparece en cualquiera de las fuentes. Una línea corta como `}` pasa siempre.

### F-02 · AMARILLO · GUIA.md:64-68 y 693-697 · línea suelta `kind-registry`
La salida de `make cluster-delete` muestra `kind-registry` sin explicación. La bitácora lo anota como cosmético, pero la guía no lo dice. Una persona nueva puede dudar si es un error. Basta una frase.

### F-03 · AMARILLO · GUIA.md ~369, 504, 548, 578 · archivos fijos en `/tmp`
Los bloques escriben `/tmp/plan1.json`, `/tmp/plan2.json`, etc. Puede chocar con un archivo ajeno o con una corrida concurrente. `mktemp` sería más seguro.

## Tareas candidatas
Las propuestas de candidata que el codificador listó en `bitacoras/U08-E.md` (guía del front tras U09-D, imagen de core 3.x de ejemplo, `GET` de un plan pendiente, espera de C-143, línea suelta de `cluster-delete`) son pertinentes. No añado otras.

## Estado de entorno
- **Worktree:** `guia.sh` regeneró `bitacoras/U08-E-evidencia/guia.txt`, que está rastreado. Lo restauré con `git checkout -- bitacoras/U08-E-evidencia/guia.txt`. Ahora `git status --short` sale vacío.
- **Clúster:** `kind-synkra` está arriba con SynKra instalado. `synkra-backend` va 1/1 Running, `synkra-core` en `synkra-co` va 1/1 Running, y `/actuator/health` responde UP. Usé solo el kubeconfig del worktree. No toqué `~/.kube/config`, `gitops2` ni `m08-gitops1`.

## Rutas de transcripciones largas
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-E-r1/ca3.log`
- `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-E-r1/ca4.txt`

VEREDICTO: VERDE
AMARILLO|summary.md «Cifras y su fuente»|dos filas y el paso 3 de cifras.sh con verificación débil
AMARILLO|GUIA.md:64-68,693-697|línea suelta `kind-registry` sin explicar
AMARILLO|GUIA.md bloques con /tmp/plan*.json|archivos fijos en /tmp
INFORME: revisiones/U08-E/ronda-1.md
