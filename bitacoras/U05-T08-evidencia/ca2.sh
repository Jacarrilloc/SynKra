#!/bin/sh
# CA-2: corre la imagen contra un servidor HTTP local en el host (--network host).
set -u
T=$(mktemp -d); chmod 755 "$T"
trap 'kill $PID 2>/dev/null; rm -rf "$T"' EXIT
mkdir "$T/srv"
printf 'contenido de plugin de prueba' > "$T/srv/plugin.jar"
SHA=$(sha256sum "$T/srv/plugin.jar" | cut -d' ' -f1)
cp "$T/srv/plugin.jar" "$T/srv/$SHA"
cat > "$T/s.py" <<'PY'
import http.server, os, sys
raiz = sys.argv[1]
class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        r = os.path.join(raiz, self.path.rsplit("/", 1)[-1])
        if self.path.startswith("/api/artefactos/") and os.path.isfile(r):
            d = open(r, "rb").read(); self.send_response(200); self.send_header("Content-Length", str(len(d))); self.end_headers(); self.wfile.write(d)
        else:
            self.send_response(404); self.end_headers()
    def log_message(self, *a): pass
s = http.server.HTTPServer(("127.0.0.1", 0), H)
open(sys.argv[2], "w").write(str(s.server_port)); s.serve_forever()
PY
python3 "$T/s.py" "$T/srv" "$T/puerto" & PID=$!
while [ ! -s "$T/puerto" ]; do sleep 0.1; done
PORT=$(cat "$T/puerto"); echo "servidor en 127.0.0.1:$PORT"
corrida() { # etiqueta hash
    D=$T/$1; mkdir -p "$D/config" "$D/plugins"; chmod 755 "$D" "$D/config"; chmod 777 "$D/plugins"
    echo "$2 plugin.jar" > "$D/config/artefactos.txt"; chmod 644 "$D/config/artefactos.txt"
    echo "--- $1"
    docker run --rm --network host -e SYNKRA_URL=http://127.0.0.1:$PORT -v "$D/config:/config:ro" -v "$D/plugins:/plugins" synkra-init-descarga:1.0.0; echo "salida=$?"
    echo '$ ls -l plugins'; ls -la "$D/plugins"
}
corrida hash-correcto "$SHA"
BAD=$(echo "$SHA" | sed 's/^./0/; s/^00/01/')
# El servidor sirve el contenido original bajo el hash alterado: la descarga llega, pero su sha256 no coincide.
cp "$T/srv/plugin.jar" "$T/srv/$BAD"
corrida hash-alterado "$BAD"
