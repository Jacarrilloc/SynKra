"""U08-E: genera summary.md a partir de summary.plantilla.md pegando, literales, lineas de los archivos de evidencia.

Marcas (cada una en su linea):
  @@GREP <ruta> <regex>@@   las lineas de <ruta> que casan con la expresion regular, con su numero de linea (como grep -nE)
  @@FIN <ruta> <n>@@        las ultimas <n> lineas de <ruta> (como tail -n)
Uso (desde la raiz): python3 -I bitacoras/U08-E-evidencia/armar-resumen.py
"""
import re

D = "bitacoras/U08-E-evidencia/"
SALIDA = "aidlc-docs/construction/U08-instalacion-limite/code/summary.md"


def leer(ruta):
    return open(ruta, encoding="utf-8").read().rstrip("\n").split("\n")


def bloque(lineas):
    return "```text\n" + "\n".join(lineas) + "\n```"


out = []
for t in open(D + "summary.plantilla.md", encoding="utf-8").read().split("\n"):
    s = t.strip()
    m = re.fullmatch(r"@@GREP (\S+) (.+)@@", s)
    f = re.fullmatch(r"@@FIN (\S+) (\d+)@@", s)
    if m:
        ls = leer(m.group(1))
        pat = re.compile(m.group(2))
        out.append(bloque(["%d:%s" % (i + 1, l) for i, l in enumerate(ls) if pat.search(l)]))
    elif f:
        out.append(bloque(leer(f.group(1))[-int(f.group(2)):]))
    else:
        out.append(t)
open(SALIDA, "w", encoding="utf-8").write("\n".join(out))
print("escrito", SALIDA)
