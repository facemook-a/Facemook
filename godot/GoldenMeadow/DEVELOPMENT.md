# Development

Godot 4.6.3, GDScript, Compatibility renderer. All gameplay assets ship locally; the game does not call remote APIs or require a login. Preserve `application/config/name` and `org.b5.goldenmeadow` when upgrading a user's existing installation.

The current B5 avatar intentionally remains the existing placeholder. Its four limb pivots are driven by an AnimationTree state machine (`Idle`, `Walk`, `Run`, `Jump`, `Swim`) with crossfades. A future realistic character should replace the visual scene and animation implementation without changing the CharacterBody3D movement or save identity.

`world.gd` owns streamed terrain and grass, house generation and high-level state. Static house visuals are batched by material, with door hinges and physics bodies kept independent. `landscape.gd` builds shared-vertex indexed terrain and samples the same triangle heights for grass. World positions and object IDs remain compatible with the previous release.

Tajawal is embedded unmodified in FontFile .tres resources; original TTFs are in an import-ignored raw folder. This avoids a FontFile importer crash observed in the provided Godot build. Rebuild the containers with tools/build_fonts.gd if changing the font.

The HUD uses original local SVG icons and the OFL-licensed Tajawal Arabic font. Icon-only play controls have desktop tooltips and a control guide; proximity interaction captions are separate from the button hit target. The HUD owns a pause-safe menu. Touch buttons, map selection and sliders are dispatched explicitly, because multi-finger movement must not be converted into a single mouse pointer. Input is suppressed behind modal panels. Audio buses separate Music, Ambience and Effects. Android quality profiles target 30 fps in low and 60 in other modes; these are caps, not measured device performance guarantees.

`ProfileStore` retains unknown legacy save fields, validates positional data, writes a flushed temporary file, backs up the last readable save and atomically renames the new file. Settings use a separate ConfigFile. Do not run test fixtures against a user's personal save directory.

## Isolated verification

On Linux/macOS, use a temporary XDG_DATA_HOME, XDG_CONFIG_HOME and XDG_CACHE_HOME. Example after importing the project:

```sh
B5_TEST_DATA="$(mktemp -d)"
XDG_DATA_HOME="$B5_TEST_DATA" godot --headless --fixed-fps 60 --script res://tests/smoke.gd -- --fresh
XDG_DATA_HOME="$B5_TEST_DATA" godot --headless --fixed-fps 60 --script res://tests/restore.gd
XDG_DATA_HOME="$B5_TEST_DATA" godot --headless --fixed-fps 60 --script res://tests/polish.gd
```

The last test deliberately replaces fixture saves and corrupts its test save to verify backup recovery. Test a fresh extracted ZIP too. Use `tests/capture_polish.gd` with an actual display to capture village, map, settings and coast; screenshots are written to that isolated `user://` directory. Dummy/headless rendering cannot verify visual appearance.

Android export requires the matching templates, Java SDK, Android SDK/platform-tools/build-tools and signing configuration. No credentials or signing keys are included. Build and physical-device validation remain necessary before shipping an APK. No APK is claimed in this milestone.
