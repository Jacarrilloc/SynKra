# Rojo de la ronda 2 (F-01, F-02, F-04) contra el codigo de a06869b.
cd "$(git rev-parse --show-toplevel)"
echo '$ F-01: SYNKRA_REGISTRO_IMAGENES=rojo-revisor.local:1 bash scripts/verificar-imagenes.sh   (sin esa imagen en local)'
SYNKRA_REGISTRO_IMAGENES=rojo-revisor.local:1 timeout 60 bash scripts/verificar-imagenes.sh 2>&1 | grep -E 'rojo-revisor|sin-apk|FALLA .*existe' ; echo "salida=${PIPESTATUS[0]}"
echo; echo '$ F-02: copia de HEAD con `from alpine:3.20 AS revision` (minusculas, sin digest) en init-descarga/Dockerfile; verificar-imagenes.sh contra las imagenes buenas'
D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
sed -i 's/^FROM alpine:3.20@sha256:[0-9a-f]* AS revision/from alpine:3.20 AS revision/' "$D/init-descarga/Dockerfile"
grep -n -i '^from' "$D/init-descarga/Dockerfile"
(cd "$D" && bash scripts/verificar-imagenes.sh 2>&1 | grep -E 'FROM|OK imagenes|FALLA'); echo "salida=${PIPESTATUS[0]}"
rm -rf "$D"
echo; echo '$ F-04: openssl falso que escribe un diagnostico y sale con 7'
D=$(mktemp -d); mkdir "$D/c"; printf '#!/bin/sh\necho "openssl: diagnostico-de-prueba" >&2\nexit 7\n' > "$D/openssl"; chmod +x "$D/openssl"
PATH="$D:$PATH" bash scripts/certificado-tls.sh "$D/c"; echo "salida=$?"
echo '$ F-04: mv falso que falla en el segundo mv (tls.crt) -> queda tls.key sin tls.crt?'
rm -f "$D/openssl"
printf '#!/bin/sh\ncase "$2" in */tls.crt) exit 1;; esac\nexec /bin/mv "$@"\n' > "$D/mv"; chmod +x "$D/mv"
PATH="$D:$PATH" bash scripts/certificado-tls.sh "$D/c"; echo "salida=$?"; ls -A "$D/c"
rm -rf "$D"
