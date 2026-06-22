#!/usr/bin/env bash
#
# sort-issues.sh - Sortiert issues.md nach Status, Priorität, Datum und Titel
#
# Nutzung:
#   ./sort-issues.sh [--file issues.md]

set -euo pipefail

FILE="issues.md"

usage() {
  echo "Usage: $0 [--file issues.md]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -f|--file)
      FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unbekannter Parameter: $1"
      usage
      ;;
  esac
done

if [[ ! -f "$FILE" ]]; then
  echo "Fehler: Datei $FILE nicht gefunden."
  exit 1
fi

HEADER=$(grep -n '^| ID ' "$FILE" | head -1 | cut -d: -f1)
SEPARATOR=$((HEADER + 1))

if [[ -z "$HEADER" ]]; then
  echo "Fehler: Tabellen-Header nicht gefunden."
  exit 1
fi

TMP=$(mktemp)

# Zeilen vor und inkl. Separator unverändert übernehmen
head -n "$SEPARATOR" "$FILE" > "$TMP"

# Datenzeilen extrahieren
DATA=$(tail -n +"$((SEPARATOR + 1))" "$FILE")

# Sortier-Logik:
# 1. Status (🔲 vor ✅) -> Wir mappen 🔲 auf 0 und ✅ auf 1
# 2. Priorität (3 -> 1) -> Numerisch absteigend
# 3. Datum (Neuestes zuerst) -> String absteigend
# 4. Titel (Alphabetisch) -> String aufsteigend

echo "$DATA" | awk -F'|' '{
  status_raw = $7
  gsub(/^[ \t]*~~[ \t]*|[ \t]*~~[ \t]*$/, "", status_raw)
  gsub(/^[ \t]+|[ \t]+$/, "", status_raw)
  status_val = (status_raw ~ /✅/) ? 1 : 0

  prio = $6
  gsub(/^[ \t]*~~[ \t]*|[ \t]*~~[ \t]*$/, "", prio)
  gsub(/^[ \t]+|[ \t]+$/, "", prio)

  date = $3
  gsub(/^[ \t]*~~[ \t]*|[ \t]*~~[ \t]*$/, "", date)
  gsub(/^[ \t]+|[ \t]+$/, "", date)

  title = $4
  gsub(/^[ \t]*~~[ \t]*|[ \t]*~~[ \t]*$/, "", title)
  gsub(/^[ \t]+|[ \t]+$/, "", title)

  print status_val "\t" prio "\t" date "\t" title "\t" $0
}' | sort -t $'\t' -k1,1n -k2,2rn -k3,3r -k4,4 | cut -f5- >> "$TMP"

mv "$TMP" "$FILE"

echo "issues.md sortiert nach Status, Priorität, Datum und Titel."
