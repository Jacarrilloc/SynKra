#!/usr/bin/env bash
# U08-E, CA-3: corre cada bloque ```bash de GUIA.md que sigue a un comentario <!-- ejecutable -->, en orden y en un solo shell
# (las variables pasan de un bloque al siguiente, igual que en la terminal de quien sigue la guia).
# Escribe bitacoras/U08-E-evidencia/guia.txt. Antes de correrlo, hace falta JAVA_HOME en Java 21. El primer bloque borra
# kind-synkra y el segundo lo instala de nuevo: la guia corre sobre una instalacion limpia.
# Uso (desde la raiz del worktree): JAVA_HOME=<java 21> bash bitacoras/U08-E-evidencia/guia.sh
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
DIR=bitacoras/U08-E-evidencia
OUT="$DIR/guia.txt"
BLOQUES="$(mktemp -d)"
trap 'rm -rf "$BLOQUES"' EXIT

python3 -I - GUIA.md "$BLOQUES" <<'PY'
import re, sys
texto = open(sys.argv[1], encoding="utf-8").read().split("\n")
n = 0
i = 0
while i < len(texto):
    if texto[i].strip() == "<!-- ejecutable -->":
        j = i + 1
        while j < len(texto) and texto[j].strip() == "":
            j += 1
        if j < len(texto) and texto[j].startswith("```bash"):
            k = j + 1
            cuerpo = []
            while k < len(texto) and not texto[k].startswith("```"):
                cuerpo.append(texto[k])
                k += 1
            n += 1
            open("%s/%03d.sh" % (sys.argv[2], n), "w", encoding="utf-8").write("\n".join(cuerpo) + "\n")
            i = k
    i += 1
print(n)
PY

{
  echo "fecha: $(date -Iseconds)"
  echo "sha: $(git rev-parse --short HEAD)"
  echo "comando: bash $DIR/guia.sh"
  echo "docker-servidor-arquitectura: $(docker info --format '{{.Architecture}}')"
  echo "JAVA_HOME: ${JAVA_HOME:-sin definir}"
  echo "nota: la ruta personal de la maquina aparece como \$HOME"
  echo "---"
} | sed "s|$HOME|\$HOME|g" >"$OUT"

total=0
malos=0
{
  # Un solo shell para todos los bloques: se leen con "." y su codigo de salida se mira uno por uno.
  # shellcheck disable=SC1090
  for f in "$BLOQUES"/*.sh; do
    total=$((total + 1))
    echo "### bloque $total"
    cat "$f" | sed 's/^/$ /'
    echo "--- salida"
    rc=0
    . "$f" || rc=$?
    echo "--- fin bloque $total salida=$rc"
    [ "$rc" -eq 0 ] || { echo "FALLA bloque $total salio con $rc"; malos=$((malos + 1)); }
  done
  echo "bloques=$total fallados=$malos"
} 2>&1 </dev/null | sed "s|$HOME|\$HOME|g" >>"$OUT"

# El shell del bucle corre en una subshell por la tuberia: el resultado se lee del archivo.
grep -q '^bloques=[0-9]* fallados=0$' "$OUT" && ! grep -q '^FALLA' "$OUT"
