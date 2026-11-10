import os
import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from release_path import ensure_not_downgrade, output_path


class ReleasePathTest(unittest.TestCase):
    def test_manifest_version_selects_only_matching_release_path(self):
        root = Path(os.environ["TMPDIR"]) / "release-path-regression"
        root.mkdir(parents=True, exist_ok=True)
        manifest = root / "AndroidManifest.xml"
        artifacts = root / "artifacts"
        manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" android:versionName="1.2.0" android:versionCode="3"/>')
        self.assertEqual(output_path(manifest, artifacts), artifacts / "med-reminder-v1.2.0.apk")
        manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" android:versionName="1.3.0" android:versionCode="4"/>')
        self.assertEqual(output_path(manifest, artifacts), artifacts / "med-reminder-v1.3.0.apk")

    def test_version_code_must_advance_past_existing_release(self):
        with self.assertRaises(ValueError):
            ensure_not_downgrade(2, [(1, "v1.0"), (3, "v1.2")])
        ensure_not_downgrade(3, [(1, "v1.0"), (2, "v1.1"), (3, "v1.2")])

    def test_fixed_schedule_save_does_not_read_hidden_frequency_inputs(self):
        source = (Path(__file__).resolve().parents[1] / "src/main/java/cn/medreminder/app/MainActivity.java").read_text()
        save = source.split("dialog.setOnShowListener(x->{styleDialog(dialog);", 1)[1].split("dialog.show();", 1)[0]
        self.assertIn("if(selected==0){long anchorAt=anchor.get();int d=Integer.parseInt(days.getText().toString()),c=Integer.parseInt(count.getText().toString())", save)
        self.assertIn("else{String fixedTimes=Core.normalizeTimes(times.getText().toString())", save)


if __name__ == "__main__":
    unittest.main()
