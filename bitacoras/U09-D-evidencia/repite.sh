#!/usr/bin/env bash
# Repite un guion de CA-7 N veces y deja solo los testigos. Uso: bash repite.sh <guion> <N>
for i in $(seq "$2"); do echo "--- $1 corrida $i"; bash "bitacoras/U09-D-evidencia/$1.sh" 2>&1 | grep -E '^(variante|desplegar|restaurado)=|registro-cambio|^corre-|FALLA|^[0-9]+:for D'; done
