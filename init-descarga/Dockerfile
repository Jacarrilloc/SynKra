# Imagen del init container del pod del país (U05-T08). Solo: sh, curl, sha256sum y descargar.sh.
FROM alpine:3.20
RUN apk add --no-cache curl \
 && addgroup -g 10001 -S synkra && adduser -S -u 10001 -G synkra -H -s /sbin/nologin synkra \
 && mkdir /plugins /config && chown 10001:10001 /plugins && chmod 0755 /config
COPY --chmod=0555 descargar.sh /usr/local/bin/descargar.sh
USER 10001:10001
ENTRYPOINT ["/usr/local/bin/descargar.sh"]
