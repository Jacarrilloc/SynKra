r() { echo "\$ $*"; eval "$@"; }
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
r 'bash scripts/certificado-tls.sh "$D"; echo "salida=$?"'
r 'ls "$D"; stat -c "%a %n" "$D/tls.key"'
r 'openssl x509 -in "$D/tls.crt" -noout -subject -ext subjectAltName'
r 'openssl x509 -in "$D/tls.crt" -noout -checkend 86400; echo "vigente=$?"'
r '[ "$(openssl x509 -in "$D/tls.crt" -noout -pubkey)" = "$(openssl pkey -in "$D/tls.key" -pubout)" ] && echo "par-ok"'
r 'sha256sum "$D/tls.crt" "$D/tls.key" | sed "s|$D|<dir>|"'
r 'bash scripts/certificado-tls.sh "$D"; echo "repetido=$?"'
r 'sha256sum "$D/tls.crt" "$D/tls.key" | sed "s|$D|<dir>|"'
r 'bash scripts/certificado-tls.sh "$D/no-existe"; echo "sin-dir=$?"'
