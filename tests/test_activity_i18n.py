import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

class ActivityResourceTest(unittest.TestCase):
    def test_bilingual_resource_sets_exist_and_match(self):
        base = ROOT / 'src/main/res/values/strings.xml'
        zh = ROOT / 'src/main/res/values-zh/strings.xml'
        self.assertTrue(base.exists(), 'English baseline resources must exist')
        self.assertTrue(zh.exists(), 'Chinese resources must exist')
        def keys(path):
            return {e.attrib['name'] for e in ET.parse(path).getroot() if e.tag in ('string', 'plurals')}
        self.assertEqual(keys(base), keys(zh))

    def test_bilingual_format_placeholders_match(self):
        import re
        base = ROOT / 'src/main/res/values/strings.xml'
        zh = ROOT / 'src/main/res/values-zh/strings.xml'
        def values(path):
            return {e.attrib['name']: ''.join(e.itertext()) for e in ET.parse(path).getroot() if e.tag == 'string'}
        en_values, zh_values = values(base), values(zh)
        self.assertEqual(set(en_values), set(zh_values))
        for key in en_values:
            self.assertEqual(re.findall(r'%\d+\$(?:0\d+)?[ds]', en_values[key]), re.findall(r'%\d+\$(?:0\d+)?[ds]', zh_values[key]), key)

    def test_activity_uses_resources_for_chinese_ui_literals(self):
        source = (ROOT / 'src/main/java/cn/medreminder/app/MainActivity.java').read_text()
        self.assertNotRegex(source, r'"[^"\n]*[\u3400-\u9fff][^"\n]*"')

    def test_notification_handler_routes_with_stable_today_state(self):
        import re
        source = (ROOT / 'src/main/java/cn/medreminder/app/MainActivity.java').read_text()
        handler = re.search(r'private void handle\(Intent i\)\s*\{(.*?)\n\s*\}', source, re.S)
        self.assertIsNotNone(handler, 'notification Intent handler must remain present')
        body = handler.group(1)
        self.assertIn('page=UiState.TODAY;', body)
        self.assertNotRegex(body, r'page\s*=\s*getString\s*\(')

    def test_alarm_presentation_uses_bilingual_resources(self):
        import re
        java_root = ROOT / 'src/main/java/cn/medreminder/app'
        source = '\n'.join((java_root / name).read_text() for name in ('Alarms.java', 'AlarmSelfTest.java'))
        self.assertNotRegex(source, r'"[^"\n]*[\u3400-\u9fff][^"\n]*"')
        keys = {e.attrib['name'] for e in ET.parse(ROOT / 'src/main/res/values/strings.xml').getroot() if e.tag == 'string'}
        used = set(re.findall(r'R\.string\.([A-Za-z0-9_]+)', source))
        self.assertTrue(used, 'alarm presentation should reference resources')
        self.assertLessEqual(used, keys, 'every alarm resource reference must be defined')
        self.assertIn('notification_channel_name', keys)
        alarms = (java_root / 'Alarms.java').read_text()
        self.assertIn('CHANNEL=\"medication_reminders\"', alarms)
        self.assertIn('CHANNEL=\"background_test\"', (java_root / 'AlarmSelfTest.java').read_text())
        manifest = (ROOT / 'AndroidManifest.xml').read_text()
        self.assertIn('package=\"cn.medreminder.app\"', manifest)
        self.assertIn('android:versionCode=\"10\"', manifest)
        self.assertIn('android:versionName=\"1.3.5\"', manifest)

    def test_license_is_packaged_verbatim(self):
        self.assertEqual((ROOT / 'LICENSE').read_bytes(), (ROOT / 'src/main/res/raw/license_mit.txt').read_bytes())

    def test_release_docs_name_current_release_and_locales(self):
        en = (ROOT / 'README.md').read_text()
        zh = (ROOT / 'README.zh-CN.md').read_text()
        en_notes = (ROOT / 'release-notes.md').read_text()
        zh_notes = (ROOT / 'release-notes.zh-CN.md').read_text()
        for content in (en_notes, zh_notes):
            self.assertIn('1.3.5', content)
            self.assertIn('10', content)
            self.assertIn('cn.medreminder.app', content)
        self.assertIn('Medication Log', en)
        self.assertIn('用药记', zh)
        self.assertIn('does not provide medical advice', en)
        self.assertIn('不提供医疗建议', zh)

if __name__ == '__main__': unittest.main()
