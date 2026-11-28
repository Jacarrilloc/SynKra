f=aidlc-docs/construction/U07-despliegue/code/summary.md
test -f "$f" && echo existe
grep -n '^## ' "$f"
for s in 'Qué expone' 'Fronteras' 'Cómo se prueba' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas' 'Candidatas abiertas' 'Cifras y su fuente'; do printf '%s ' "$s"; grep -c "^## $s\$" "$f"; done
for h in H-06 H-08 H-09 H-11 H-12 H-13 H-14 H-16 H-17 H-20 H-24; do printf '%s ' $h; grep -c "$h" "$f"; done
for c in C-30 C-35 C-76 C-82 C-83 C-87 C-90 C-106 C-107 C-108 C-109 C-110 C-112 C-113 C-114 C-115 C-116 C-117 C-118 C-119 C-121 C-122 C-123; do printf '%s ' $c; grep -c "$c" "$f"; done
for r in R-D1 R-D2 R-D3 R-D4 R-D5 R-D6 R-D7 R-D8 R-D9 R-D10 R-D11 R-D12 R-D13; do printf '%s ' $r; grep -cw -- "$r" "$f"; done
grep -n -i 'leerPais\|Plan\.PENDIENTE\|501\|antes de aplicar\|X-Synkra-Usuario\|aceptaAplicar' "$f" | cut -c1-120
grep -c 'David\|Julian' "$f"
