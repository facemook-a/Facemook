#!/usr/bin/env python3
"""Generate real fixture projects with the production generator, then build all four APKs."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile


def main():
    root = Path(__file__).resolve().parents[1]
    wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
    subprocess.run([str(wrapper), ":app:testDebugUnitTest", "--tests",
                    "com.morocco.appfactory.build.SourceGeneratorTest"], cwd=root, check=True)
    results = []
    for mode in ("village", "game", "app", "code"):
        project = root / "app/build/export-fixtures" / mode
        shutil.copyfile(root / "gradle/wrapper/gradle-wrapper.jar",
                        project / "gradle/wrapper/gradle-wrapper.jar")
        subprocess.run([str(wrapper), "--project-dir", str(project), "assembleDebug"],
                       cwd=root, check=True)
        apk = project / "app/build/outputs/apk/debug/app-debug.apk"
        with zipfile.ZipFile(apk) as archive:
            assert {"AndroidManifest.xml", "classes.dex"}.issubset(archive.namelist())
        results.append({"mode": mode, "apk": str(apk.relative_to(root)),
                        "sha256": hashlib.sha256(apk.read_bytes()).hexdigest()})
        print("Verified", mode, "APK", flush=True)
    report = root / "app/build/reports/export-builds.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
