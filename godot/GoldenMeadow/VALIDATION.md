# Validation — 0.4

Engine: Godot 4.6.3 stable, Linux. Compatibility rendering was exercised on a virtual X11 display using Mesa llvmpipe. This is functional and visual verification, not an Android FPS benchmark.

- Original gameplay suite: 28 passing checks for movement, jump/landing, multi-touch camera/joystick, touch jump, streamed solid terrain, bounded chunks, quality changes, nine doors, entering the cottage, beach terrain, following/stopping companions, bird movement, ocean audio, collectibles and persistence.
- Polish suite: 25 checks for legacy-save migration, all five AnimationTree states including swimming/float behavior, desktop map/Escape input, pause/resume, map navigation, touch sliders, saved audio settings and zero-volume mute, journal progression, retained legacy fields, saved/restored doors and companions, house batching and recovery from a corrupted save.
- Separate-process restore and fresh-ZIP import/run checks are required for the release archive; final command results are recorded with the delivery.
- Actual game captures cover the village, map, settings and coast. They show the existing B5 placeholder; the AI character reference is not a finished in-game model.

Android APK export was attempted and blocked by unavailable Android export templates and SDK/build-tools configuration. Android hardware, thermal behavior and touch behavior on an actual phone have not been tested. Some automated SceneTree runners report a resource cleanup warning at exit; the gameplay assertions complete. The ordinary timed game run was also exercised separately.
