d=aidlc-docs/construction/U07-despliegue/code
awk '/^```/{b=!b;next} b' "$d/summary.md" | grep -v '^\$ ' | while IFS= read -r l; do
  [ -z "$l" ] && continue
  grep -rqxF -- "$l" "$d/evidencia" bitacoras/U07-G-evidencia bitacoras/U07-A-evidencia bitacoras/U07-B-evidencia bitacoras/U07-C-evidencia bitacoras/U07-D-evidencia bitacoras/U07-E-evidencia bitacoras/U07-F-evidencia revisiones/U07-A revisiones/U07-B revisiones/U07-C revisiones/U07-D revisiones/U07-E revisiones/U07-F || echo "NO LITERAL: $l"
done
grep -c '\[\.\.\.\]' "$d/summary.md"
