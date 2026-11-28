# uso: correr.sh caN.sh  -> caN.txt con fecha, sha y comandos
f=$1; { date -Iseconds; git rev-parse --short HEAD; echo "--- script ($f):"; cat bitacoras/U07-D-evidencia/$f; echo "--- salida:"; bash bitacoras/U07-D-evidencia/$f 2>&1; } > bitacoras/U07-D-evidencia/${f%.sh}.txt
