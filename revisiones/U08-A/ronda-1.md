# Ronda 1 — U08-A

VEREDICTO: NO-VERDE

Corrí yo los diez criterios en x86_64 y todos pasan. El código no depende de la arquitectura y la guardia rechaza cualquier otro contexto sin aplicar nada. Lo único que bloquea es un NARANJA de evidencia: faltan dos de los pegados que la tarea exige en la bitácora.

Comprobaciones previas:
- **Hash de la tarea:** coincide, `eedd72470cd67ad30c75c54b4f0349900e80e0a3`.
- **Commits:** HEAD `4799a82`; último commit de código `3a738aa`; base `8306ee7`.
- **Máquina:** x86_64, kind v0.33.0, kubectl v1.36.4. Las dos imágenes fijadas por digest estaban en caché como `amd64` y no se descargó nada. La imagen de nodo es v1.36.1, no v1.37.x, igual que anotó el codificador; containerd es 2.3.1.
- **Clúster:** `kind-synkra` no existía. Lo creé con `make cluster` usando `$PWD/.kube/kind-synkra.yaml`. Nunca usé `~/.kube/config` ni listé otros clústeres; los únicos `docker ps` llevaron filtro de nombre.

## Criterios de aceptación, verificados por mí
| # | Comando que corrí | Resultado |
|---|---|---|
| 1 | los comandos de CA-1, tal cual | pasa. `salida=0` dos veces; `kind-synkra`; permisos 600 y 700; `.gitignore:10:/.kube/`; `[('synkra-control-plane','True','true','amd64'),('synkra-worker','True','-','amd64')]`; los puertos 80, 443 y la API solo en 127.0.0.1; `no-recreado`; `0`; `variable=1` |
| 2 | los comandos de CA-2 (y `ca2.sh`) | pasa. `Id` igual antes y después; `true registry@sha256:1be5527… always bridge kind`; `5000/tcp -> 127.0.0.1:5001`; `{} v2=0`; hosts.toml en los dos nodos |
| 3 | los comandos de CA-3 (y `ca3.sh`) | pasa. `push=0`, `tags:["1"]`, `pull=0` en los dos nodos, las `rmi` limpian. Dentro del nodo, `localhost:5001` da curl 7 y `kind-registry:5000` da 200 |
| 4 | `bash bitacoras/U08-A-evidencia/ca4.sh` (idéntico al de la tarea) | pasa. Los seis objetivos salen con 2 y `1`; mismo-nombre `1`; global `1`; falta `1`; `falso-intacto`; `nada-aplicado`; `real salida=0` |
| 5 | los comandos de CA-5 | pasa. `3`, `0`, `OK base: 8 objetos`, `salida=0`, la lista exacta de objetos, `prohibidos=1`. Entrada vacía o YAML inválido: `FALLA entrada` y 1 |
| 6 | `bash bitacoras/U08-A-evidencia/ca6.sh` (idéntico al de la tarea) | pasa. 8 `yes` y 10 `no`; con el token real `propio=0`, `ajeno=1` Forbidden, `lista-pais=0`, `lista-synkra=1`, `system:serviceaccount:synkra:deployer`; `[]` ClusterRoleBindings |
| 7 | `bash ca7.sh` y después `ca6.sh` | pasa. `borrar=0`, `existe=1`, `registro=1`, `install=0` en 32 s, nodos Ready, namespaces Active, `v2=0`, `kubeconfig-global-intacto`. Aquí los dos clústeres ajenos están parados: `false` antes y `false` después, así que no se tocaron (la tarea esperaba `true` porque en la otra máquina corrían). CA-6 vuelve a pasar después |
| 8 | `bash ca8-todas.sh` | pasa. Las ocho mutaciones caen donde deben, y `nada-aplicado` sale en todas. El rojo inicial de `0fa392b` está en `rojo.txt` |
| 9 | `bash ca9.sh` (idéntico al de la tarea), después `ca2.sh` y `ca3.sh` | pasa. `sintaxis=0`; el `trap` en la línea 18 y el `docker run` en la 40; `error: docker network connect red-que-no-existe kind-registry falló con código 1`; `salida=1`; `quedo=1`; `restaurado=0`; CA-2 y CA-3 vuelven a pasar; el clúster no se recreó |
| 10 | `bash ca10.sh` | pasa. `fuera=1`, `0`, `0`, árbol limpio. El sha de `ca10.txt` (`4c21603`) es el padre de `4799a82` |

Pruebas extra que hice (todas pasan):
- Con `otro.yaml` y sin clúster, `make install` y `make cluster` salen con 2 antes de crear nada: no queda ningún contenedor `synkra-*` ni registro, y el falso queda intacto.
- Un symlink al `~/.kube/config` de un HOME falso se rechaza. Una ruta relativa a ese mismo config también.
- Un `kind-registry` ajeno sin etiqueta da `kind-registry existe y no es de SynKra: no se toca` y `make registro` sale con 2 sin tocarlo.
- Con `KUBECONFIG_SYNKRA="a:b"` no hay bypass: se trata como un solo archivo y el global falso queda intacto.
- `grep` de `arm64|aarch64|amd64|x86|platform` en el código no encuentra nada.

## Hallazgos

### F-01 · NARANJA · bitacoras/U08-A.md y U08-A-evidencia/ (CA-4, CA-9, CA-6) · faltan pegados que la tarea exige
Lo barrí en toda la evidencia; son tres casos:
- **CA-4:** «La bitácora pega además `make -n install` con el falso». `grep -rn 'make -n install' bitacoras/` sale con rc=1: no está en ninguna parte. Lo corrí yo y la primera receta es `scripts/guardia-contexto.sh --antes-de-crear`, así que el comportamiento está bien, pero el pegado falta.
- **CA-9:** pide que después de CA-9 «CA-2 y CA-3 vuelven a pasar (se pegan)». Los `ca2.txt` y `ca3.txt` finales son de antes de CA-9 (05:12:27 y 05:12:28, frente a 05:12:33 de `ca9.txt`). La bitácora lo admite («previos al ca9»). Los únicos `ca2-tras-ca9.txt` y `ca3-tras-ca9.txt` eran de `85d4923`, anteriores al cambio de guardia de `3a738aa`, y `4c21603` los borró. Aun así la bitácora declara «CA-9 cumple». Lo corrí yo y pasa.
- **CA-6 (menor):** se pide pegar `T=<redactado>` y no aparece. El token no se filtra: no hay ningún `eyJ` en la evidencia.

### F-02 · AMARILLO · scripts/guardia-contexto.sh:41-48 · paso «2b» que la decisión 6 no prevé
Con `--antes-de-crear`, un kubeconfig sin contextos sale con 0 y el mensaje `kubeconfig sin contextos todavía…`, que no figura en la lista de la decisión 6. Lo comprobé: un archivo con `clusters` y `users` ajenos pero sin contextos también pasa, y `make cluster` exportaría encima de él.
- Resuelve un hueco real de la especificación: tras `kind delete cluster`, el kubeconfig queda vacío y CA-7 no podría pasar. Está anotado en la bitácora.
- Lo que acota la regla está cubierto por `ca4.sh`: `install` con `otro` pasa por `--antes-de-crear` y se rechaza.
- Alternativa más estrecha: que `cluster-delete` borre `$(KUBECONFIG_SYNKRA)`. Que lo ratifique el orquestador.

### F-03 · AMARILLO · decisión 7 · el `containerdConfigPatches` condicional no se aplicó
El nodo tiene `config_path = ''` (`containerd config dump`, líneas 31 y 262), que es justo la condición literal de la decisión 7 para añadir el parche. El codificador no lo añadió porque hosts.toml ya funciona sin él. CA-3 lo demuestra en las dos arquitecturas, también en la mía. Es una desviación razonable, pero la decidió el codificador; que la ratifique el orquestador.

### F-04 · AMARILLO · Makefile y scripts/registro-local.sh:58-64 · literales de espera
La espera del registro son 30 vueltas de `sleep 1` con `curl --max-time 2`. Está acotada y tiene condición, así que vale, pero el 30 es un literal sin nombre. El resto de literales (`5001`, `180s`, los digests) los fija la propia tarea.

### Nota sobre la especificación, no es un defecto del codificador
Los valores esperados de CA-1 (`arm64`) y de CA-7 (`true` para los clústeres ajenos) dependen de la máquina. En x86_64 salen `amd64` y `false/false`, y la propiedad se cumple igual. Convendría escribirlos como «arquitectura del host» y «igual antes y después».

## Tareas candidatas (a partir de C-135)
- **C-135:** cada `kubectl` de las recetas escribe la caché de descubrimiento en el `$HOME/.kube/cache` real. No toca `~/.kube/config`, pero sale del proyecto. Lo vi con un HOME falso, donde apareció `casa/.kube/cache`. Se arreglaría con `--cache-dir` o `KUBECACHEDIR` dentro de `.kube/`.

## Estado al terminar
- `kind-synkra` está arriba: dos nodos Ready, v1.36.1, API en `https://127.0.0.1:44323`, `deploy/base` aplicado. Es un clúster nuevo, porque CA-7 lo borró y `make install` lo recreó.
- `kind-registry` está arriba en `127.0.0.1:5001`, recreado por CA-9 y por mi prueba del registro ajeno; quedó con la etiqueta de SynKra.
- `.kube/` tiene 700 y el kubeconfig 600.
- Quedan cero imágenes `localhost:5001/*`.
- `git status --short` sale vacío: el worktree está limpio.
- No toqué los otros clústeres: solo hice el `docker inspect` por nombre que exige CA-7.

## Rutas de transcripciones
Todo en /tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U08-A-r1/:
- ca1.txt
- ca2-ca3.txt
- ca4.txt
- ca5-pre.txt
- ca6.txt
- ca7.txt
- ca8.txt
- ca9.txt
- extra-registro.txt
- extra-lista.txt

```
VEREDICTO: NO-VERDE
NARANJA|bitacoras/U08-A.md (CA-4, CA-9, CA-6)|faltan pegados exigidos: make -n install, CA-2/CA-3 tras CA-9, T=<redactado>
INFORME: revisiones/U08-A/ronda-1.md
```
