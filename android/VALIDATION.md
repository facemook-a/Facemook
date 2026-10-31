# Validation evidence

The current cloud checkout was validated with JDK 17.0.17, Gradle 8.2.1, AGP 8.2.2, Android SDK 33/34 and build-tools 34.0.0.

| Check | Result |
| --- | --- |
| `:app:assembleDebug` | Passed; genuine debug APK generated |
| `:app:testDebugUnitTest` | 19 tests passed, 0 failed, 0 skipped |
| `:app:lintDebug` | Passed; 0 errors, 110 warnings |
| Included Gradle wrapper | Download/checksum verification and execution passed |
| `python3 tools/verify_exports.py` | All four generated projects compiled to APKs |
| APK signature verification | Passed, v1 and v2 debug signing |
| APK manifest inspection | `com.morocco.appfactory`, min SDK 21, target SDK 33 |
| Dashboard rendering | Rendered with Robolectric native graphics and visually inspected |

The full suite comprises 7 generator tests, 1 password-hashing test, 10 screen/workflow tests and 1 asynchronous export-service test. The minimum-SDK test inflates all four builders using Robolectric API 21. The service test verifies that the ZIP contains its real Java source, manifest, project JSON and Gradle wrapper JAR. The saved code-draft test recreates the editor while a Java file is incomplete.

The separate export-verification script reruns the 7 generator tests and builds the resulting village, game, no-code app and code app with Gradle. Its generated build report records SHA-256 hashes. This checks actual compilation, including Unicode, XML/Java escaping, all palette component types and both no-code click actions.

Lint warnings remain, primarily for hardcoded English UI text, resource/attribute compatibility guidance, and intentionally pinned SDK/dependency versions. No Lint baseline, assertion disabling or TLS-verification bypass was used.

No physical device, Android emulator or AndroidIDE installation was available. Robolectric screen tests are not a substitute for a device test. Google OAuth configuration, actual Google sign-in and rewarded test-ad delivery were not exercised; the app exposes real SDK integrations and reports failures without simulating success. APK compilation of user projects happens in AndroidIDE after source export, not inside the factory app.
