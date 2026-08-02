# Album-Art im Android-Miniplayer (Coil-Bridge Ansatz)

## Ziel
Das Album-Art soll in der Media-Notification (System-Miniplayer) sichtbar sein, indem Media3's `BitmapLoader` an den bestehenden `ImageLoader` von Coil delegiert.

## Kontext / Root Cause
- `albumart://` URIs werden von Coil (via `AlbumArtFetcher`) korrekt aufgelöst.
- Media3 nutzt standardmäßig den `DataSourceBitmapLoader`, der `albumart://` nicht kennt.
- Statt die Lese-Logik zu duplizieren, nutzen wir Coil als Brücke für Media3.

## Proposed Changes

### [Service]

#### [NEW] [CoilBitmapLoader.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/service/CoilBitmapLoader.kt)
Implementiert `BitmapLoader` von Media3.
- Nutzt `ImageLoader.execute()` um Bilder zu laden.
- Wandelt `Drawable` (Coil Ergebnis) in `Bitmap` um.
- Nutzt `ResolvableFuture`, um Coroutine-Ergebnisse an Media3 zurückzugeben.

#### [MODIFY] [ServiceModule.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/service/ServiceModule.kt)
- Registriert `CoilBitmapLoader` als Singleton.

#### [MODIFY] [MusicPlayerService.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/service/MusicPlayerService.kt)
- Injiziert den `CoilBitmapLoader`.
- Setzt ihn im `MediaLibrarySession.Builder` mittels `.setBitmapLoader()`.

## Verification Plan

### Automated Tests
- `./gradlew ktlintFormat`
- `./gradlew testDebugUnitTest`

### Manual Verification
- App starten, Song abspielen.
- Prüfen, ob das Album-Cover in der Android-Benachrichtigung und auf dem Lockscreen angezeigt wird.
- Verifizieren, dass sowohl lokale `albumart://` als auch potenzielle HTTP-Cover (falls vorhanden) funktionieren.
