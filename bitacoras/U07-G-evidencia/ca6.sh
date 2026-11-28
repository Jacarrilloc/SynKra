d=aidlc-docs/construction/U07-despliegue/code
awk '/^```/{b=!b;next} b' "$d/summary.md" | grep -v '^\$ ' | while IFS= read -r l; do
  [ -z "$l" ] && continue
  grep -rqxF -- "$l" "$d/evidencia" bitacoras/U07-G-evidencia bitacoras/U07-A-evidencia bitacoras/U07-B-evidencia bitacoras/U07-C-evidencia bitacoras/U07-D-evidencia bitacoras/U07-E-evidencia bitacoras/U07-F-evidencia revisiones/U07-A revisiones/U07-B revisiones/U07-C revisiones/U07-D revisiones/U07-E revisiones/U07-F || echo "NO LITERAL: $l"
done
grep -c '\[\.\.\.\]' "$d/summary.md"
# Citas textuales: cada una está en el archivo que se nombra como fuente y en el resumen (F-02, ronda 2)
while IFS=$'\t' read -r ruta cita; do
  grep -qF -- "$cita" "$ruta" || echo "CITA NO ESTA EN LA FUENTE: $ruta :: $cita"
  grep -qF -- "$cita" "$d/summary.md" || echo "CITA NO ESTA EN EL RESUMEN: $cita"
done < bitacoras/U07-G-evidencia/citas.txt
