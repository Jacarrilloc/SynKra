r() { echo "\$ $*"; eval "$@"; }
r 'bash -n scripts/imagenes.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh; echo "sintaxis=$?"'
r 'grep -n "set -euo pipefail" scripts/imagenes.sh scripts/verificar-imagenes.sh scripts/certificado-tls.sh'
r "echo 'no: [yaml' | python3 -I scripts/verificar-backend.py; echo \"yaml=\$?\""
r ': | python3 -I scripts/verificar-backend.py; echo "vacio=$?"'
r 'JAVA_HOME=/no/existe bash scripts/imagenes.sh; echo "java=$?"'
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
PATH="$D:$PATH"; printf '#!/bin/sh\nexit 7\n' > "$D/openssl"; chmod +x "$D/openssl"
mkdir "$D/c"
echo '$ (openssl falso que sale con 7 en el PATH) bash scripts/certificado-tls.sh "$D/c"; echo "openssl=$?"; ls -A "$D/c" | wc -l'
bash scripts/certificado-tls.sh "$D/c"; echo "openssl=$?"; ls -A "$D/c" | wc -l
