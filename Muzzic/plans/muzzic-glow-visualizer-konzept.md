# Konzept v4: BARS auf LED-Leuchtkraft-Niveau heben + Frost-Look nachschärfen – Muzzic

**Diese Version basiert auf einer Analyse des tatsächlichen Codes** (Branch `feature/muzzic`, Modul `Muzzic/visualization`). Sie ersetzt die früheren Versionen 1–2, die von einer Jetpack-Compose-`Canvas`-Implementierung ausgingen. **Die reale Implementierung ist eine OpenGL-ES-3.0-Shader-Pipeline** und bereits erheblich weiter als angenommen — dieses Dokument beschreibt eine **gezielte Erweiterung**, keinen Neubau.

**v4-Änderungen gegenüber v3 (Code-Review):** `uAlpha`-Erhalt im BARS-`cover`-Modell klargestellt, `cellPx`/SDF-Invariante festgeschrieben, Band-Index-Wiederverwendung dokumentiert, Stärke-Retuning als eigene Phase ergänzt, Reserved-Words-Hinweis und Doku-Pflicht (`docs/04-rendering-gles.md`, `README.md`) aufgenommen.

**Zielgruppe dieses Dokuments:** Eine KI (oder ein Entwickler), die die Änderung im Muzzic-Projekt umsetzt.
**Sprache der Umsetzung:** Kotlin + GLSL ES 3.00 (kein Compose-`Canvas`-Drawing für die eigentliche Visualisierung).

---

## 1. Ausgangslage – was bereits existiert

Modul: `Muzzic/visualization`. Kernkomponenten:

- **`render/shaders/LedFragment.kt`** – Fragment-Shader für den Modus `LED_SPECTRUM`. Implementiert bereits ein vollständiges „Light Kit“: Halo (`uGlow`, zweistufig: enger Rim + weite Aura), weißglühender Kern mit gesättigtem Ring (`uHotCore`), wanderndes Dome-Glanzlicht (`uSpecular`), Streulicht über der Signalspitze plus Spaltenschimmer (`uBleed`), Helligkeits­abstufung von gedimmter Basis zu hellem Top (`uGlowGrade`), weiches Erlöschen der Frontier (`uFade`), Nachleucht-Geister aus einem Trail-Speicher (`uTrail`/`uTrailStrength`), Shimmer, sowie ein sehr dezenter Off-State-Rim.
- **`render/shaders/BarsFragment.kt`** – Fragment-Shader für den Modus `BARS` (mirrored/gefaltete Balken, Bass in der Mitte, Balken wachsen bidirektional von der Mittellinie nach außen). Implementiert nur: Halo (`uGlow`), Shimmer, ein amplitudengesteuerter weißer Farbmix und einen weißen Tip-Highlight (`uTipGlow`). **Kein HotCore, Specular, Bleed, GlowGrade, Fade oder Trail.**
- **`render/LedBarRenderer.kt`** – kompiliert beide Programme, hält alle Uniform-Locations, füttert pro Frame `packedBands`/`packedPeaks`/`packedTrail` (aus `LedBarSmoother`, für **beide** Stile berechnet) sowie statische Uniforms (Farben, Geometrie, Effekt-Stärken) aus `VisualizerConfig`/`VisualizerTheme`.
- **`render/LedBarSmoother.kt`** – berechnet Attack/Decay, Peak-Hold und Trail-Speicher **unabhängig vom Render-Stil**; die Daten für Peak und Trail liegen also für BARS bereits vor, werden aber vom `barsProgram` nicht abgefragt.
- **`VisualizerConfig.kt`** – deklariert `hotCoreStrength`, `specularStrength`, `bleedStrength`, `glowGradeStrength`, `fadeStrength`, `trailStrength` bereits **generisch für beide Stile** mit sinnvollen Defaults (0.85 / 0.25 / 0.8 / 0.65 / 1.0 / 0.7). `VisualizerFactory.resolve()` für `BARS` überschreibt nur `segmentCount`, `shimmerStrength`, `tipGlowStrength` – die übrigen Werte sind also bereits „scharf“, werden nur nicht konsumiert.
- **`VisualizerTheme.kt`** – `barsThemeFrom(color)` setzt `background = Transparent` und **`offIntensity = 0f`** (unlit-Segmente vollständig unsichtbar; testgesichert in `BarsThemeTest`). `ledThemeFrom(color)` setzt `offIntensity` nicht explizit → Default `0.06f` (kaum sichtbar).
- **`component/MusicVisualization.kt`** – Einstiegs-Composable; **Default-Engine ist `VisualizerEngine.BARS`**.
- Tests: `ShaderUniformTest.kt` (Golden-List `ledUniforms`, prüft, dass der LED-Shader jedes vom Renderer abgefragte Uniform deklariert – **noch keine äquivalente Liste für BARS**), `BarsThemeTest.kt` (pinnt `offIntensity ≈ 0` für BARS), `ConfigDefaultsTest.kt`, `MirroredBarsTest.kt`, `LedThemeTest.kt`, `SpectrumProcessorTest.kt`, `AutoGainTest.kt` u. a.

## 2. Diagnose

„Fad und plump“ betrifft mit hoher Wahrscheinlichkeit primär **BARS**, weil:

1. BARS ist der **Default-Modus** der App.
2. BARS besitzt nur einen Bruchteil des Licht-Modells, das LED bereits hat.
3. Bei BARS sind unbeleuchtete Segmente **komplett unsichtbar** (transparent) statt als „Material“ erkennbar – im Referenzfoto sind die unbeleuchteten Acrylplatten dagegen klar als helles Milchglas sichtbar.
4. Auch LED zeigt mit `offIntensity = 0.06` kaum erkennbare unbeleuchtete Struktur – für den im Referenzfoto gewünschten Frost-Look ebenfalls zu dunkel.

**Konsequenz für dieses Konzept:** kein Neubau, sondern (a) Portierung des bestehenden LED-Licht-Modells auf BARS, angepasst an dessen gespiegelte/gefaltete Geometrie, und (b) Nachschärfen des Off-State-Looks in beiden Shadern.

## 3. Ziel

1. BARS erreicht die gleiche Leuchtkraft/Materialität wie LED: HotCore, Specular, Bleed, GlowGrade, Fade, Trail — adaptiert an die bidirektional gefaltete Balkengeometrie.
2. In beiden Modi sind unbeleuchtete Segmente als helles, mattiertes „Glas“ erkennbar (Referenzfoto), nicht als Schwarz/Transparenz.
3. Bestehende Funktionalität (Spiegelung, Farbverlauf Center→Edge, Band-Mapping, Auto-Gain, Smoothing) bleibt unverändert; nur der Shader-Look und die Renderer-Uniform-Verdrahtung werden erweitert.

## 4. Nicht-Ziele

- Keine Änderung an `SpectrumProcessor`, `Fft`, `AutoGain`, `BandMapper`, `LedBarSmoother`-Berechnungslogik, `SpectrumBus`.
- Keine Änderung an `mirroredBandIndex`/Spiegel-Mapping.
- Keine Änderung am ProjectM-Pfad (`VisualizerEngine.PROJECT_M`).
- Keine Rückkehr zu Compose-`Canvas`-Rendering für die eigentliche Visualisierung (die `StaticBarsPreview` in `MusicVisualization.kt` ist nur ein Preview-Mock ohne GL-Kontext und bleibt wie sie ist, sollte aber optional dieselben `offIntensity`-Werte widerspiegeln, siehe 8.4).

## 5. Vorgehen für die umsetzende KI

1. `Muzzic/AGENTS.md` lesen (Kotlin-/KDoc-Konventionen, **Doku-Pflicht**: `docs/04-rendering-gles.md` + `README.md`-Strukturdiagramm im selben Change nachziehen).
2. Diese Dateien im Detail lesen, bevor Änderungen beginnen: `LedFragment.kt`, `BarsFragment.kt`, `ShaderSnippets.kt`, `LedBarRenderer.kt`, `VisualizerConfig.kt`, `VisualizerTheme.kt`, `ShaderUniformTest.kt`, `BarsThemeTest.kt`, `ShaderReservedWordsTest.kt`.
3. Abschnitt 6 umsetzen (Shader-Erweiterung), dann Abschnitt 7 (Renderer-Verdrahtung), dann Abschnitt 8 (Frost/Off-State).
4. Bestehende Tests laufen lassen; die in Abschnitt 9 gelisteten Tests bewusst aktualisieren/ergänzen (keine stillschweigenden Änderungen an golden-value-Assertions ohne Kommentar/Begründung im Diff).
5. Akzeptanzkriterien (Abschnitt 10) prüfen (inkl. Stärke-Retuning am Gerät, Abschnitt 10a).

## 6. BarsFragment.kt erweitern

Ziel: das komplette LED-Licht-Modell auf die BARS-Geometrie übertragen. Die BARS-Geometrie unterscheidet sich von LED wie folgt und die Portierung muss das berücksichtigen:

| LED (`LedFragment.kt`) | BARS (`BarsFragment.kt`) | Übertragung |
|---|---|---|
| `sy = uv.y * uSegments`, wächst von unten (0) nach oben | `sy = abs(uv.y - 0.5) * 2.0 * halfSegs`, wächst von der Mitte (0) nach außen (`halfSegs`) | `sy`/`halfSegs` statt `sy`/`uSegments` als Bezugsgröße verwenden |
| `t = (seg+0.5)/uSegments` (0=unten,1=oben; steuert Zonenfarbe + Grading) | `t = (seg+0.5)/halfSegs` (0=Mitte,1=Rand; steuert bereits den Base→Target-Gradienten) | vorhandenes `t` wiederverwenden, keine Neuberechnung |
| `front = value*uSegments`, `hasSignal = step(0.5/uSegments, value)` | analog mit `halfSegs`: `front = value*halfSegs`, `hasSignal = step(0.5/halfSegs, value)` | direkt übertragbar |
| `litHard/litSoft/lit` über `uFade` gemischt | aktuell nur `lit = step(t, value)` | `litSoft`/`mix(...,uFade)` ergänzen, exakt wie LED |
| `peakSg`/`isPeak` aus `uPeaks` | aktuell kein Peak-Array verdrahtet; `isTip` nur aus der aktuellen Lautstärke abgeleitet (kein Hold/Decay) | `uPeaks`-Array ergänzen (siehe 7), `isPeak` analog zu LED bauen; ersetzt/ergänzt `isTip` um eine **echte** Peak-Hold-Spitze, die kurz stehen bleibt und langsam abfällt (bereits von `LedBarSmoother` berechnet) |
| `zone` aus `uColLow/uColMid/uColHigh` + `uZoneStart` | `grad = mix(uColBase, uColTarget, t)` | als „zone“-Äquivalent für HotCore/Specular/Bleed/Halo verwenden |
| `grade = mix(0.15,1.0,pow(t,2.2))`, `energy = 0.5+0.5*value`, `glowScale` | nicht vorhanden | 1:1 übernehmen (mit BARS-`t`, BARS-`value`) |
| HotCore: skalierte SDF-Kopie (`qCore`/`sdCore`/`core`/`ring`), White-Mix auf `litCol` | nicht vorhanden | 1:1 übernehmen, `litCol` = aktuelles `onCol` |
| Specular: `sweep`/`spec`, additiv auf `litCol` | nicht vorhanden | 1:1 übernehmen |
| Halo: `haloCol`, `halo`, `aura` (Rim + weite Aura) | nur einfacher `halo`-Term ohne Aura, `haloCol` ohne Overbright-Rim | auf das LED-Zweistufen-Modell (Rim + Aura, `haloCol = mix(grad*1.6, grad, ...)`) heben |
| Bleed: `above`, `bleedBand`, `colMask`, `spill`, `wash` | nicht vorhanden | 1:1 übernehmen; `colMask` bezieht sich auf die **halbe** Spalte (von der Mittellinie zum Rand), `above = sy - front` bleibt unverändert korrekt |
| Off-Rim: `rim`/`rimCol` | nicht vorhanden (BARS hat aktuell gar keine Off-State-Darstellung außer `uOffIntensity`-Grundhelligkeit) | 1:1 übernehmen |
| Trail/Ghost: `trailFront`, `lag`, `ghost`, `ghostCol` | nicht vorhanden | 1:1 übernehmen; benötigt `uTrail`-Array (siehe 7); Band-Index `band` (aus gespiegeltem `m`) wird für `uBands`/`uPeaks`/`uTrail` identisch wiederverwendet (`uX[band >> 2][band & 3]`) |
| `cover`/`fragColor`-Alpha-Komposition | einfacher (`cover = clamp(led+halo,...)`, `vis`-Gate, `uAlpha`-Multiplikator) | auf das LED-Modell heben (`cover` folgt allen Licht-Termen), aber **`uAlpha` und `vis`-Gate zwingend erhalten**: `fragColor = vec4(mix(uBackground, c, cover), cover * uAlpha * vis)` mit `vis = max(on, step(0.001, uOffIntensity))`. LED kennt kein `uAlpha` — hier nichts von LED übernehmen, BARS-Semantik (Alpha aus `theme.colLow.alpha`) bleibt maßgeblich |

**Invarianten beim Port (nicht verhandelbar):**

- `cellPx = uRes / vec2(float(uColumns), uSegments)` und damit der gesamte `ROUNDED_BOX_SDF`-Block bleiben auf dem **vollen** `uSegments` (BARS: 16). Nur `front`/`hasSignal`/`litSoft`/`t`-bezogene Helligkeitslogik rechnet mit `halfSegs` (BARS: 8). Die LED-Größe/Geometrie darf sich durch den Port nicht ändern.
- Alle neuen Terme arbeiten auf gefaltetem `sy`/`seg`/`t` — kein zusätzliches Spiegeln (siehe Hinweis unten).

**Wichtig – zwei Hälften pro Spalte:** BARS faltet den Balken symmetrisch um die Mittellinie (`abs(uv.y-0.5)`). Alle neuen Terme (HotCore, Specular, Bleed, Trail, Ghost) müssen auf Basis von `sy`/`seg`/`t` arbeiten, die bereits gefaltet sind – **kein** zusätzliches Spiegeln nötig, da `sy` schon relativ zur Mitte läuft. Die Sweep-Formel für `uSpecular` sollte `cell.y` weiterhin aus dem gefalteten `sy` ableiten (`fract(sy)`), nicht aus `uv.y` direkt, sonst bricht die Spiegelsymmetrie sichtbar auf.

**Neue Uniforms** (Delta-Liste — `uRes`, `uColumns`, `uBandCount`, `uSegments`, `uBands`, `uColBase`, `uColTarget`, `uShimmer`, `uTipGlow`, `uTime`, `uAlpha`, `uLedHalfSize`, `uCornerRadius`, `uOffIntensity`, `uBackground`, `uGlow` existieren bereits; Namen identisch zu LED, damit `LedBarRenderer` beide Programme mit derselben Konfiguration füttern kann):

```
uniform vec4  uPeaks[16];       // gepackte Peak-Hold-Werte 0..1
uniform vec4  uTrail[16];       // gepackte Trail-Speicher-Werte 0..1
uniform float uHotCore;
uniform float uSpecular;
uniform float uBleed;
uniform float uGlowGrade;
uniform float uFade;
uniform float uTrailStrength;
```

`uTipGlow` bleibt erhalten (bestehender, BARS-spezifischer Parameter) und wird **zusätzlich** zum HotCore/Peak-Highlight angewendet, nicht ersetzt — beide Effekte überlappen sich am selben Segment und verstärken sich (Tip-Glow als schneller amplitudengetriebener Mix, HotCore/Peak als strukturelles Leuchten).

**Kein `#include`** in GLSL ES: Wiederverwendbare Formeln (z. B. der HotCore-/Bleed-/Trail-Block) sollten wie `ROUNDED_BOX_SDF` als weiteres Template-Snippet in `ShaderSnippets.kt` abgelegt werden (z. B. `LIGHT_KIT_COMMON`), damit `LedFragment.kt` und `BarsFragment.kt` denselben Kotlin-String für den identischen GLSL-Block einbinden, statt ihn zu duplizieren. Variablennamen im Snippet müssen mit beiden Shadern kompatibel sein (`zone`/`grad` vereinheitlichen, z. B. beide auf `vec3 zone` umbenennen, damit das Snippet in beiden Shadern unverändert funktioniert).

**Reserved Words:** Neuer GLSL-Code darf keine Bezeichner aus GLSL ES 3.00 §3.6 verwenden (`ShaderReservedWordsTest.kt` prüft per `\b`-Regex — `halfSegs`/`halfCols` sind unkritisch, aber `half`, `sample`, `filter`, `input`, `output` als Variablennamen sind verboten). `spill`/`wash`/`ghost`/`core`/`ring` aus LED sind unkritisch und wiederverwendbar.

## 7. LedBarRenderer.kt erweitern

1. Neue Uniform-Location-Felder für `barsProgram` ergänzen (Präfix `b`, analog zu den bestehenden `bXxxLoc`-Feldern): `bPeaksLoc`, `bTrailLoc`, `bTrailStrengthLoc`, `bHotCoreLoc`, `bSpecularLoc`, `bBleedLoc`, `bGlowGradeLoc`, `bFadeLoc`.
2. In `onSurfaceCreated()`, im `barsProgram`-Zweig, `glGetUniformLocation` für jede neue Uniform ergänzen.
3. In `applyStaticUniformsIfNeeded()`, im `barsProgram`-Block, die neuen Stärken aus `config` setzen (`config.hotCoreStrength`, `config.specularStrength`, `config.bleedStrength`, `config.glowGradeStrength`, `config.fadeStrength`, `config.trailStrength`) — dieselben Felder, die LED bereits liest; keine neuen `VisualizerConfig`-Felder nötig.
4. In `drawMirroredBars()`, die Per-Frame-Uniforms um `packedPeaks` und `packedTrail` ergänzen (`GLES30.glUniform4fv(bPeaksLoc, vec4Count, packedPeaks, 0)`, entsprechend für `bTrailLoc`) — diese Arrays werden bereits pro Frame befüllt (`onDrawFrame()`), nur der Bars-Zweig liest sie aktuell nicht.
5. `vec4Count` wird in `onDrawFrame()` bereits berechnet und an `drawLed()` übergeben; für `drawMirroredBars()` intern denselben Wert berechnen (aktuell tut sie das schon für `uBandsLoc`, nur ergänzen für die zwei neuen Arrays).
6. Keine neue Dirty-Logik nötig: `staticUniformsDirty` wird bereits von den `config`/`theme`/`style`-Settern gesetzt und von `applyStaticUniformsIfNeeded()` auf dem GL-Thread übernommen — neue statische Uniforms sind automatisch abgedeckt.

## 8. Frost-/Off-State-Look (beide Modi)

Ziel: unbeleuchtete Segmente sollen wie mattiertes Acrylglas wirken (Referenzfoto), nicht wie Schwarz oder komplette Transparenz.

### 8.1 `offIntensity` anheben

- `VisualizerTheme.barsThemeFrom`: `offIntensity` von `0f` auf einen sichtbaren, aber dezenten Wert anheben (Richtwert **0.14f** — Feintuning am Gerät). Hintergrund bleibt `Color.Transparent`.
- `VisualizerTheme.ledThemeFrom`: `offIntensity` explizit setzen statt den globalen Default zu erben (Richtwert **0.14f**, statt bisher implizit `0.06f`).
- Der globale Default in der `VisualizerTheme`-Datenklasse (aktuell `0.06f`) kann unverändert bleiben (wirkt nur für `ClassicGreen`/`Amber`/`IceBlue`, die als feste Presets vermutlich bewusst dunkler gehalten sind — falls diese ebenfalls angepasst werden sollen, das explizit mit dem Nutzer klären statt anzunehmen).

### 8.2 Off-State-Kantenlicht verstärken

Der bestehende `rim`-Term in `LedFragment.kt` (`rimCol = zone * rim * 0.10`) ist sehr dezent. Für die „milchige Platte mit heller Oberkante“ aus dem Referenzfoto:

- Faktor `0.10` auf einen konfigurierbaren oder testweise höheren Wert wie `0.22` anheben, **oder**
- einen zweiten, gerichteten Term ergänzen, der speziell die obere Kante jedes Segments (nicht den ganzen Rand) leicht aufhellt — analog zum `edgeHighlight`-Gedanken aus der Bildanalyse (Abschnitt 8.3 nennt die Umsetzung als Option).

**Vorsicht bei `uGlow = 0`:** Die KDoc von `LedFragment` verspricht „`uGlow = 0` reproduziert exakt das Pre-Glow-Verhalten“. Der `rim`-Term hängt nicht von `uGlow` ab, ändert sich also nicht — das bleibt korrekt. Wird der Rim-Faktor erhöht, ändert sich das Pre-Glow-Verhalten (`uGlow=0`) mit, was von der KDoc-Aussage nicht mehr exakt gedeckt ist. Das ist hier gewünscht (Frost soll unabhängig vom Glow sichtbar sein), aber die betroffene KDoc-Zeile in `LedFragment.kt` entsprechend nachziehen, damit Doku und Verhalten übereinstimmen.

### 8.3 Optionaler, expliziter Frost-Term (statt nur den Rim zu verstärken)

Sauberer als das Hochdrehen des bestehenden Rim-Faktors: ein eigener, klar benannter `uFrost`-Term mit eigenem `VisualizerConfig`-Feld (`frostStrength`, Default z. B. `0.16f`), der unabhängig vom Glow und vom `on`-Zustand eine helle Kante an der **Oberkante jedes Segments** zeichnet (oben = weiter von der Mittellinie/Basis entfernt liegende Kante, je nach Modus). Vorteil: sauber konfigurierbar, ohne bestehende Glow-Semantik zu überladen. Nachteil: ein weiteres Uniform in beiden Shadern plus Config-Feld plus Tests. Empfehlung: **8.1 + moderates Anheben von 8.2** zuerst umsetzen und optisch prüfen; 8.3 nur nachziehen, falls der Materialeindruck damit noch nicht ausreicht.

### 8.4 Preview-Mock

`StaticBarsPreview` in `MusicVisualization.kt` sollte weiterhin dieselbe `theme.offIntensity` aus dem übergebenen Theme lesen (tut sie bereits: `if (!isLit && theme.offIntensity <= 0.001f) continue` und `finalColor = zoneColor.copy(alpha = theme.offIntensity)`). Mit den obigen Änderungen zeigt der Compose-Preview die neue Off-State-Helligkeit automatisch korrekt an, ohne Code-Änderung in dieser Datei.

## 9. Erforderliche Test- und Doku-Anpassungen

- **`ShaderUniformTest.kt`**: eine `barsUniforms`-Liste (analog zu `ledUniforms`) ergänzen, die alle vom `barsProgram`-Zweig in `LedBarRenderer` abgefragten Uniforms auflistet, plus einen Test `` `bars program declares every uniform the renderer queries` ``, analog zum bestehenden LED-Test. Das schließt die Lücke, dass BARS bisher nicht gegen dieses Sicherheitsnetz geprüft wird.
- **`ShaderReservedWordsTest.kt`**: läuft unverändert mit (neue `LIGHT_KIT_COMMON`-Snippet-Blöcke automatisch abgedeckt, da der Test die finalen `SOURCE`-Strings prüft). Bei Snippet-Einführung zusätzlich prüfen, dass beide finalen `SOURCE`-Strings weiterhin den SDF-Block enthalten.
- **`BarsThemeTest.kt`**: den Test `` `bars theme keeps base color and transparent background` `` anpassen — `offIntensity` wird nun bewusst auf den neuen Zielwert (z. B. `0.14f`) statt auf `0f` geprüft. Kommentar im Diff ergänzen, warum sich der Erwartungswert ändert (Verweis auf dieses Konzept/den Kontext „Frost-Look“).
- **`ConfigDefaultsTest.kt`**: keine Änderung nötig, sofern keine neuen `VisualizerConfig`-Felder eingeführt werden (Abschnitt 6/7 nutzt ausschließlich bestehende Felder). Nur falls Option 8.3 (`frostStrength`) umgesetzt wird, hier den neuen Default ergänzen.
- Neuer Test (empfohlen, analog zu `MirroredBarsTest.kt`): dass die gepackten `peaks`/`trail`-Arrays für BARS dieselben Werte enthalten wie für LED bei identischem `SpectrumBus`-Input (Renderer-Ebene, ggf. als Robolectric-/Instrumentation-Test, falls der bestehende Testrahmen GLES-Aufrufe dort schon mockt — sonst als Kotlin-Unit-Test auf `LedBarSmoother`-Ebene, der lediglich dokumentiert, dass Peak/Trail unabhängig vom Stil berechnet werden; das ist bereits so, ein Test macht es nur explizit).
- **Doku (Pflicht pro `AGENTS.md`):** `visualization/docs/04-rendering-gles.md` (Kap. 4.4/4.6: neue BARS-Uniforms, `cover`-Formel mit `uAlpha`, `LIGHT_KIT_COMMON`-Snippet falls eingeführt) und `visualization/README.md` (Komponentenliste, falls Snippet-Datei dazukommt) im selben Change nachziehen.

## 10. Akzeptanzkriterien

- [ ] BARS zeigt (nach Retuning, Abschnitt 10a) sichtbar denselben Leuchtcharakter wie LED: Halo mit Rim+Aura, weißglühender Kern, Streulicht über der Signalspitze, Helligkeitsabstufung von Mitte zu Rand, weiches Erlöschen, Nachleucht-Geister.
- [ ] BARS zeigt eine echte Peak-Hold-Spitze (aus `smoother.peaks`), die kurz stehen bleibt und dann abfällt — nicht mehr nur den amplitudengetriebenen Tip-Mix.
- [ ] Unbeleuchtete Segmente sind in BARS und LED als helles, mattiertes Material erkennbar statt schwarz/unsichtbar.
- [ ] `uGlow = 0`, `uHotCore = 0`, `uSpecular = 0`, `uBleed = 0`, `uGlowGrade = 0`, `uFade = 0`, `uTrailStrength = 0` deaktivieren im BARS-Shader jeweils exakt den zugehörigen Effekt, ohne andere Terme zu beeinflussen (Analogie zur LED-KDoc-Garantie).
- [ ] BARS-Transparenzsemantik erhalten: `cover * uAlpha * vis` — `uAlpha` aus `theme.colLow.alpha`, `vis`-Gate aus `uOffIntensity`. Keine undurchsichtigen Flächen, wo vorher Transparenz war.
- [ ] Bestehende Funktionalität unverändert: Spiegel-Mapping (`MirroredBarsTest`), Band-Zuordnung, Auto-Gain, Farbverlauf Center→Edge, Shimmer/TipGlow-Toggle über `BarsParams`; LED-Geometrie (`cellPx`) unverändert.
- [ ] `ShaderUniformTest`, `BarsThemeTest`, `ConfigDefaultsTest`, `MirroredBarsTest`, `LedThemeTest` und alle übrigen bestehenden Tests sind grün (inkl. der in Abschnitt 9 beschriebenen bewussten Anpassungen).
- [ ] Keine Duplicate-Uniform-Fehler (von `ShaderUniformTest` automatisch geprüft).
- [ ] Beide Shader kompilieren und linken auf einem echten Gerät/Emulator mit GLES 3.0 (`GlUtil.createProgram` liefert `!= 0`).
- [ ] Performance: keine spürbaren Frame-Drops gegenüber dem aktuellen Stand bei typischer Bandanzahl (32) und Spaltenanzahl (64) — die neuen Terme sind reine Fragment-Shader-Arithmetik ohne zusätzliche Draw-Calls, sollten also unkritisch sein; trotzdem auf einem Mittelklassegerät gegenprüfen.
- [ ] `AGENTS.md`-Konventionen eingehalten (KDoc auf allen neuen public/internal Deklarationen, keine unbegründeten Magic Numbers außerhalb der bereits etablierten Shader-Mathematik-Konventionen).
- [ ] Betroffene Docs aktualisiert (`docs/04-rendering-gles.md`, `README.md`).

### 10a. Stärke-Retuning (eigene Phase nach dem Port)

Die LED-Defaults (`hotCore 0.85`, `specular 0.25`, `bleed 0.8`, `glowGrade 0.65`, `glow 0.8`, `trail 0.7`, `fade 1.0`) sind Startwerte, keine Zielwerte: Auf 64×16-Zellen (kleiner als LED-Türme) wirken gleiche Zahlen stärker/dichter. Vorgehen: Port mit 1:1-Werten landen → Screenshot gegen Referenzfoto + LED-Nebeneinander → pro Stärke einzeln nachziehen (`VisualizerConfig.copy(...)` im Test-Setup, keine Config-Umbenennung) → finale Werte als neue BARS-Betriebspunkte in `VisualizerFactory.resolve()` hinterlegen **oder** bewusst bei den Shared-Defaults bleiben (dann im Diff begründen). BARS ist Default-Engine — Änderung trifft alle Nutzer sofort, daher kein stilles Übernehmen ohne Sichtprüfung.

## 11. Verifikation

1. **Optisch:** App auf Gerät/Emulator mit Musik laufen lassen, BARS und LED nebeneinander mit dem Referenzfoto vergleichen. Insbesondere: Ist die Off-State-Struktur jetzt erkennbar? Wirkt BARS jetzt genauso „leuchtend“ wie LED?
2. **Parametertest:** Jede neue Stärke (`hotCoreStrength` etc.) einzeln auf `0f` setzen und bestätigen, dass genau der erwartete Effekt verschwindet (visuell, ggf. unterstützt durch die KDoc-Garantien).
3. **Regression:** Modus-Umschaltung BARS↔LED↔ProjectM, Rotation, Pause/Play, App-Wechsel; kein Flackern, kein Absturz bei `bandCount`-Änderung.
4. **Unit-/Shader-Tests:** `./gradlew :visualization:test` (oder passendes Gradle-Target) grün.

## 12. Offene Punkte für die umsetzende KI

- Exakte Zielwerte für `offIntensity` (8.1) und Rim-Faktor (8.2) sind Richtwerte aus der Bildanalyse — finales Feintuning am Gerät, idealerweise mit Screenshot-Vergleich zum Referenzfoto.
- Ob Option 8.3 (eigener `uFrost`-Term) nötig ist oder 8.1+8.2 genügen, sollte erst nach visueller Prüfung von 8.1+8.2 entschieden werden.
- Ob die festen Presets `ClassicGreen`/`Amber`/`IceBlue` (aktuell `offIntensity`-Default `0.06f`) ebenfalls angehoben werden sollen, ist nicht Teil dieses Konzepts und sollte separat abgestimmt werden, falls gewünscht.
- Falls `ShaderSnippets.LIGHT_KIT_COMMON` (Abschnitt 6) eingeführt wird: prüfen, ob sich `zone`/`grad`, `p`/`halfSizePx`/`r` (aus `ROUNDED_BOX_SDF`) und `value`/`t`/`sy` in beiden Shadern konsistent genug benennen lassen, um das Snippet unverändert zu teilen — falls nicht, ist eine bewusste Duplizierung mit Kommentar-Verweis aufeinander die pragmatischere Wahl.
