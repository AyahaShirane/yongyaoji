#!/usr/bin/env python3
"""Derive a versioned APK output path directly from the Android manifest."""
import sys
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


def output_path(manifest: Path, artifacts: Path) -> Path:
    root = ET.parse(manifest).getroot()
    version = root.attrib.get("{http://schemas.android.com/apk/res/android}versionName")
    code = int(root.attrib["{http://schemas.android.com/apk/res/android}versionCode"])
    if not version or any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_" for c in version):
        raise ValueError("manifest has no safe android:versionName")
    found = []
    for existing in artifacts.glob("med-reminder-v*.apk"):
        output = subprocess.check_output(["aapt", "dump", "badging", str(existing)], text=True)
        match = re.search(r"versionCode='(\d+)'", output)
        if match:
            found.append((int(match.group(1)), existing))
    ensure_not_downgrade(code, found)
    return artifacts / ("med-reminder-v" + version + ".apk")


def ensure_not_downgrade(code: int, existing) -> None:
    newer = [path for old_code, path in existing if old_code > code]
    if newer:
        raise ValueError(f"refusing versionCode downgrade below {newer[0]}")


if __name__ == "__main__":
    print(output_path(Path(sys.argv[1]), Path(sys.argv[2])))
