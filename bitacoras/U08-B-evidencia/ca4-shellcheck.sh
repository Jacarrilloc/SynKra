# Log del docker build del init sin cache: la etapa revision corre shellcheck sobre descargar.sh (C-95). Etiqueta desechable.
echo '$ docker build --no-cache --progress=plain -t verde.local:1/init-descarga:1.0.0 init-descarga'
docker build --no-cache --progress=plain -t verde.local:1/init-descarga:1.0.0 init-descarga 2>&1 | grep -v 'Installing\|fetch https'; echo "build=${PIPESTATUS[0]}"
docker rmi verde.local:1/init-descarga:1.0.0 | grep -c Untagged
