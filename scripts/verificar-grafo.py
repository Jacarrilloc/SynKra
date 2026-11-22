#!/usr/bin/env python3
# Verifica que el grafo de dependencias de un documento de AI-DLC no tiene ciclos.
# Lee la tabla bajo "## Matriz": primera columna = nodo (C01, U01...), segunda = de qué depende.
# Uso: python3 scripts/verificar-grafo.py <archivo.md>
import re,sys
t=open(sys.argv[1]).read()
sec=t.split("## Matriz")[1].split("**Recursos")[0]
deps={}
for line in sec.splitlines():
    m=re.match(r"\| ([CU]\d\d) [^|]*\| ([^|]*)\|",line)
    if m: deps[m.group(1)]=re.findall(r"[CU]\d\d",m.group(2))
n=sum(len(v) for v in deps.values())
print(f"{len(deps)} nodos, {n} dependencias")
indeg={k:0 for k in deps}
for k,v in deps.items():
    for d in v: indeg[k]+=1
order=[];ready=[k for k in deps if indeg[k]==0]
rev={k:[] for k in deps}
for k,v in deps.items():
    for d in v: rev[d].append(k)
while ready:
    ready.sort(); x=ready.pop(0); order.append(x)
    for y in rev[x]:
        indeg[y]-=1
        if indeg[y]==0: ready.append(y)
print("Orden topologico:"," ".join(order))
print("Sin ciclos: OK" if len(order)==len(deps) else "CICLO")
