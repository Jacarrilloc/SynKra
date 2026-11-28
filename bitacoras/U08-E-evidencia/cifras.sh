#!/usr/bin/env bash
# U08-E, CA-4: cada cifra del resumen y cada fragmento de respuesta de la guia salen de un archivo real.
#  1. «Cifras y su fuente» de summary.md: cada fila `| cifra | valor | `fuente` | `texto` |` se busca con grep -F en su fuente.
#  2. Las citas «texto» (`fuente`) de «Desviaciones del diseño» y «Candidatas abiertas» se buscan con grep -F en su fuente.
#  3. Cada linea de cada bloque ```text de GUIA.md tiene que estar, entera, en guia.txt, en la evidencia o en borrar.txt.
# Una fuente que empieza por orq: se lee del arbol del orquestador (solo lectura): tareas/ y CANDIDATAS.md de la rama orq.
# Salida: una linea `ok` o `FALTA` por comprobacion. Sale con 1 si hay alguna FALTA.
# Uso (desde la raiz del worktree): bash bitacoras/U08-E-evidencia/cifras.sh
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
ORQ=/home/casdav/Projects/Master/wt-orq-U04
SUMMARY=aidlc-docs/construction/U08-instalacion-limite/code/summary.md
EV=aidlc-docs/construction/U08-instalacion-limite/code/evidencia

python3 -I - "$SUMMARY" "$ORQ" "$EV" GUIA.md bitacoras/U08-E-evidencia <<'PY'
import os, re, sys

resumen, orq, ev, guia, bit = sys.argv[1:6]
malos = 0
oks = 0


def ruta(fuente):
    if fuente.startswith("orq:"):
        return os.path.join(orq, fuente[4:])
    return fuente


def buscar(fuente, texto, etiqueta):
    global malos, oks
    p = ruta(fuente)
    try:
        contenido = open(p, encoding="utf-8").read()
    except OSError as e:
        print("FALTA %s: no se pudo leer %s (%s)" % (etiqueta, fuente, e))
        malos += 1
        return
    if texto in contenido:
        print("ok %s: %s" % (fuente, texto[:100]))
        oks += 1
    else:
        print("FALTA %s: %s no contiene «%s»" % (etiqueta, fuente, texto[:100]))
        malos += 1


def seccion(texto, nombre):
    m = re.search(r"^## %s\n(.*?)(?=^## |\Z)" % re.escape(nombre), texto, re.S | re.M)
    return m.group(1) if m else ""


s = open(resumen, encoding="utf-8").read()

# 1. la tabla
filas = 0
for linea in seccion(s, "Cifras y su fuente").split("\n"):
    if not linea.startswith("|") or linea.startswith("|---") or linea.startswith("| Cifra"):
        continue
    celdas = [c.strip() for c in linea.strip().strip("|").split(" | ")]
    if len(celdas) != 4:
        print("FALTA fila mal formada: " + linea[:100])
        malos += 1
        continue
    m1 = re.fullmatch(r"`(.+)`", celdas[2])
    m2 = re.fullmatch(r"`(.+)`", celdas[3])
    if not (m1 and m2):
        print("FALTA fila sin fuente o sin texto entre comillas inversas: " + linea[:100])
        malos += 1
        continue
    filas += 1
    buscar(m1.group(1), m2.group(1), "cifra «%s»" % celdas[0])
print("filas de la tabla: %d" % filas)

# 2. las citas
citas = 0
for nombre in ("Desviaciones del diseño", "Candidatas abiertas", "Tareas y rondas"):
    for m in re.finditer(r"«([^»]+)» \(`([^`]+)`\)", seccion(s, nombre)):
        citas += 1
        buscar(m.group(2), m.group(1), "cita")
print("citas: %d" % citas)

# 3. los fragmentos de la guia
fuentes = [os.path.join(bit, "guia.txt"), os.path.join(bit, "borrar.txt")]
fuentes += [os.path.join(ev, f) for f in ("install.txt", "limite.txt", "e2e.txt")]
conocidas = set()
for f in fuentes:
    conocidas.update(open(f, encoding="utf-8").read().split("\n"))
g = open(guia, encoding="utf-8").read()
frag = 0
for m in re.finditer(r"^```text\n(.*?)^```$", g, re.S | re.M):
    for linea in m.group(1).split("\n"):
        if linea == "":
            continue
        frag += 1
        if linea in conocidas:
            oks += 1
        else:
            print("FALTA fragmento de GUIA.md: " + linea[:120])
            malos += 1
print("lineas de fragmentos de la guia comprobadas: %d" % frag)
print("ok=%d falta=%d" % (oks, malos))
sys.exit(1 if malos else 0)
PY
