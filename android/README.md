# Morocco Factory — Java / AndroidIDE

A standalone Android project inside the Facemook repository. The existing Express application is unchanged. All Android application code is Java, with XML resources and Groovy Gradle files.

## Build and install

Open **this `android/` directory**, rather than the repository root, in AndroidIDE or Android Studio.

- JDK **17**; Gradle **8.2.1** (included wrapper, SHA-256 pinned); Android Gradle Plugin **8.2.2**.
- Install Android SDK **34** and build-tools **34.0.0**. `compileSdk=34` is required by Material 1.11; **minSdk=21**, **targetSdk=33**.
- AndroidIDE supplies its Android-compatible `aapt2` through its build environment. Keep that configuration; do not replace it with the Linux executable when building on a phone.
- Let the IDE create `local.properties`, or set `ANDROID_HOME` to your SDK. No machine-specific SDK path is committed.

```sh
chmod +x gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Connect an Android device with USB debugging enabled:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Release signing keys are intentionally not included. AndroidIDE itself must supply an SDK and Java toolchain; an APK does not contain Gradle or a Java compiler.

## What works

| Area | Behavior |
| --- | --- |
| Entry | Animated splash, guest session, email/password local accounts, Google sign-in integration |
| Dashboard | Searchable creation modes, saved projects, guide, profile and workspace selection |
| Village | Name-seeded sample geography, isometric software rendering, drag-to-orbit, house/road statistics |
| Game | Walking Sim, Driving and Building; add and long-press-drag houses, joystick play, collisions, JSON game logic |
| App designer | Tap/drag palette, move/select components, edit text/color/width, Toast and second-screen logic, interactive preview |
| Java editor | Multiple `.java` files, line numbers, keyword/string/comment highlighting, save, static text preview and export validation |
| Export | Rewarded-ad gate, background project generation, gold progress indicator, complete Gradle project ZIP, share sheet |

Titles use bundled Poppins Bold (license in `third_party/`). The night village is an original vector illustration, so no remote image or font request is required. The phone designer stacks its panels; wide screens place palette, canvas and properties side by side.

## Storage and recovery

Projects are JSON files with stable UUIDs and schema version 1. Local writes use a temporary file and atomic rename. Saved projects reopen from the Projects tab; long press deletes the local copy.

Default workspace:

```text
/storage/emulated/0/Android/data/com.morocco.appfactory/files/MoroccoFactory/
  Projects/<project-id>.json
  Export/<name>_<id>_<timestamp>/
  Export/<name>_<id>_<timestamp>.zip
```

Internal app storage is used if external storage is unavailable. Android 11+ does not permit arbitrary writes to `/sdcard/MoroccoFactory` with target SDK 33. To keep copies in that location, use **Profile → Choose storage folder**, create/select `MoroccoFactory`, and grant access through Android's folder picker. Future explicit saves mirror JSON into `Projects/`, and exports mirror ZIPs into `Export/`. Folder-copy errors are reported while the local copy is retained. Removing the app deletes its app-specific workspace, so copy projects to your chosen folder first. Shared copies are portable backups; the in-app project list reads the local workspace, not arbitrary folder contents.

Drafts survive activity recreation through small saved-state references to cache files. Explicit Save is required for durable project-library entries. Local profiles share the same device workspace; these are not isolated cloud accounts.

## APK export contract

The requested `ApkBuilderService` creates **real source files and a Gradle wrapper**, then packages them in a ZIP. It does **not** relabel a ZIP as an APK, simulate successful compilation, or embed an Android build toolchain.

1. Save your project.
2. Watch Google's rewarded **test** ad completely. Export unlocks only in `onUserEarnedReward`; a failed/cancelled ad grants nothing. The reward is saved for that project.
3. Tap **EXPORT APK**. The adjacent explanation identifies this as source export.
4. Share/unzip the project and open its root folder in AndroidIDE.
5. Build `assembleDebug` to produce a real, installable debug APK.

Exported projects use package `com.morocco.generated`, min SDK 21, target/compile SDK 33, and no third-party application dependencies. They include the project JSON, manifest, source, layout, Gradle files, wrapper JAR and build instructions. Change their application ID before installing multiple generated apps side by side. Code-mode files must use package `com.morocco.generated` and include `MainActivity.java`. Draft files may be incomplete; export checks the package/file contract, while AndroidIDE supplies full Java compilation diagnostics.

The village module intentionally uses sample map data, as requested. It does not contact OpenStreetMap. Game previews and generated games support movement/collision; there is no Unity engine. Code-mode **Run Preview** displays the first literal `setText("...")` value and explicitly does not execute arbitrary Java. Kotlin compilation is not included because this project is Java-only.

## Authentication and ads

**Guest/local accounts** work offline. The first email/password login creates a device-local account. Passwords use unique 128-bit salts and PBKDF2-HMAC-SHA1 with 120,000 iterations; cleartext passwords are never saved. This is local authentication, not a backend identity service or cloud sync.

**Google sign-in** uses Play Services Auth. Register the Android OAuth client for `com.morocco.appfactory` and the signing certificate SHA-1 in Google Cloud Console. Configure the consent screen/test users as appropriate and use a device with Google Play Services. Obtain the debug signing identity with `./gradlew :app:signingReport`. No server client ID, API key, `google-services.json`, or fabricated credential is bundled. Configuration or device failures are shown to the user; they do not create a fake Google session. This application uses the returned Google email for its local profile, not for backend authorization.

**AdMob** uses the requested SDK **22.0.0** and Google's official sample application/rewarded-unit IDs in `res/values/strings.xml`. Test ads require network access and suitable Play Services. Replace both IDs with your own and add the consent/account configuration required for your distribution before enabling live monetization. Do not use the sample IDs to claim production revenue.

## Validation

`testDebugUnitTest` covers source generation for all four modes, escaping/path safety, password hashing, screen inflation, guest navigation, scene generation, game controls, project round trips, designer properties/logic, Java highlighting and dashboard search/rendering. Tests write exported fixtures under `app/build/export-fixtures/` for independent Gradle builds, plus a dashboard rendering under `app/build/reports/ui/`.

Device-dependent Google authentication and rewarded-ad delivery require a configured device/network and are not proven by JVM tests. AndroidIDE-specific execution must also be checked on an actual AndroidIDE installation; desktop Gradle validation does not establish that a phone has the required toolchain.

To reproduce independent compilation of all four exported templates (SDK 33 and build-tools 34.0.0 required):

```sh
python3 tools/verify_exports.py
```

This invokes the production source generator through its tests, adds the same wrapper JAR used by the export service, builds each generated project, and records APK SHA-256 hashes in `app/build/reports/export-builds.json`. On a corporate or sandboxed network, configure the supported Java/Gradle proxy and CA trust; do not disable TLS verification.
