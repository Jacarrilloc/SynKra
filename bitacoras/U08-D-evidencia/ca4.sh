#!/usr/bin/env bash
# CA-4: la segunda corrida, sobre la misma instalacion, tambien termina con 0.
time ./scripts/e2e-api.sh; echo "e2e-2=$?"
