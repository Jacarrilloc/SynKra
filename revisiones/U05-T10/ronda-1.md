# Ronda 1 — U05-T10

VEREDICTO: NO-VERDE

Los tres criterios pasan con mis propias ejecuciones y la cerca de alcance se respeta. Quedan dos NARANJA: evidencia que no trae el comando ni el sha, y una candidata abierta que falta en el resumen. El worktree quedó limpio (`git status --short` sin líneas) y los temporales se borraron.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí (desde /home/julian/Escritorio/Proyectos/wt-m6r-U05-T10, HOME y KUBECONFIG falsos) | Resultado |
|---|---|---|---|
| 1a | Pruebas del adaptador | `./gradlew :backend:test --rerun --tests '*kubernetes*'` | pasa: BUILD SUCCESSFUL. En los XML hay 10 clases y 127 pruebas, con skipped, failures y errors en 0. Los tiempos por clase van de 0,001 a 3,3 s, coherentes con el servidor simulado. Los conteos por clase coinciden uno a uno con `evidencia/conteo-por-clase.txt`. |
| 1b | Script del init | `init-descarga/test.sh; echo "salida=$?"` | pasa: salida=0 y 27 casos `ok`. La lista es idéntica a `evidencia/init-descarga-test.txt` (diff vacío). |
| 1c | Conteos en el resumen | `grep -c 'BUILD SUCCESSFUL'` y `grep -c 'salida=0'` sobre summary.md | pasa: 1 y 1 |
| 1d | Imagen (decisión 3) | `docker build` y luego `docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' synkra-init-descarga:1.0.0` | pasa: `10001:10001 ["/usr/local/bin/descargar.sh"]`, igual que en el resumen |
| 2 | Trazabilidad | el bucle `for h in H-12 … H-28` | pasa: cada historia aparece 2 veces |
| 3 | Diff acotado | `git rev-parse --short HEAD` y `git diff --stat 03943ec...HEAD` | pasa: f2a5806, 6 archivos, 218 inserciones. Solo hay archivos en `code/**` y la bitácora. |

Contrasté el resumen con los archivos:
- **Rondas:** cuadran con los veredictos de cada ronda-N.md. T01 a T06 los leí en `revisiones/` de `orq/U05-U02-tareas`; T07, T08 y T09 en sus ramas `m6r/`. Todas terminan en VERDE.
- **PR:** los números y estados cuadran con `gh pr list`. #85, #87 y #90 siguen abiertos. #68, #73, #74 y #77 son de la rama del orquestador.
- **Métodos:** los cinco métodos y los dos auxiliares de `AdaptadorKubernetes` coinciden con el código.
- **Paquetes:** los tipos de `api/` y las clases internas también coinciden, igual que `ObjetosPais` con sus tres `yaml*()`.
- **Dockerfile:** fija USER 10001:10001 y el entrypoint en `/usr/local/bin/descargar.sh`.
- **C-68, C-70, C-73 y C-79:** la tarea de U05-T09 (decisiones 3 a 5) y su bitácora sostienen que se recogieron. También que el tercer punto de C-73 y la unificación de las reglas de imagen siguen abiertos.
- **Descripciones de candidatas:** las de C-17 a C-102 citadas cuadran con CANDIDATAS.md.

## Hallazgos

### F-01 · NARANJA · evidencia/gradle-kubernetes.txt, evidencia/imagen-init.txt, bitacoras/U05-T10.md · Evidencia sin comando ni sha, y una cola presentada como «salida completa»
La decisión 2 de la tarea pide pegar cada salida «con la fecha, el sha de la rama y el comando». Barrido de toda la clase:
- **`gradle-kubernetes.txt`:** trae fecha y sha, pero no el comando. Además son solo las últimas ~8 líneas, y el resumen (sección «Pruebas del adaptador») dice «salida completa en `evidencia/gradle-kubernetes.txt`». Esa afirmación no se sostiene con el archivo.
- **`imagen-init.txt`:** no trae sha. El comando aparece truncado (`$ docker inspect -f ...`) y el build es una cola, aunque el resumen lo llama «Salida completa».
- **Bitácora, CA-1:** «1 / 1» sin el comando que produjo cada número.
- **Bitácora, CA-2:** «cada H- aparece 2 veces» sin la salida.
- **Bitácora, CA-3:** el `--stat` (204 inserciones) se tomó antes del commit de la bitácora. Está rotulado, pero no anota el sha del `git rev-parse --short HEAD` que el criterio pide («Se anota el sha»). El diff final tiene 218 inserciones.

Es la misma clase que C-103 señaló en U02-T11. Los valores son reales, porque los reproduje, pero el archivo no sostiene lo que dice el resumen.

### F-02 · NARANJA · summary.md, sección «Candidatas abiertas que nombran U05, U07 o U08» · Falta C-30
C-30 nombra el plan de U07 (`U07-despliegue-code-generation-plan.md:3` nombra a una persona). Su decisión es «se corrige al escribir las tareas de U07» y no está marcada como cerrada. El resumen sí incluye otras candidatas ajenas a U05 que van a U07 (C-35, C-62), así que por su propio criterio C-30 debía aparecer. Lo encontré barriendo CANDIDATAS.md: es la única fila hasta C-102 que nombra U05, U07 o U08, sigue abierta y falta en el resumen. C-71 solo nombra U05 en «Salió de» y está atendida en los despachos. C-72 y C-81 se resolvieron en T03, T06 y T08 y el resumen las cita.

### F-03 · AMARILLO · summary.md, tabla de trazabilidad · Se aparta del plan sin decirlo
La tabla del plan (`U05-pod-pais-code-generation-plan.md`, líneas 59 a 64) asigna H-15 a T03, T07 y T10, y H-13 solo a T02. El resumen añade T08 a H-15 y T09 a H-13. Las dos atribuciones son defendibles, pero el resumen debería avisar que amplía la tabla del plan.

### F-04 · AMARILLO · summary.md, tabla de tareas · «#85 (abierto, apilado)»
`gh pr view 85` da base `main`. #87 y #90 sí están apilados (bases `m6r/U05-T07` y `m6r/U05-T08`), pero #85 no.

### F-05 · AMARILLO · summary.md, párrafo bajo la tabla de tareas · Errata
Errata en «Los PR salen de `gh pr list`;.».

### F-06 · AMARILLO · summary.md, candidatas · C-103 no aparece
C-103 (literales repetidos en las tres imágenes, incluida la del init; va a U08) nació después del despacho. El resumen declara con honestidad «hasta C-102», así que solo conviene añadirla si la ronda 2 toca esa sección.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/gradle.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/init.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/cand.txt

```
VEREDICTO: NO-VERDE
NARANJA|evidencia/gradle-kubernetes.txt, evidencia/imagen-init.txt, bitacoras/U05-T10.md|Evidencia sin comando ni sha y cola presentada como salida completa (decisión 2, CA-3 sin sha)
NARANJA|summary.md, sección Candidatas abiertas|Falta C-30, abierta y para U07
AMARILLO|summary.md, Trazabilidad|Se aparta del plan (T08 en H-15, T09 en H-13) sin decirlo
AMARILLO|summary.md, tabla de tareas|#85 no está apilado (base main)
AMARILLO|summary.md, párrafo de PR|Errata «gh pr list;.»
AMARILLO|summary.md, Candidatas|C-103 posterior al despacho, sin listar
INFORME: revisiones/U05-T10/ronda-1.md
```
