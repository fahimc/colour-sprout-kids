# Play Store Submission Guide

This folder contains the Play Store-ready handoff for Colour My World `v1.0.19`.

## Upload artifact

Upload this file to Play Console:

`ColourMyWorld-v1.0.19-playstore.aab`

Do not upload the APK for Play Store production. The APK is useful for direct device testing only. Google Play expects the Android App Bundle for modern releases.

## Current app identifiers

- App name: `Colour My World`
- Developer/game maker: `Remetheia`
- Package name: `com.coloursprout.kids`
- Version name: `1.0.19`
- Version code: `20`
- Target SDK: `35`
- Min SDK: `26`
- Price: Free
- Suggested category: `Game > Educational`
- Contains ads: No
- In-app purchases: No
- Requires login: No

Important: Once the first AAB is uploaded, the package name and signing lineage are effectively locked for this Play app. Confirm `com.coloursprout.kids` and the upload key are the ones you want before first submission.

## Before opening Play Console

1. Keep the upload keystore safe. The local release key is not committed to git.
2. Host the privacy policy from `privacy-policy-draft.md` and copy the public URL.
3. Prepare a support email for Play users.
4. Prepare at least two phone screenshots.
5. Prepare a 1024 x 500 feature graphic.
6. Prepare a 512 x 512 high-res icon if Play Console requests one.
7. Review `app/src/main/assets/licenses.json` before submission so the artwork/license manifest is accurate.

## Play Console steps

1. Open Play Console.
2. Create a new app.
3. Enter app name: `Colour My World`.
4. Choose `Game`.
5. Choose `Free`.
6. Add the support email.
7. Accept the developer policy/export declarations and Play App Signing terms.
8. Complete App content:
   - Privacy policy URL: paste the hosted privacy policy URL.
   - Ads: No.
   - App access: all functionality is available without login.
   - Target audience: choose the real intended age groups. If targeting children, complete Families declarations.
   - Data safety: use `DATA_SAFETY_AND_FAMILIES.md` as the current build guide.
   - Content rating: answer from the current app behavior; see the content rating notes in `DATA_SAFETY_AND_FAMILIES.md`.
9. Complete Store listing:
   - Use copy from `STORE_LISTING.md`.
   - Upload screenshots, feature graphic, and high-res icon.
10. Go to Release > Testing > Internal testing first.
11. Create a new release.
12. Upload `ColourMyWorld-v1.0.19-playstore.aab`.
13. Paste release notes from `release-notes.txt`.
14. Save, review, and roll out to internal testing.
15. Install from the internal testing link and test:
   - Splash navigation
   - Android system back navigation
   - Category browsing
   - Fill tool
   - Brush/crayon/eraser
   - Pinch zoom
   - Save to gallery
   - Export PNG
   - Portrait and landscape
16. After internal testing passes, promote the same release to production or create a production release using the same AAB.

## Updating after submission

For every future Play upload:

1. Increase `versionCode` in `app/build.gradle.kts`.
2. Usually increase `versionName`.
3. Build a new signed AAB.
4. Upload the new AAB to Play Console.
5. Update release notes and the Data safety form if behavior changed.

## Local rebuild commands

PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_KEYSTORE_PATH=(Resolve-Path '.signing\colour-sprout-release.jks')
$env:ANDROID_KEYSTORE_PASSWORD='<keystore password>'
$env:ANDROID_KEY_ALIAS='<key alias>'
$env:ANDROID_KEY_PASSWORD='<key password>'
.\gradlew.bat clean lintDebug testDebugUnitTest bundleRelease
```

The signed bundle is created at:

`app/build/outputs/bundle/release/app-release.aab`

## Verification

The SHA-256 checksum for the packaged AAB is in:

`ColourMyWorld-v1.0.19-SHA256SUMS.txt`

After copying or downloading the AAB, verify with:

```powershell
Get-FileHash -Algorithm SHA256 .\ColourMyWorld-v1.0.19-playstore.aab
```
