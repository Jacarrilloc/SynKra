#!/usr/bin/env bash
# CA-5: rojo primero. Con el codigo del primer commit (sin el arreglo): las pruebas nuevas caen y el e2e cae en CU3 sobre un kind nuevo.
./gradlew :backend:test --rerun --tests 'io.synkra.kubernetes.ConstruirObjetosPaisTest'; echo "gradle=$?"
make cluster-delete; echo "borrar=$?"
make install > /dev/null 2>&1; echo "install=$?"
time ./scripts/e2e-api.sh; echo "e2e=$?"
