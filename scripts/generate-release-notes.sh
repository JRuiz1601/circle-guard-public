#!/usr/bin/env bash
# Genera una entrada en CHANGELOG.md a partir de commits recientes (Conventional Commits).
# Idempotente por SHA: no duplica la misma versión [auto-<shortsha>].
#
# Variables opcionales:
#   RELEASE_VERSION  — forzar el título de sección (por defecto: auto-<git short>)
#   GIT_DEPTH        — cuántos commits analizar (default 80)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHANGELOG="$ROOT/CHANGELOG.md"
DEPTH="${GIT_DEPTH:-80}"

cd "$ROOT"
SHORT="$(git rev-parse --short HEAD)"
VERSION="${RELEASE_VERSION:-auto-${SHORT}}"
MARKER="## [${VERSION}]"

if [[ -f "$CHANGELOG" ]] && grep -Fq "$MARKER" "$CHANGELOG"; then
  echo "generate-release-notes: ya existe $MARKER — sin cambios."
  exit 0
fi

mapfile -t commits < <(git log "-${DEPTH}" --pretty=format:'%s')

feat=()
fix=()
chore=()
docs=()
other=()

for s in "${commits[@]}"; do
  if [[ "$s" == feat:* ]] || [[ "$s" == feat\(* ]]; then feat+=("$s")
  elif [[ "$s" == fix:* ]] || [[ "$s" == fix\(* ]]; then fix+=("$s")
  elif [[ "$s" == chore:* ]] || [[ "$s" == chore\(* ]]; then chore+=("$s")
  elif [[ "$s" == docs:* ]] || [[ "$s" == docs\(* ]]; then docs+=("$s")
  else other+=("$s")
  fi
done

ISO="$(date -u +'%Y-%m-%d %H:%M UTC')"
TMP="$(mktemp)"

{
  echo ""
  echo "$MARKER - $ISO"
  echo ""
  echo "Resumen automático desde \`git log\` (Conventional Commits)."
  echo ""

  if ((${#feat[@]})); then
    echo "### feat"
    for x in "${feat[@]}"; do echo "- $x"; done
    echo ""
  fi
  if ((${#fix[@]})); then
    echo "### fix"
    for x in "${fix[@]}"; do echo "- $x"; done
    echo ""
  fi
  if ((${#chore[@]})); then
    echo "### chore"
    for x in "${chore[@]}"; do echo "- $x"; done
    echo ""
  fi
  if ((${#docs[@]})); then
    echo "### docs"
    for x in "${docs[@]}"; do echo "- $x"; done
    echo ""
  fi
  if ((${#other[@]})) && ((${#other[@]} <= 25)); then
    echo "### otros"
    for x in "${other[@]}"; do echo "- $x"; done
    echo ""
  fi
} >"$TMP"

if [[ ! -f "$CHANGELOG" ]]; then
  {
    echo "# Changelog"
    echo ""
    echo "Formato inspirado en [Keep a Changelog](https://keepachangelog.com/) y [Conventional Commits](https://www.conventionalcommits.org/)."
    echo ""
    echo "## [Unreleased]"
    cat "$TMP"
  } >"$CHANGELOG"
  rm -f "$TMP"
  echo "generate-release-notes: creado $CHANGELOG con $MARKER"
  exit 0
fi

OUT="$(mktemp)"
awk -v insertfile="$TMP" '
  BEGIN { inserted=0 }
  /^## \[Unreleased\]/ {
    print
    if (!inserted) {
      while ((getline line < insertfile) > 0) print line
      close(insertfile)
      inserted=1
    }
    next
  }
  { print }
' "$CHANGELOG" >"$OUT"
mv "$OUT" "$CHANGELOG"
rm -f "$TMP"

echo "generate-release-notes: insertado bajo [Unreleased]: $MARKER"
echo "Nota: para versionar en Git, haz commit y push de CHANGELOG.md desde tu máquina o con credenciales en Jenkins."
