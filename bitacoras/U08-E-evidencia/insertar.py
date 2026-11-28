"""U08-E: genera GUIA.md a partir de GUIA.plantilla.md pegando, literales, las salidas reales.

Marcas de la plantilla (cada una en su propia linea, salvo TIEMPO):
  @@SALIDA@@            salida del bloque ejecutable anterior, tal como la dejo guia.sh en guia.txt
  @@TIEMPO CUn@@        segundos de la linea "tiempo CUn Ns" de e2e.txt (dentro de una frase)
  @@LINEA <texto>@@     la primera linea de e2e.txt que empieza por <texto>, en un bloque de codigo
  @@LINEAS <texto>@@    todas las lineas de e2e.txt que empiezan por <texto>
  @@BORRAR@@            la salida de make cluster-delete de la corrida (borrar.txt)
Uso (desde la raiz): python3 -I bitacoras/U08-E-evidencia/insertar.py
"""
import re

EV = "aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt"
D = "bitacoras/U08-E-evidencia/"
guia = open(D + "guia.txt", encoding="utf-8").read().split("\n")

salidas = {}  # numero de bloque -> lineas de su salida
i = 0
while i < len(guia):
    m = re.match(r"^### bloque (\d+)$", guia[i])
    if m:
        n = int(m.group(1))
        j = i
        while guia[j] != "--- salida":
            j += 1
        k = j + 1
        cuerpo = []
        while not guia[k].startswith("--- fin bloque %d " % n):
            cuerpo.append(guia[k])
            k += 1
        salidas[n] = cuerpo
        i = k
    i += 1

e2e = open(EV, encoding="utf-8").read().split("\n")


def bloque(lineas):
    return "```text\n" + "\n".join(lineas) + "\n```"


def tiempo(m):
    for linea in e2e:
        if linea.startswith("tiempo %s " % m.group(1)):
            return linea.split()[2]
    raise SystemExit("no hay tiempo de " + m.group(1))


tpl = open(D + "GUIA.plantilla.md", encoding="utf-8").read().split("\n")
sal = []
n = 0
for t in tpl:
    s = t.strip()
    if s == "<!-- ejecutable -->":
        n += 1
    if s == "@@SALIDA@@":
        sal.append(bloque(salidas[n]))
    elif s == "@@BORRAR@@":
        b = open(D + "borrar.txt", encoding="utf-8").read().split("\n")
        b = b[b.index("---") + 1:]
        while b and b[-1] == "":
            b.pop()
        sal.append(bloque(b))
    elif s.startswith("@@LINEA "):
        pref = s[len("@@LINEA "):-2]
        sal.append(bloque([x for x in e2e if x.startswith(pref)][:1]))
    elif s.startswith("@@LINEAS "):
        pref = s[len("@@LINEAS "):-2]
        sal.append(bloque([x for x in e2e if x.startswith(pref)]))
    else:
        sal.append(re.sub(r"@@TIEMPO (CU\d)@@", tiempo, t))
open("GUIA.md", "w", encoding="utf-8").write("\n".join(sal))
print("bloques ejecutables:", n)
