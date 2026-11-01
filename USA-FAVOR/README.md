# USA-FAVOR · B5 STUDIOS

Two native Android applications, one Firebase Realtime Database backend, and an embedded OpenVPN engine. No external application server or Cloud Functions required. No uploaded ZIP was opened. This project was created alongside the existing Facemook checkout; the web repository was not changed.

## Build and install

Open this root folder in Android Studio: `Admin` is `com.usa.admin`, `User` is `com.usa.user`, and `openvpn` is the embedded engine. `USA-Admin/` and `USA-Favor/` also open independently as Android Studio projects. Android 8.0/API 26 or newer, compile/target SDK 35, JDK 17+ (validated with JDK 21), Gradle 8.11.1, AGP 8.9.1. Install Android SDK Platform 35 and Build Tools 35.0.0. Set your SDK path in `local.properties` or `ANDROID_HOME`.

In this prepared cloud machine, `python3 tools/cloud-build.py` activates the retained JDK/SDK and forwards the configured proxy/CA trust without recording credentials. On a standard Android development machine, run `tools/build.sh` to assemble both debug-signed APKs and unit tests. Output: `dist/USA-Admin.apk` and `dist/USA-Favor.apk`. These APKs are installable development builds, not Play Store releases. Keep your release signing keystore private and configure your own release signing before distribution. The archive omits machine-specific local.properties, caches, and build outputs; it includes Gradle wrappers and the OpenVPN native libraries for arm64-v8a, armeabi-v7a, x86, and x86_64.

`./gradlew :Admin:lintDebug :User:lintDebug` runs Android lint. Gradle respects Java HTTP/HTTPS proxy properties in managed networks; preserve your proxy and CA trust rather than disabling TLS checks.

## Firebase setup (free HTTPS backend)

1. In Firebase Console create a project on the Spark/free plan. Register Android applications `com.usa.admin` and `com.usa.user`.
2. Create a Realtime Database in **locked mode**, choosing your region. Save its full HTTPS URL (`https://PROJECT-default-rtdb.firebaseio.com` or regional `https://PROJECT-default-rtdb.REGION.firebasedatabase.app`). Do not enable public read/write rules.
3. Enable Authentication → Anonymous for the user application, and Email/Password for the administrator.
4. Create a real administrator account in Authentication. Its Firebase password must satisfy Firebase's minimum length; the local `admin / 1234` PIN is a separate UI gate, not a database credential.
5. Publish `firebase/database.rules.json` in Realtime Database → Rules. In the database **Console**, create `/admins/ADMIN_AUTH_UID: true` using the UID of the account in step 4. Client apps cannot grant themselves admin status. The example seed is illustrative: replace the UID; do not overwrite existing database data.
6. Download each app's `google-services.json`. The supplied files are visibly marked placeholders and cannot connect to a real project. Run:

   ```sh
   python3 tools/configure-firebase.py \
     --admin-json /path/to/admin-google-services.json \
     --user-json /path/to/user-google-services.json \
     --database-url https://YOUR-DATABASE-URL
   tools/build.sh
   ```

   Alternatively the supplied APKs show **Connect Firebase** on first launch. Enter Project ID, Database URL, Web API Key, and that app's Firebase App ID. These are public application identifiers, not service-account keys. Settings are local to each app. Restart after changing an already initialized Firebase configuration. No service-account credentials should be placed in an APK.
7. Open USA-Admin, enter `admin / 1234`, then sign in with the authorized Firebase administrator account. Generate a duration-bound `USA-XXXX` code. Open USA-Favor and activate it.

Both apps must use the same Firebase project/database, with their own Android App IDs. Firebase Spark has quotas (including a simultaneous-connection limit); “free” does not mean unlimited. Enable Firebase's appropriate API-key application restrictions only after adding the correct package/signature configuration.

### Database behavior and protection

- `/codes/{CODE}` stores duration, used, createdAt, isActive, and—after activation—deviceId, deviceModel, ownerUid, and expiryAt for finite-duration codes. Firebase omits null children; LIFETIME has no expiryAt. Non-activated codes have no device binding.
- Activation is a Firebase transaction. Rules permit only the first unused-to-used transition, enforce the intended duration against server time, and forbid clients from extending, resetting, or stealing the binding. A previously used code on another device/identity is rejected with `Code already used`. Same-device/same-identity access resumes without consuming another activation. Reinstall/cleared app data changes the anonymous Firebase identity and may require a replacement code.
- `/users/{ANDROID_ID}` includes codeUsed, ownerUid, deviceModel, ip, country, connectedAt, lastSeen, isOnline. A user can update only the device/code bound to that identity. IP/country are client-observed operational information, not trustworthy security attestations.
- Online presence represents the app being open, not necessarily an established VPN. `onDisconnect` is registered before setting online. Foreground heartbeat updates every 60 seconds; app closing queues offline. Admin **Active Users NOW** counts `isOnline == true` with lastSeen less than 120 seconds old, and stale records display offline. Offline clients cannot guarantee an immediate disconnect write, so the two-minute freshness cutoff matters.
- Expiry, deletion, or deactivation locks the user screen and stops the VPN when synchronized; finite expiry is checked locally every second even while the VPN service runs. Countdown uses a Firebase time offset anchored to Android monotonic elapsed time between synchronizations.
- `/config/socials` is publicly readable so expired users can contact B5, and writable only by authorized admins. Links must use HTTPS. Four follow buttons display configured TikTok, Instagram, YouTube and Telegram destinations.
- No permissive root rules or client-stored administrator password are used. Four-character access codes are a requested UX tradeoff; Firebase-only rules do not provide general per-IP brute-force rate limiting. An authorized administrator can inspect all operational records. Review privacy and quota requirements before a public launch.

## OpenVPN, location, and Kill Switch

The uploaded `US-NewYork.ovpn` is embedded **byte for byte** at `USA-Favor/app/src/main/assets/US-NewYork.ovpn`, including its inline certificate material. It specifies **TCP port 1923**. A second asset `US-NewYork-UDP.ovpn` preserves its inline material and selects the initial request's **UDP port 1195**. The transport switch selects between the uploaded TCP profile (default) and UDP alternative. Existing connections use their selected profile until reconnected. The public endpoint is `vpn318641113.opengw.net`; its uptime, supported ports, and geographic location are controlled by that VPN operator.

Real encrypted traffic uses the embedded upstream OpenVPN-for-Android v0.7.64 engine, not a simulated status display or an external installed VPN app. The official engine is the local `:openvpn` library, including its `de.blinkt.openvpn` classes and AIDL interfaces. No unresolved/fictitious `de.blinkt.openvpn:openvpn-api` Maven coordinate is used. Official-release native libraries were extracted only after APK signature verification; provenance, license and corresponding source are included under `USA-Favor/openvpn/` and `third-party/`.

On first connection Android prompts for VPN consent. Notifications show the actual engine state. The app queries `https://ipwho.is/` to verify the exit IP and country and shows “New York” only when the exit location response actually reports it. The profile name alone never proves geographic location. TikTok recommendations depend on more than IP; neither a VPN nor clearing cache guarantees an American feed. Language opens Android's language settings for the user to select English (United States).

**Real mock GPS:** `MockLocationService.java` uses `LocationManager.addTestProvider`, `setTestProviderEnabled`, and `setTestProviderLocation` to publish `40.7128, -74.0060` every second. Grant location permission and select Developer Options → Select mock location app → USA-Favor. The app checks the Android mock-location AppOp and displays the exact setup instruction if unavailable. The service follows VPN connection/disconnection and respects the GPS preference; Android marks the location as mock. It cannot secretly change hardware GPS or bypass other apps' mock-location detection.

**Real app Kill Switch:** `B5VpnService` and the integrated engine route IPv4 and IPv6 through the tunnel, use blocking TUN descriptors, clear underlying-network overrides, and retain the TUN during transport reconnection. When the transport fails, an established blackhole TUN holds all routed traffic until a new OpenVPN TUN replaces it. The ON/OFF switch persists in SharedPreferences. Intentional disconnect/expiry releases the app guard. App protection requires the service to remain alive. Android **Always-on VPN + Block connections without VPN** is required to cover force-stop, process death, VPN permission revocation, or reboot; the app opens those system settings and reports the real OS lockdown state. `setBlocking(true)` controls file-descriptor I/O and is not by itself a kill switch.

## Validation scope

See `dist/VALIDATION.md` for current build/test/signature results and limitations. A successful APK build and unit/shadow tests do not prove an end-to-end tunnel, real-device packet-leak protection, GPS acceptance by TikTok, or a live Firebase deployment. Firebase identifiers and an Android device with the relevant consent/settings are needed to complete that validation.

Firebase rules tests are in `firebase/test/rules.test.mjs` and cover admin-only generation, activation ownership, duration limits, immutable bindings, user privacy, and social-link writes. To run them on a development machine: `cd firebase && npm install && npm test`. This uses the **local test emulator only**; no external backend server is required in production. Its official emulator download needs `storage.googleapis.com`, which this cloud environment currently blocks.

## B5 branding and licenses

Both apps use a black/gold B5 monogram drawn natively with a fused letter/number, depth gradient and glow, a 2.5-second scale/fade splash, “B5 STUDIOS”, “Developed by B5”, and “Powered by B5”. No third-party fashion logo is copied.

The user application incorporates GPL-covered OpenVPN source and native code. GPL plus upstream additional terms apply; preserve the notices and corresponding source when redistributing. This repository provides the application source, upstream license files, native-source archive and integration change log. Do not describe the GPL engine as proprietary. Android/Firebase and other dependencies retain their respective licenses.
