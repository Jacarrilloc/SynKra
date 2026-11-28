#!/usr/bin/env bash
# U08-E, CA-1 (las comprobaciones sobre la evidencia de la corrida fresca) y CA-2 (secciones y privacidad).
# Uso (desde la raiz del worktree): bash bitacoras/U08-E-evidencia/ca1-ca2.sh
set -u
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
E=aidlc-docs/construction/U08-instalacion-limite/code/evidencia
S=aidlc-docs/construction/U08-instalacion-limite/code/summary.md
echo "fecha: $(date -Iseconds)"
echo "sha: $(git rev-parse --short HEAD)"
echo '$ grep -c '"'"'^salida=0$'"'"' '$E'/{install,limite,e2e}.txt'
grep -c '^salida=0$' $E/{install,limite,e2e}.txt
echo '$ grep -F '"'"'OK e2e'"'"' '$E'/e2e.txt'
grep -F 'OK e2e' $E/e2e.txt
echo '$ head -6 de cada archivo (cabeceras)'
for f in install limite e2e; do echo "== $f"; head -6 $E/$f.txt; done
echo '$ secciones del resumen'
for s in 'Qué se construyó' 'Cómo se prueba' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas y rondas' 'Candidatas abiertas' 'Cifras y su fuente'; do printf '%s: ' "$s"; grep -c "^## $s" $S; done
echo '$ secciones de la guia'
for s in 'Requisitos' 'Instalar' 'Comprobar el límite' 'Entrar' 'Usar cada función' 'El recorrido completo' 'Errores comunes' 'Desinstalar'; do printf '%s: ' "$s"; grep -c "^## $s" GUIA.md; done
echo '$ orden de las secciones (numero de linea)'
grep -n '^## ' $S | cut -d: -f1 | paste -sd' ' -
grep -n '^## ' GUIA.md | cut -d: -f1 | paste -sd' ' -
echo '$ grep -cE '"'"'David|Julian|casdav|/home/'"'"' GUIA.md '$S
grep -cE 'David|Julian|casdav|/home/' GUIA.md $S
