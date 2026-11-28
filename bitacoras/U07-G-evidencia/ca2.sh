#!/bin/bash
# Uso: ca2.sh <mutacion> <commit>   (desde la raíz del worktree; una copia por mutación, sin tocar el worktree)
# mutaciones: registro kubernetes catalogo fabric8 modulonuevo inverso allowed ninguna
m=$1; c=${2:-HEAD}
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
D=$(mktemp -d); git archive "$c" | tar -x -C "$D"; (cd "$D" && git init -q)
J=$D/backend/src/main/java/io/synkra
campo() { sed -i "0,/^    private final /s//    private $2 mutacion;\n    private final /" "$1"; }
case $m in
  registro)   sed -i 's/^final class MotivosRegistro/public final class MotivosRegistro/' $J/registro/MotivosRegistro.java
              campo $J/despliegue/ConfirmacionServicio.java io.synkra.registro.MotivosRegistro ;;
  kubernetes) sed -i 's/^final class GuardiaNamespace/public final class GuardiaNamespace/' $J/kubernetes/GuardiaNamespace.java
              campo $J/despliegue/PlanServicio.java io.synkra.kubernetes.GuardiaNamespace ;;
  catalogo)   sed -i 's/^final class CodigosCatalogo/public final class CodigosCatalogo/' $J/catalogo/CodigosCatalogo.java
              campo $J/despliegue/VerificacionVivo.java io.synkra.catalogo.CodigosCatalogo ;;
  fabric8)    campo $J/despliegue/PlanServicio.java io.fabric8.kubernetes.client.KubernetesClient ;;
  modulonuevo) mkdir -p $J/otro
              printf 'package io.synkra.otro;\n\npublic class Extra {\n}\n' > $J/otro/Extra.java
              campo $J/despliegue/PlanServicio.java io.synkra.otro.Extra ;;
  inverso)    sed -i 's/^final class CodigosDespliegue/public final class CodigosDespliegue/' $J/despliegue/CodigosDespliegue.java
              campo $J/plataforma/ManejadorErrores.java io.synkra.despliegue.CodigosDespliegue ;;
  allowed)    sed -i 's/"registro :: api", //' $J/despliegue/package-info.java ;;
  ninguna)    ;;
esac
echo "== mutacion=$m commit=$(git rev-parse --short $c)"
(cd "$D" && ./gradlew :backend:test --tests '*FronterasModulosTest' 2>&1 | tee "$D/gradle.log" | grep -E "BUILD|FronterasModulosTest > .* FAILED"; true)
echo "salida-gradle-ver-xml:"
grep -B1 '<failure' "$D"/backend/build/test-results/test/TEST-*FronterasModulosTest.xml | grep -o 'testcase name="[A-Za-z0-9]*()"'
[ -n "$KEEP" ] && echo "KEEP=$D" || rm -rf "$D"
