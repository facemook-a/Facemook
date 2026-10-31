# Morocco Factory downloads

- [Installable debug APK](MoroccoFactory-debug.apk?raw=true)
- [Complete AndroidIDE source ZIP](MoroccoFactory-source.zip?raw=true)
- [APK and source together](MoroccoFactory-download.zip?raw=true)
- [SHA-256 checksums](SHA256SUMS.txt)

The application uses package `com.morocco.appfactory`, minimum SDK 21 and target SDK 33. This APK is signed with a development key, not a production release key. AdMob uses Google test IDs. Google sign-in requires the OAuth configuration described in [the Android README](../android/README.md).

The factory exports Android source projects. Build those projects in AndroidIDE to obtain their APKs. The cloud validation passed 19 tests, Lint with zero errors, and independent APK builds for all four generated project types. Physical-device and AndroidIDE execution were not tested here.

The original Facemook web application is unchanged. The full Java Android project is in [android/](../android/).
