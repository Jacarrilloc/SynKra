#!/usr/bin/env bash
# U08-E, CA-4: el par rojo-verde de cifras.sh. Cambia a mano una cifra del resumen, una cita y un fragmento de la guia
# (en el mismo archivo, con copia de respaldo que se restaura siempre), corre cifras.sh y muestra los FALTA; despues restaura
# y vuelve a correr cifras.sh sobre los archivos buenos.
# Uso (desde la raiz del worktree): bash bitacoras/U08-E-evidencia/ca4-rojo.sh
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"
S=aidlc-docs/construction/U08-instalacion-limite/code/summary.md
RESPALDO="$(mktemp -d)"
cp "$S" "$RESPALDO/summary.md"
cp GUIA.md "$RESPALDO/GUIA.md"
restaurar() { cp "$RESPALDO/summary.md" "$S"; cp "$RESPALDO/GUIA.md" GUIA.md; rm -rf "$RESPALDO"; }
trap restaurar EXIT
echo "fecha: $(date -Iseconds)"
echo "sha: $(git rev-parse --short HEAD)"
echo "--- rojo: cifra cambiada (84 a 85 s), cifra de criterios cambiada (8 a 7), cita cambiada, fragmento de la guia cambiado"
sed -i 's/`segundos=84`/`segundos=85`/; s/`criterios de aceptación: 8 (CA-1 a CA-8)`/`criterios de aceptación: 7 (CA-1 a CA-7)`/' "$S"
sed -i 's/«La acepto\. Lo único que hay es/«La acepto. Lo único que había es/' "$S"
sed -i 's/^synkra-co  *Active/synkra-cx Active/' GUIA.md
bash bitacoras/U08-E-evidencia/cifras.sh | grep -v '^ok '
echo "cifras-rojo salida=${PIPESTATUS[0]}"
restaurar
mkdir -p "$RESPALDO"
cp "$S" "$RESPALDO/summary.md"
cp GUIA.md "$RESPALDO/GUIA.md"
echo "--- verde: los archivos buenos"
bash bitacoras/U08-E-evidencia/cifras.sh | grep -v '^ok '
echo "cifras-verde salida=${PIPESTATUS[0]}"
