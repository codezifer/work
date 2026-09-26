#!/usr/bin/env bash
# Baut visualization/docs/*.md (inkl. Mermaid-Diagrammen) zu einem PDF.
# Toolchain A: pandoc + mermaid-cli (via npx) + xelatex.
#
#   cd visualization/docs && ./generate_pdf.sh [-o datei.pdf] [--no-mermaid]
set -euo pipefail

DOCS_DIR="$(cd "$(dirname "$0")" && pwd)"
GEN_DIR="$DOCS_DIR/.gen"
OUT="$DOCS_DIR/visualization-doku.pdf"
RENDER_MERMAID=1

while [ $# -gt 0 ]; do
    case "$1" in
        -o) OUT="$2"; shift 2 ;;
        --no-mermaid) RENDER_MERMAID=0; shift ;;
        -h|--help)
            echo "Usage: generate_pdf.sh [-o datei.pdf] [--no-mermaid]"
            exit 0 ;;
        *) echo "Unbekannte Option: $1" >&2; exit 1 ;;
    esac
done

CHAPTERS="00-ueberblick.md 01-architektur.md 02-audio-pipeline.md 03-spectrum-bus.md 04-rendering-gles.md 05-projectm-nativ.md 06-konfiguration.md 07-build-test.md 08-glossar.md"

for c in $CHAPTERS; do
    [ -f "$DOCS_DIR/$c" ] || { echo "Fehlt: $c" >&2; exit 1; }
done
command -v pandoc >/dev/null || { echo "pandoc fehlt (apt install pandoc)" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 fehlt" >&2; exit 1; }

rm -rf "$GEN_DIR"
mkdir -p "$GEN_DIR/assets"

# 1. Mermaid-Bloecke extrahieren -> .mmd, Referenzen auf .png umschreiben.
python3 - "$DOCS_DIR" "$GEN_DIR" "$RENDER_MERMAID" $CHAPTERS <<'EOF'
import re, sys

docs, gen, render = sys.argv[1], sys.argv[2], sys.argv[3] == "1"
chapters = sys.argv[4:]
counter = [0]

for ch in chapters:
    with open(f"{docs}/{ch}") as f:
        text = f.read()
    # Kapitel abschnittsweise verarbeiten: Die letzte Ueberschrift vor jedem
    # Diagramm wird als sprechender Bild-Alt-Text uebernommen.
    parts = re.split(r"(```mermaid\n.*?```)", text, flags=re.S)
    out = []
    heading = ""
    for p in parts:
        if p.startswith("```mermaid"):
            counter[0] += 1
            code = p[len("```mermaid\n"):-len("```")]
            if not render:
                out.append("```text\n" + code + "```")
                continue
            stem = f"mermaid-{counter[0]:02d}"
            with open(f"{gen}/assets/{stem}.mmd", "w") as f:
                f.write(code)
            alt = f"Diagramm {counter[0]}" + (f" ({heading})" if heading else "")
            out.append(f"![{alt}](assets/{stem}.png)")
        else:
            hm = re.findall(r"^#{1,4}\s+(.+)$", p, flags=re.M)
            if hm:
                heading = re.sub(r"[*_`]", "", hm[-1]).strip()[:80]
            out.append(p)
    with open(f"{gen}/{ch}", "w") as f:
        f.write("".join(out))
print(f"mermaid blocks: {counter[0]}")
EOF

# 2. Mermaid-Bloecke rendern (npx zieht mermaid-cli beim ersten Lauf).
if [ "$RENDER_MERMAID" -eq 1 ]; then
    command -v npx >/dev/null || { echo "npx/node fehlt" >&2; exit 1; }
    if [ -z "${PUPPETEER_EXECUTABLE_PATH:-}" ] && command -v chromium >/dev/null; then
        export PUPPETEER_EXECUTABLE_PATH="$(command -v chromium)"
    fi
    for mmd in "$GEN_DIR"/assets/*.mmd; do
        [ -e "$mmd" ] || break
        png="${mmd%.mmd}.png"
        echo "render: $(basename "$mmd")"
        npx -y @mermaid-js/mermaid-cli@12 -i "$mmd" -o "$png" -b white -s 2
    done
fi

# 3. PDF via pandoc + xelatex bauen.
# WICHTIG: im .gen-Verzeichnis arbeiten — dort sind die Mermaid-Bloecke
# bereits durch gerenderte PNG-Bilder ersetzt. Ein relativer -o-Pfad wird
# zuvor gegen docs/ aufgeloest, damit die Ausgabe am erwarteten Ort landet.
case "$OUT" in
    /*) ;;
    *) OUT="$DOCS_DIR/$OUT" ;;
esac
(
cd "$GEN_DIR"
# shellcheck disable=SC2086
pandoc $CHAPTERS \
    --from=gfm --pdf-engine=xelatex --toc --toc-depth=3 \
    -V lang=de-DE -V geometry:margin=2.5cm \
    -V fontsize=11pt -V colorlinks=true \
    -V mainfont="DejaVu Serif" -V sansfont="DejaVu Sans" -V monofont="DejaVu Sans Mono" \
    -M title="Muzzic — Visualisierungsmodul (:visualization)" \
    -M subtitle="Architektur- und Klassendokumentation" \
    -M date="$(date +%Y-%m-%d)" \
    --highlight-style=tango \
    --lua-filter="$DOCS_DIR/table-colwidth.lua" \
    -V header-includes="\setkeys{Gin}{width=\linewidth,height=0.85\textheight,keepaspectratio}" \
    -o "$OUT" \
    --resource-path="$GEN_DIR"
)
echo "OK: $OUT"
