# Colour My World

Colour My World is a kid-friendly Android colouring-book game built with Kotlin, Android Studio, and Jetpack Compose. It is offline-first, has no ads, no analytics, no accounts, and uses original generated starter art instead of copyrighted brands or characters.

## What Is Included

- Bright splash/home screen with a cartoon vector art-room background and large game-style play button.
- Category browser with 32 starter colouring pages across animals, dinosaurs, vehicles, space, fantasy, sea life, nature, and cute food.
- Canvas-first colouring screen with tap-to-fill, brush, crayon, marker, glitter, eraser, eyedropper, undo, redo, clear area, clear page, save, and export PNG.
- Mask-constrained colouring so paint stays inside the line-art regions.
- Local progress saving per page.
- Export of final artwork as PNG.
- Asset generator at `tools/fetch_coloring_assets`.
- License manifest at `app/src/main/assets/licenses.json`.
- Splash background source note at `docs/splash/SOURCE.md`.

## Mask-Based Colouring System

Each page lives under `app/src/main/assets/coloring/<category>/<page_id>/` and contains:

- `line.png`: transparent PNG with black outlines.
- `mask.png`: hidden region map. Each fillable region has a unique flat RGB colour. This colour is never shown to the user.
- `thumb.png`: colourful gallery thumbnail.
- `metadata.json`: app-facing page metadata and region summaries.

Rendering order is:

1. White/themed background.
2. User paint bitmap.
3. `line.png` outline layer.
4. Compose UI controls.

When the child taps the canvas, screen coordinates are mapped to original image coordinates. The app reads the pixel in `mask.png`, resolves the internal region id, and fills the cached pixel list for that region on the user paint layer. Brush tools clip every brush sample to the region where the stroke started, so strokes do not bleed into neighboring areas.

## Progress And Export

Progress is saved in app-private storage:

- A PNG paint layer for brush/stroke work.
- A JSON progress record with `pageId`, `regionColours`, `paintLayerPath`, `lastEdited`, and `completed`.

Export composes white background + user paint layer + line art into a final PNG. On Android 10 and newer it writes to `Pictures/Colour My World` through `MediaStore`; older devices use the app external files directory.

## Run The Asset Pipeline

The asset pipeline is local and reproducible. It uses the generated source sheets in `tools/generated_sources/`, then splits them into individual colouring pages:

```powershell
python tools\fetch_coloring_assets
```

It processes 32 original generated line-art pages, writes metadata, writes `app/src/main/assets/licenses.json`, and writes `asset-report.json`. The starter art is generated image content created for this project, not hand-drawn procedural placeholders and not third-party franchise/IP artwork.

## Verify Licenses

Open `app/src/main/assets/licenses.json` and verify every item has:

- title
- source site
- original source link
- author if available
- license
- attribution required yes/no
- local file path
- date downloaded

If you replace generated art with third-party art, keep only public-domain, CC0, or properly licensed assets. If you generate more art, save the source images and prompt/provenance in the repo and update `licenses.json`. Do not use Disney, Pixar, Marvel, Star Wars, Pokemon, Peppa Pig, Paw Patrol, or any copyrighted/trademarked characters, names, logos, or artwork.

## Add A New Colouring Page

1. Create `app/src/main/assets/coloring/<category>/<page_id>/`.
2. Add `line.png`, `mask.png`, `thumb.png`, and `metadata.json`.
3. Make sure `line.png` and `mask.png` have the same pixel dimensions.
4. Ensure every fillable region in `mask.png` uses a unique opaque RGB colour.
5. Ignore tiny/noisy regions when generating masks.
6. Add the asset license entry to `app/src/main/assets/licenses.json`.
7. Rebuild and open the category browser.

## Build Debug APK

PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build Release APK And AAB

Set release signing environment variables:

```powershell
$env:ANDROID_KEYSTORE_PATH='C:\path\to\release.jks'
$env:ANDROID_KEYSTORE_PASSWORD='your-store-password'
$env:ANDROID_KEY_ALIAS='your-key-alias'
$env:ANDROID_KEY_PASSWORD='your-key-password'
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleRelease bundleRelease
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

This checkout also includes local release artifacts under `release/`.

## GitHub Release

After committing and pushing:

```powershell
git tag v1.0.0
git push origin main --tags
```

Upload:

- `release/ColourSproutKids-v1.0.0-release.apk`
- `release/ColourSproutKids-v1.0.0-release.aab`
- `release/ColourSproutKids-v1.0.0-SHA256SUMS.txt`

## Known Limitations

- Pinch-to-zoom and pan are not implemented yet.
- Automatic mask generation is used for generated starter assets; imported or newly generated art may need manual mask cleanup.
- The finished screen confirms the existing exported PNG rather than creating multiple duplicate exports.
- The current local release keystore is for this build only. Replace it with your own secure keystore before store distribution.

## Future Improvements

- Add pinch-to-zoom and pan on the canvas.
- Add richer brush textures.
- Add parent-gated settings.
- Add a manual mask editor for imported art.
- Add optional page packs while keeping all content child-safe and properly licensed.
