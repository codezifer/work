#!/usr/bin/env bash
#
# close-issue.sh - Markiert ein Issue in issues.md als erledigt (✅)
#
# Nutzung:
#   ./close-issue.sh --id 0001
#   ./close-issue.sh -i 0001 -f issues.md

set -euo pipefail

FILE="issues.md"
ID=""

usage() {
  echo "Usage: $0 --id 0001 [--file issues.md]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -i|--id)
      ID="$2"
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

if [[ -z "$ID" ]]; then
  echo "Fehler: --id ist erforderlich."
  usage
fi

# ID auf 4-stellig normalisieren
ID=$(printf "%04d" "$((10#$ID))")

if [[ ! -f "$FILE" ]]; then
  echo "Fehler: Datei $FILE nicht gefunden."
  exit 1
fi

if ! grep -qE "^\| $ID \|" "$FILE"; then
  echo "Fehler: Issue $ID nicht gefunden in $FILE."
  exit 1
fi

TMP=$(mktemp)
awk -v id="$ID" '
BEGIN { FS="|"; OFS="|" }
{
  if ($0 ~ ("^\\| " id " \\|")) {
    n = NF
    # letztes Feld (vor abschließendem leeren Feld durch trailing |) ist Status
    $(n-1) = " ✅ "
  }
  print
}
' "$FILE" > "$TMP"

mv "$TMP" "$FILE"

echo "Issue $ID als erledigt markiert (✅)."
