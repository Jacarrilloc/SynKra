#!/bin/sh
# Init container del pod del país (H-15): descarga los JAR de /config/artefactos.txt
# desde el catálogo de SynKra, comprueba su sha256 (C-64) y los deja en /plugins.
# Instalado en la imagen como /usr/local/bin/descargar.sh (C-72).
# Si algo falla, termina con código distinto de 0 y /plugins queda sin JAR de esta corrida.
set -eu

CONFIG=${SYNKRA_CONFIG:-/config}
PLUGINS=${SYNKRA_PLUGINS:-/plugins}
ESPERA_BASE=${SYNKRA_ESPERA_BASE:-1}
MAX_INTENTOS=5
CURL_MAX_TIME=${SYNKRA_CURL_MAX_TIME:-60}
FORMATO='^[0-9a-f]{64} [A-Za-z0-9][A-Za-z0-9._+-]*\.jar$'

error() { echo "error: $*" >&2; }

if [ -z "${SYNKRA_URL:-}" ]; then
    error "falta la variable SYNKRA_URL"
    exit 2
fi
case $ESPERA_BASE in
    '' | *[!0-9]*)
        error "SYNKRA_ESPERA_BASE debe ser un entero no negativo (segundos): $ESPERA_BASE"
        exit 2
        ;;
esac
URL_BASE=${SYNKRA_URL%/}
LISTA=$CONFIG/artefactos.txt
if [ ! -f "$LISTA" ]; then
    error "no existe la lista de artefactos: $LISTA"
    exit 2
fi
if [ ! -d "$PLUGINS" ]; then
    error "no existe el directorio de plugins: $PLUGINS"
    exit 2
fi

if [ -L "$PLUGINS" ]; then
    error "el directorio de plugins es un enlace simbólico: $PLUGINS"
    exit 2
fi

# Comportamiento aceptado: si en $PLUGINS ya hay un archivo regular con el mismo nombre
# (el emptyDir sobrevive a los reinicios del contenedor), se reemplaza; y si esta corrida
# falla después, ese archivo se borra junto con los demás de la corrida. Nunca se escribe
# a través de un enlace simbólico ni sobre un directorio.
destino_seguro() { # nombre
    if [ -L "$PLUGINS/$1" ] || [ -d "$PLUGINS/$1" ]; then
        error "el destino ya existe y es un enlace o un directorio: $PLUGINS/$1"
        exit 7
    fi
}

# Archivos que esta corrida dejó en $PLUGINS (uno por línea), para deshacerlos si algo falla.
CREADOS=""
PARCIAL=""
TERMINADO=0
limpiar() {
    [ -n "$PARCIAL" ] && rm -f -- "$PARCIAL"
    if [ "$TERMINADO" -ne 1 ]; then
        printf '%s' "$CREADOS" | while IFS= read -r f; do
            [ -n "$f" ] && rm -f -- "$PLUGINS/$f"
        done
    fi
}
trap limpiar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# 1. Validar toda la lista antes de descargar nada.
numero=0
while IFS= read -r linea || [ -n "$linea" ]; do
    numero=$((numero + 1))
    [ -z "$linea" ] && continue
    if ! printf '%s\n' "$linea" | grep -Eq "$FORMATO"; then
        error "línea $numero de $LISTA rechazada (formato inválido): $linea"
        exit 3
    fi
    destino_seguro "${linea#* }"
done <"$LISTA"

# 2. Descargar cada uno.
descargar() { # sha nombre
    sha=$1
    nombre=$2
    url=$URL_BASE/api/artefactos/$sha
    destino_seguro "$nombre"
    # Temporal nuevo (mktemp lo crea con O_EXCL: no puede ser un enlace previo), dentro de $PLUGINS.
    PARCIAL=$(mktemp "$PLUGINS/.parcial.XXXXXX") || { error "no se pudo crear un temporal en $PLUGINS"; exit 2; }
    intento=1
    while :; do
        echo "intento $intento/$MAX_INTENTOS: $url" >&2
        rc=0
        codigo=$(curl --fail --silent --show-error --max-time "$CURL_MAX_TIME" \
            --output "$PARCIAL" --write-out '%{http_code}' "$url") || rc=$?
        if [ "$rc" -eq 0 ]; then
            break
        fi
        case $rc:$codigo in
            22:4*)
                error "el catálogo respondió HTTP $codigo para $nombre: $url"
                exit 4
                ;;
        esac
        if [ "$intento" -ge "$MAX_INTENTOS" ]; then
            error "catálogo no responde tras $MAX_INTENTOS intentos: $url"
            exit 5
        fi
        espera=$((ESPERA_BASE * (1 << (intento - 1))))
        echo "fallo (curl $rc, HTTP $codigo); reintento en ${espera}s" >&2
        sleep "$espera"
        intento=$((intento + 1))
    done
    recibido=$(sha256sum <"$PARCIAL" | cut -d' ' -f1)
    if [ "$recibido" != "$sha" ]; then
        error "hash distinto para $nombre: esperado $sha, recibido $recibido"
        exit 6
    fi
    destino_seguro "$nombre"
    chmod 644 "$PARCIAL"
    mv -f -- "$PARCIAL" "$PLUGINS/$nombre"
    PARCIAL=""
    CREADOS="$CREADOS$nombre
"
    echo "ok $nombre ($sha)" >&2
}

while IFS=' ' read -r sha nombre || [ -n "$sha" ]; do
    [ -z "$sha" ] && continue
    descargar "$sha" "$nombre"
done <"$LISTA"

TERMINADO=1
