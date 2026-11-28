# Imagen del backend: solo empaqueta el JAR compilado en el host (./gradlew :backend:bootJar).
# Base fijada por digest del indice multi-arquitectura (C-101); la etiqueta queda como referencia legible.
FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70 AS capas
WORKDIR /app
COPY build/libs/backend-*.jar /jars/
RUN test "$(ls /jars | wc -l)" -eq 1 || { echo "ERROR: hay mas de un backend-*.jar en build/libs; borra los viejos" >&2; ls /jars >&2; exit 1; } \
 && mv /jars/backend-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extraido

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
WORKDIR /app
COPY --from=capas /extraido/dependencies/ ./
COPY --from=capas /extraido/spring-boot-loader/ ./
COPY --from=capas /extraido/snapshot-dependencies/ ./
COPY --from=capas /extraido/application/ ./
RUN addgroup -g 10001 -S synkra && adduser -S -u 10001 -G synkra -H synkra \
 && mkdir /data && chown 10001:10001 /data
USER 10001:10001
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
