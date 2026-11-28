#!/usr/bin/env bash
# Genera un certificado autofirmado para synkra.local (EC P-256, 365 dias) en <directorio>/tls.crt y tls.key.
# Uso: bash scripts/certificado-tls.sh <directorio>   (no sobrescribe; no habla con ningun cluster)
set -euo pipefail

DIR="${1:-}"
[ -n "$DIR" ] && [ -d "$DIR" ] || { echo "FALLA certificado: el directorio '$DIR' no existe" >&2; exit 1; }
if [ -e "$DIR/tls.crt" ] || [ -e "$DIR/tls.key" ]; then
  echo "FALLA certificado: ya hay tls.crt o tls.key en $DIR; no se sobrescriben" >&2
  exit 1
fi

TMP_CRT="$DIR/.tls.crt.$$"; TMP_KEY="$DIR/.tls.key.$$"
LISTO=0
limpiar() { rm -f "$TMP_CRT" "$TMP_KEY"; [ "$LISTO" -eq 1 ] || rm -f "$DIR/tls.key" "$DIR/tls.crt"; }
trap limpiar EXIT
umask 077

rc=0
SALIDA="$(openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:P-256 -nodes -days 365 \
  -subj "/CN=synkra.local" -addext "subjectAltName=DNS:synkra.local" \
  -keyout "$TMP_KEY" -out "$TMP_CRT" 2>&1)" || rc=$?
[ "$rc" -eq 0 ] || {
  [ -z "$SALIDA" ] || printf '%s\n' "$SALIDA" >&2
  echo "FALLA certificado: openssl req -x509 salió con $rc" >&2; exit 1; }

chmod 0600 "$TMP_KEY"; chmod 0644 "$TMP_CRT"
mv "$TMP_KEY" "$DIR/tls.key" && mv "$TMP_CRT" "$DIR/tls.crt" || { echo "FALLA certificado: no se pudieron mover los archivos a $DIR" >&2; exit 1; }
LISTO=1
echo "OK certificado: $DIR/tls.crt y $DIR/tls.key"
