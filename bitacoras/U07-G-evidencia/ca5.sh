f=aidlc-docs/construction/U07-despliegue/code/summary.md
sed -n '/^## Cifras y su fuente/,/^## [^C]/p' "$f" | grep '^| ' | tail -n +3 | while IFS='|' read -r _ c r s _; do
  r=$(echo "$r" | tr -d ' `'); s=$(echo "$s" | sed 's/^ *`//; s/` *$//')
  if grep -qF -- "$s" "$r"; then echo "ok $r"; else echo "FALTA $r :: $s"; fi
done | sort | uniq -c
