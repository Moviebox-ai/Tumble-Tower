# Tumble Tower: Skyfall

A physics-driven, 2.5D isometric tower-balancing Android game built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM + StateFlow**, **Room + DataStore**, and **Firebase Firestore + Google Sign-In**.

## Key Features
- **7 Special Block Types**: Wood, Ice (low friction + melts over time), Rubber (elastic bounce), Magnet (stabilizes nearby layers), Gold (heavy + bonus coins), Bomb/TNT (countdown blast impulse), and Glass (shatters under high upper load).
- **Live Weather & Physics Events**: Directional Wind gusts, Earthquakes (tremors + screen shake), Storm Rain (slippery blocks), and Low Gravity.
- **Steady Hand & Stability Meter**: Precision pull tracking awards combo multipliers; live Green/Yellow/Red stability meter triggers haptic & heartbeat alerts.
- **Slow-Mo Collapse Cam + Replay**: 0.28x slow-motion collapse physics with frame snapshot recording for instant replay.
- **5 Power-Ups**: Freeze, Glue, Undo, Shield, and Mini Crane.
- **7 Game Modes & 210 Campaign Levels**: 5 Worlds (Mystic Forest, Sunstone Desert, Frostbite Peaks, Nebula Orbit, Lava Kingdom) with Boss Towers every 10th level (*Leaning Tower*, *Pyramid of Doom*, *Spiral Skyscraper*), Classic, Endless Rise (with Ghost Tower outline), Time Attack (90s), Survival, Pass & Play (2–4 players), and deterministic Daily Challenge.
- **Photo Mode**: Capture tower snapshots with backdrop presets, toggleable watermark, and zero-permission Android share sheet.
- **Shop, Rewards, Spin Wheel, Missions & Settings**: Block skins, tower themes, particle effects, 7-day daily streak, Lucky Spin Wheel, 8-language localization (`en`, `hi`, `es`, `pt`, `ar`, `in`, `fr`, `de`), Left-Hand Mode, Colorblind symbols mode, and 30 FPS battery-friendly option.


## NEW: The Readable Tower (signature mechanic)
Most tower games are pure luck. Here the tower is *readable* - a real load-path solver (`game/physics/StructuralSolver.kt`) computes how much weight every block carries and how dangerous each pull is.

- **Load-path solver**: mass above each layer + its centre of mass is spread over the layer's blocks as a rigid plate on non-tension supports (active-set solve). `ratio = load / capacity(type, melt, glue)`.
- **Stress Vision (X-Ray)**: toggle on the HUD. Every block is heat-tinted by *pull risk* with a load gauge on its bottom edge and a live "PULL RISK %" badge while dragging. It runs on **Focus**, which drains while active and refills from Steady-Hand pulls, Daredevil pulls and Calm Drops.
- **Crush failures**: a block over 100% load for 0.45 s is crushed (glass shatters at 102%). Blocks show tension cracks and the tower creaks (sound + haptics) before it fails, so what you see is what can kill you.
- **Daredevil wager**: surviving a pull the solver rated >= 45% risky pays a bonus after the tower stays up 1.1 s (streak multiplier). Fatal pulls pay nothing.
- **Calm Drop**: releasing a block while the tower is still gives bonus + damping; releasing during heavy sway feeds the wobble. Wind and earthquakes make timing matter.
- **Mini Crane** now asks the solver for the lowest-risk block.
- **Rendering**: chunky reference-style blocks (wood grain, glossy cracked ice, embossed gold, TNT stripes), stone pedestal, auto-fit camera.

Tests: `StructuralSolverTest`, `ReadableTowerMechanicsTest` (pure Kotlin, run with `gradle :app:testDebugUnitTest`).

## Setup & Configuration

### 1. Firebase Setup
- `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are provisioned for **Firebase Firestore** and **Firebase Auth (Google Sign-In via Credential Manager)**.
- Note: Anonymous Auth (`signInAnonymously()`) is intentionally replaced with Google Sign-In (`GetSignInWithGoogleOption`) per backend security requirements. If you also want Firebase Remote Config, Analytics, or Crashlytics in production, enable them in the Firebase Console and add `firebase-config`, `firebase-analytics`, and `firebase-crashlytics` to `app/build.gradle.kts`.
- Deploy security rules from `firestore.rules` before release.

### 2. Replacing AdMob & Billing Test IDs
- **AdMob Unit IDs**: Replace the official Google test IDs in `app/src/main/java/com/example/services/AdAndBillingServices.kt` (`AdConfig`) and the `com.google.android.gms.ads.APPLICATION_ID` meta-data tag in `app/src/main/AndroidManifest.xml`.
- **Google Play Billing**: `PlayBillingAdapter` in `AdAndBillingServices.kt` provides the SKU catalog (`remove_ads_forever`, `coins_pile_2500`, `coins_vault_10000`, `gems_crown_250`). Swap its `purchaseProduct`/`restorePurchases` methods with `BillingClient` for Google Play release.

### 3. Audio & Visual Assets
- All SFX (wood knock, ice crack, glass shatter, rubber boing, magnet hum, coin ding, TNT boom, heartbeat, collapse boom, fanfare) and adaptive dynamic music are synthesized procedurally via `AudioTrack` in `SoundAndHapticsEngine.kt` with zero external asset bloat.
- All tower worlds, isometric blocks, and particles are rendered natively on Jetpack Compose `Canvas` (`TowerCanvas.kt`).

## Build & Test Steps
1. Open the project in Android Studio (or Gradle CLI with `ANDROID_SDK_ROOT` set).
2. Build debug APK: `gradle :app:assembleDebug`
3. Run unit & Robolectric tests: `gradle :app:testDebugUnitTest`

## GitHub Actions CI/CD
Two workflows are included in `.github/workflows/`:
- **`android-ci.yml`**: Runs on pushes/PRs to `main`, `master`, or `develop`. Executes physics unit tests, Android Lint (`lintDebug`), builds the debug APK (`assembleDebug`), uploads artifacts/reports, and verifies `firestore.rules` + `FirebaseSyncRepositoryRuleTest` inside the Firebase Auth & Firestore emulators.
- **`android-release.yml`**: Runs on version tags (`v*.*.*`) or manual `workflow_dispatch`. Builds signed release APK & Play App Bundle (`.aab`) and attaches them to a GitHub Release.
  - Required GitHub Repository Secrets for signed releases:
    - `RELEASE_KEYSTORE_BASE64`: Base64-encoded upload keystore (`my-upload-key.jks`)
    - `STORE_PASSWORD`: Keystore password
    - `KEY_PASSWORD`: Upload key password (`upload` alias)
    - `GOOGLE_SERVICES_JSON_BASE64` *(optional if `app/google-services.json` is not committed)*: Base64-encoded `app/google-services.json`

