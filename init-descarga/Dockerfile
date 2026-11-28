# Imagen del init container del pod del pais (U05-T08). Solo: sh, curl, sha256sum y descargar.sh.
# Base fijada por digest del indice multi-arquitectura (C-98); la etiqueta queda como referencia legible.
# curl va sin version fija: Alpine solo guarda la ultima de cada rama y un curl=<version> rompe el build (desviacion de C-98).

# Etapa de revision (C-95): si shellcheck encuentra algo en descargar.sh, el build falla.
FROM alpine:3.20@sha256:d9e853e87e55526f6b2917df91a2115c36dd7c696a35be12163d44e6e2a4b6bc AS revision
RUN apk add --no-cache shellcheck
COPY descargar.sh /descargar.sh
RUN shellcheck -s sh /descargar.sh

FROM alpine:3.20@sha256:d9e853e87e55526f6b2917df91a2115c36dd7c696a35be12163d44e6e2a4b6bc
RUN apk add --no-cache curl \
 && addgroup -g 10001 -S synkra && adduser -S -u 10001 -G synkra -H -s /sbin/nologin synkra \
 && mkdir /plugins /config && chown 10001:10001 /plugins && chmod 0755 /config \
 && apk del --no-cache apk-tools
COPY --from=revision --chmod=0555 /descargar.sh /usr/local/bin/descargar.sh
USER 10001:10001
ENTRYPOINT ["/usr/local/bin/descargar.sh"]
