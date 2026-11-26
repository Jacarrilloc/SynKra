# Imagen del backend: solo empaqueta el JAR compilado en el host (./gradlew :backend:bootJar).
FROM eclipse-temurin:21-jre-alpine AS capas
WORKDIR /app
COPY build/libs/backend-0.1.0.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extraido

FROM eclipse-temurin:21-jre-alpine
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
