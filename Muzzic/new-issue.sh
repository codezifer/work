#!/usr/bin/env bash
#
# new-issue.sh - Fügt ein neues Issue in issues.md hinzu
#
# Nutzung:
#   ./new-issue.sh --title "Titel" --description "Beschreibung"
#   ./new-issue.sh -t "Titel" -d "Beschreibung"
#
# Erstellt automatisch:
#   - die nächste fortlaufende ID (4-stellig, z.B. 0002)
#   - das aktuelle Datum (YYYY-MM-DD)
#   - Status standardmäßig "offen" ([ ])

set -euo pipefail

FILE="issues.md"
TITLE=""
DESCRIPTION=""

usage() {
  echo "Usage: $0 --title \"Titel\" [--description \"Beschreibung\"] [--file issues.md]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--title)
      TITLE="$2"
      shift 2
      ;;
    -d|--description)
      DESCRIPTION="$2"
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

if [[ -z "$TITLE" ]]; then
  echo "Fehler: --title ist erforderlich."
  usage
fi

if [[ ! -f "$FILE" ]]; then
  echo "Datei $FILE nicht gefunden. Erstelle neue Datei..."
  cat > "$FILE" <<EOC
# Issue Tracking

| ID | Erstellt am | Titel | Beschreibung | Status |
|----|-------------|-------|---------------|----------|
EOC
fi

LAST_ID=$(grep -oE '^\| [0-9]{4} ' "$FILE" | grep -oE '[0-9]{4}' | sort -n | tail -1 || true)

if [[ -z "$LAST_ID" ]]; then
  NEXT_ID="0001"
else
  NEXT_ID=$(printf "%04d" $((10#$LAST_ID + 1)))
fi

DATE=$(date +%Y-%m-%d)

echo "| $NEXT_ID | $DATE | $TITLE | $DESCRIPTION | 🔲 |" >> "$FILE"

echo "Neues Issue $NEXT_ID hinzugefügt ($DATE)."
