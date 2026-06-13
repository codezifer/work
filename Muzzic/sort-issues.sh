#!/usr/bin/env bash
#
# sort-issues.sh - Sortiert issues.md nach Priorität
#
# Nutzung:
#   ./sort-issues.sh [--order desc|asc] [--file issues.md]
#
# Standard: desc (hoch -> niedrig)

set -euo pipefail

FILE="issues.md"
ORDER="desc"

usage() {
  echo "Usage: $0 [--order desc|asc] [--file issues.md]"
  echo "  desc = hoch -> niedrig (Standard)"
  echo "  asc  = niedrig -> hoch"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -o|--order)
      ORDER="$2"
      shift 2
      ;;
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

if [[ "$ORDER" != "desc" && "$ORDER" != "asc" ]]; then
  echo "Fehler: --order muss 'desc' oder 'asc' sein."
  usage
fi

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

# Datenzeilen extrahieren, nach Priorität (Feld 6) sortieren
DATA=$(tail -n +"$((SEPARATOR + 1))" "$FILE")

if [[ "$ORDER" == "desc" ]]; then
  SORT_OPT="-rn"
else
  SORT_OPT="-n"
fi

echo "$DATA" | awk -F'|' '{
  prio = $6
  gsub(/^ +| +$/, "", prio)
  print prio "\t" $0
}' \
  | sort $SORT_OPT -k1,1 -s \
  | cut -f2- \
  | sed 's/^[ \t]*|/|/' >> "$TMP"

mv "$TMP" "$FILE"

echo "issues.md nach Priorität sortiert ($ORDER)."
