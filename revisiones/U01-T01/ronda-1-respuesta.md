# Respuesta a la ronda 1 — U01-T01

F-01: Corregido en el commit de la ronda 2 (rama tarea/U01-T01): se regeneró el wrapper con `./gradlew wrapper --gradle-version 9.9.2 ...` ejecutado por el propio wrapper 9.9.2; el jar ahora tiene sha256 621008af...cdaf8 (el oficial) y gradlew, gradlew.bat y propiedades son los de 9.9.2. La afirmación errónea de la ronda 1 queda corregida en la bitácora.
F-02: Fuera de alcance: plugin de convención en buildSrc/build incluido, tarea candidata propuesta (a decidir en U03-T01 cuando entre Spring Boot); no se amplía el alcance.
F-03: Fuera de alcance: fijar la Daemon JVM (`gradle/gradle-daemon-jvm.properties`) queda como tarea candidata; CA-2 pasa y el toolchain fija la compilación en 21.
