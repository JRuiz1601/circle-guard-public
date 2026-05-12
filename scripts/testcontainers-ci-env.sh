#!/usr/bin/env bash
# Emite en stdout: export TESTCONTAINERS_HOST_OVERRIDE=... (vacío si no aplica).
# Uso en CI: eval "$(bash scripts/testcontainers-ci-env.sh)"

set -euo pipefail

if [[ ! -f /.dockerenv ]] || [[ -n "${TESTCONTAINERS_HOST_OVERRIDE:-}" ]]; then
  exit 0
fi

GW=""
GW="$(getent ahostsv4 host.docker.internal 2>/dev/null | awk '{print $1; exit}' || true)"
if [[ -z "$GW" ]] && command -v docker >/dev/null 2>&1; then
  GW="$(docker run --rm --add-host=host.docker.internal:host-gateway alpine:3.19 getent ahostsv4 host.docker.internal 2>/dev/null | head -n1 | awk '{print $1}' || true)"
fi
if [[ -z "$GW" ]] && command -v docker >/dev/null 2>&1; then
  GW="$(docker network inspect bridge -f '{{(index .IPAM.Config 0).Gateway}}' 2>/dev/null || true)"
  [[ "$GW" == "<no value>" ]] && GW=""
fi
if [[ -z "$GW" ]]; then
  LINE="$(ip -4 route list match 0/0 2>/dev/null | head -n1 || true)"
  [[ -z "$LINE" ]] && LINE="$(ip route 2>/dev/null | grep '^default' | head -n1 || true)"
  if [[ -n "$LINE" ]]; then
    # shellcheck disable=SC2086
    set -- $LINE
    if [[ "$1" == default && "$2" == via ]]; then
      GW="$3"
    fi
  fi
fi
[[ -z "$GW" ]] && GW=172.17.0.1

printf 'export TESTCONTAINERS_HOST_OVERRIDE=%q\n' "$GW"
