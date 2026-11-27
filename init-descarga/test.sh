#!/bin/sh
# Prueba de descargar.sh contra un servidor HTTP local (solo 127.0.0.1).
set -u
AQUI=$(cd "$(dirname "$0")" && pwd)
SCRIPT=${DESCARGAR:-$AQUI/descargar.sh}
T=$(mktemp -d)
SRV_PID=
limpiar() {
    [ -n "$SRV_PID" ] && kill "$SRV_PID" 2>/dev/null
    rm -rf "$T"
}
trap limpiar EXIT INT TERM

# Servidor: sirve $T/srv/<sha> en /api/artefactos/<sha>; /api/artefactos/<sha> con
# archivo "<sha>.500" responde 500. Registra cada petición en $T/peticiones.log.
cat >"$T/servidor.py" <<'PY'
import http.server, os, sys
raiz, log = sys.argv[1], sys.argv[2]
class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        with open(log, "a") as f:
            f.write(self.path + "\n")
        pref = "/api/artefactos/"
        sha = self.path[len(pref):] if self.path.startswith(pref) else ""
        ruta = os.path.join(raiz, sha)
        if sha and os.path.exists(ruta + ".500"):
            self.send_response(500); self.end_headers(); return
        if sha and "/" not in sha and os.path.isfile(ruta):
            datos = open(ruta, "rb").read()
            self.send_response(200)
            self.send_header("Content-Length", str(len(datos)))
            self.send_header("X-Synkra-Sha256", sha)
            self.end_headers(); self.wfile.write(datos); return
        self.send_response(404); self.end_headers()
    def log_message(self, *a): pass
s = http.server.HTTPServer(("127.0.0.1", 0), H)
open(os.path.join(raiz, "..", "puerto"), "w").write(str(s.server_port))
s.serve_forever()
PY
mkdir "$T/srv"
: >"$T/peticiones.log"
python3 "$T/servidor.py" "$T/srv" "$T/peticiones.log" &
SRV_PID=$!
i=0
while [ ! -s "$T/puerto" ] && [ "$i" -lt 100 ]; do sleep 0.1; i=$((i + 1)); done
[ -s "$T/puerto" ] || { echo "FALLA servidor-no-arranco"; exit 1; }
PUERTO=$(cat "$T/puerto")
URL_OK=http://127.0.0.1:$PUERTO
# Puerto cerrado: se abre uno efímero y se cierra.
PUERTO_CERRADO=$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1])')
URL_CAIDA=http://127.0.0.1:$PUERTO_CERRADO

FALLOS=0
ok() { echo "ok $1"; }
falla() { echo "FALLA $1: $2"; FALLOS=$((FALLOS + 1)); }

sha_de() { printf '%s' "$1" | sha256sum | cut -d' ' -f1; }
# Prepara un caso: C=config, P=plugins limpios y log de peticiones vacío.
nuevo() {
    C=$T/$1/config; P=$T/$1/plugins
    rm -rf "$T/$1"; mkdir -p "$C" "$P"
    rm -f "$T"/srv/*; : >"$T/peticiones.log"
}
# Ejecuta el script; deja código en $COD y salida en $SAL.
correr() { # url
    SAL=$T/salida.txt
    env PATH="${PATH_EXTRA:+$PATH_EXTRA:}$PATH" \
        SYNKRA_URL="$1" SYNKRA_CONFIG="$C" SYNKRA_PLUGINS="$P" SYNKRA_ESPERA_BASE="${BASE:-0}" \
        sh "$SCRIPT" >"$SAL" 2>&1
    COD=$?
}
vacio() { [ -z "$(ls -A "$P")" ]; }

# 1. hash correcto
caso=hash-correcto
nuevo c1
CONT=contenido-uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -eq 0 ] && [ "$(ls -A "$P")" = "plugin-a-1.0.jar" ] \
    && [ "$(sha256sum <"$P/plugin-a-1.0.jar" | cut -d' ' -f1)" = "$SHA" ]; then ok $caso
else falla $caso "cod=$COD plugins=$(ls -A "$P")"; fi

# 2. hash alterado
caso=hash-alterado
nuevo c2
SHA=$(sha_de bueno); printf '%s' malo >"$T/srv/$SHA"
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && vacio && grep -q 'hash distinto' "$SAL"; then ok $caso
else falla $caso "cod=$COD plugins=$(ls -A "$P")"; fi

# 3. catálogo caído: 5 intentos exactos
caso=catalogo-caido
nuevo c3
SHA=$(sha_de x)
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_CAIDA"
N=$(grep -c 'intento [0-9]*/5' "$SAL")
if [ "$COD" -ne 0 ] && [ "$N" -eq 5 ] && vacio && grep -q 'no responde tras 5 intentos' "$SAL"; then ok $caso
else falla $caso "cod=$COD intentos=$N"; fi

# 3b. 500 persistente: también 5 intentos y peticiones reales
caso=http-500
nuevo c3b
SHA=$(sha_de x); : >"$T/srv/$SHA.500"
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_OK"
N=$(grep -c 'intento [0-9]*/5' "$SAL"); R=$(wc -l <"$T/peticiones.log")
if [ "$COD" -ne 0 ] && [ "$N" -eq 5 ] && [ "$R" -eq 5 ] && vacio; then ok $caso
else falla $caso "cod=$COD intentos=$N peticiones=$R"; fi

# 3c. 404: no se reintenta
caso=http-404-sin-reintento
nuevo c3c
SHA=$(sha_de x)
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_OK"
R=$(wc -l <"$T/peticiones.log")
if [ "$COD" -ne 0 ] && [ "$R" -eq 1 ] && vacio; then ok $caso
else falla $caso "cod=$COD peticiones=$R"; fi

# 4. nombres con ../ o /
for mal in '../evil.jar' '/abs.jar' 'a/b.jar' '..'; do
    caso="nombre-malo($mal)"
    nuevo c4
    SHA=$(sha_de x); printf x >"$T/srv/$SHA"
    echo "$SHA $mal" >"$C/artefactos.txt"
    correr "$URL_OK"
    R=$(wc -l <"$T/peticiones.log")
    if [ "$COD" -ne 0 ] && [ "$R" -eq 0 ] && vacio && [ ! -e "$T/c4/evil.jar" ]; then ok "$caso"
    else falla "$caso" "cod=$COD peticiones=$R"; fi
done

# 4b. hash mal formado
caso=hash-mal-formado
nuevo c4b
echo "ABC plugin-a-1.0.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && [ "$(wc -l <"$T/peticiones.log")" -eq 0 ] && vacio; then ok $caso
else falla $caso "cod=$COD"; fi

# 4c. línea mala después de una buena: no se descarga nada
caso=linea-mala-tras-buena
nuevo c4c
SHA=$(sha_de uno); printf uno >"$T/srv/$SHA"
printf '%s a-1.jar\n%s ../b.jar\n' "$SHA" "$SHA" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && [ "$(wc -l <"$T/peticiones.log")" -eq 0 ] && vacio; then ok $caso
else falla $caso "cod=$COD peticiones=$(wc -l <"$T/peticiones.log")"; fi

# 5. dos JAR, el segundo alterado
caso=segundo-alterado
nuevo c5
S1=$(sha_de uno); printf uno >"$T/srv/$S1"
S2=$(sha_de dos); printf otro >"$T/srv/$S2"
printf '%s a-1.jar\n%s b-1.jar\n' "$S1" "$S2" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && vacio && grep -q 'hash distinto' "$SAL"; then ok $caso
else falla $caso "cod=$COD plugins=$(ls -A "$P")"; fi

# 5b. dos JAR correctos
caso=dos-correctos
nuevo c5b
S1=$(sha_de uno); printf uno >"$T/srv/$S1"
S2=$(sha_de dos); printf dos >"$T/srv/$S2"
printf '%s a-1.jar\n%s b-1.jar\n' "$S1" "$S2" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -eq 0 ] && [ "$(ls -A "$P" | tr '\n' ' ')" = "a-1.jar b-1.jar " ]; then ok $caso
else falla $caso "cod=$COD plugins=$(ls -A "$P")"; fi

# 6. artefactos.txt vacío
caso=lista-vacia
nuevo c6
: >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -eq 0 ] && vacio; then ok $caso; else falla $caso "cod=$COD"; fi

# 7. sin SYNKRA_URL
caso=sin-url
nuevo c7
SHA=$(sha_de x); echo "$SHA a-1.jar" >"$C/artefactos.txt"
env -u SYNKRA_URL SYNKRA_CONFIG="$C" SYNKRA_PLUGINS="$P" SYNKRA_ESPERA_BASE=0 sh "$SCRIPT" >"$T/salida.txt" 2>&1
COD=$?
if [ "$COD" -ne 0 ] && grep -q 'SYNKRA_URL' "$T/salida.txt" && vacio; then ok $caso
else falla $caso "cod=$COD"; fi

# 8. sin artefactos.txt
caso=sin-lista
nuevo c8
correr "$URL_OK"
if [ "$COD" -ne 0 ] && vacio; then ok $caso; else falla $caso "cod=$COD"; fi

# 9. espera creciente (decisión 5): sleep falso al frente del PATH que registra sus argumentos
caso=espera-creciente
nuevo c9
mkdir -p "$T/falso"
printf '#!/bin/sh\necho "$1" >>"%s/sleeps.log"\n' "$T" >"$T/falso/sleep"
chmod +x "$T/falso/sleep"; : >"$T/sleeps.log"
SHA=$(sha_de x)
echo "$SHA plugin-a-1.0.jar" >"$C/artefactos.txt"
PATH_EXTRA=$T/falso BASE=1 correr "$URL_CAIDA"
ESP=$(tr '\n' ' ' <"$T/sleeps.log")
if [ "$COD" -ne 0 ] && [ "$ESP" = "1 2 4 8 " ]; then ok $caso
else falla $caso "cod=$COD esperas=$ESP"; fi

# 10. SYNKRA_ESPERA_BASE debe ser entero no negativo
for mal in 0.5 -1 abc; do
    caso="espera-base-invalida($mal)"
    nuevo c10
    SHA=$(sha_de x); echo "$SHA a-1.jar" >"$C/artefactos.txt"
    BASE=$mal correr "$URL_OK"
    if [ "$COD" -eq 2 ] && grep -q 'SYNKRA_ESPERA_BASE' "$SAL" \
        && [ "$(wc -l <"$T/peticiones.log")" -eq 0 ] && vacio; then ok "$caso"
    else falla "$caso" "cod=$COD"; fi
done

# 11. línea vacía en medio de la lista: se salta (decisión ratificada)
caso=linea-vacia-en-medio
nuevo c11
S1=$(sha_de uno); printf uno >"$T/srv/$S1"
S2=$(sha_de dos); printf dos >"$T/srv/$S2"
printf '%s a-1.jar\n\n%s b-1.jar\n' "$S1" "$S2" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -eq 0 ] && [ "$(ls -A "$P" | tr '\n' ' ')" = "a-1.jar b-1.jar " ]; then ok $caso
else falla $caso "cod=$COD plugins=$(ls -A "$P")"; fi

# 12. enlaces (F-01). A: temporal antiguo es enlace a un archivo de fuera
caso=enlace-en-temporal-antiguo
nuevo c12a
mkdir "$T/c12a/fuera"; echo original >"$T/c12a/fuera/victima"
ln -s "$T/c12a/fuera/victima" "$P/.a-1.jar.parcial"
CONT=uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA a-1.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$(cat "$T/c12a/fuera/victima")" = original ] && [ -z "$(ls -A "$T/c12a/fuera" | grep -v '^victima$')" ] \
    && { [ "$COD" -ne 0 ] || { [ -f "$P/a-1.jar" ] && [ ! -L "$P/a-1.jar" ]; }; }; then ok $caso
else falla $caso "cod=$COD victima=$(cat "$T/c12a/fuera/victima")"; fi

# 12b. B: el destino es un enlace a un directorio de fuera
caso=enlace-en-destino
nuevo c12b
mkdir "$T/c12b/fuera"; ln -s "$T/c12b/fuera" "$P/a-1.jar"
CONT=uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA a-1.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && [ -z "$(ls -A "$T/c12b/fuera")" ] && [ -L "$P/a-1.jar" ] \
    && [ "$(wc -l <"$T/peticiones.log")" -eq 0 ]; then ok $caso
else falla $caso "cod=$COD fuera=$(ls -A "$T/c12b/fuera")"; fi

# 12c. destino enlace a archivo de fuera: se niega y no lo toca
caso=enlace-a-archivo-en-destino
nuevo c12c
mkdir "$T/c12c/fuera"; echo original >"$T/c12c/fuera/victima"; ln -s "$T/c12c/fuera/victima" "$P/a-1.jar"
CONT=uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA a-1.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -ne 0 ] && [ "$(cat "$T/c12c/fuera/victima")" = original ]; then ok $caso
else falla $caso "cod=$COD"; fi

# 12d. $PLUGINS mismo es un enlace: se niega sin escribir
caso=plugins-es-enlace
nuevo c12d
mkdir "$T/c12d/real"; ln -s "$T/c12d/real" "$T/c12d/enlace"
CONT=uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA a-1.jar" >"$C/artefactos.txt"
P=$T/c12d/enlace correr "$URL_OK"
if [ "$COD" -ne 0 ] && [ -z "$(ls -A "$T/c12d/real")" ]; then ok $caso
else falla $caso "cod=$COD"; fi

# 12e. un archivo regular previo con el mismo nombre se reemplaza (el emptyDir sobrevive a reinicios)
caso=archivo-previo-se-reemplaza
nuevo c12e
echo viejo >"$P/a-1.jar"
CONT=uno; SHA=$(sha_de "$CONT"); printf '%s' "$CONT" >"$T/srv/$SHA"
echo "$SHA a-1.jar" >"$C/artefactos.txt"
correr "$URL_OK"
if [ "$COD" -eq 0 ] && [ "$(sha256sum <"$P/a-1.jar" | cut -d' ' -f1)" = "$SHA" ]; then ok $caso
else falla $caso "cod=$COD"; fi

# 13. protege la línea set -eu del script (quitarla no rompe ningún otro caso)
caso=set-eu-presente
if grep -q '^set -eu$' "$SCRIPT"; then ok $caso; else falla $caso "falta set -eu"; fi

if [ "$FALLOS" -ne 0 ]; then echo "$FALLOS caso(s) con FALLA"; exit 1; fi
echo "todos los casos ok"
