"""Offline regression tests for public platform integrity checks."""
import hashlib
import importlib.util
import io
import lzma
from pathlib import Path
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location('verify_public', Path(__file__).parents[1] / 'verify-public-platform.py')
PREP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREP)


class PublicPlatformTest(unittest.TestCase):
    def fixture(self, checksum=True, missing=False):
        content = b'<repository><units><unit id="example" version="1.0.0"><artifacts><artifact classifier="osgi.bundle" id="example" version="1.0.0"/></artifacts></unit></units></repository>'
        prop = '<properties><property name="download.checksum.sha-256" value="' + 'a' * 64 + '"/></properties>' if checksum else ''
        artifacts = ('<repository><artifacts>' + ('' if missing else '<artifact classifier="osgi.bundle" id="example" version="1.0.0">' + prop + '</artifact>') + '</artifacts></repository>').encode()
        payloads = {'content.xml.xz': lzma.compress(content), 'artifacts.xml.xz': lzma.compress(artifacts)}
        repo = dict(id='example', url='https://example.invalid/', metadata={
            n: hashlib.sha256(v).hexdigest() for n, v in payloads.items()})
        return repo, payloads

    def target(self, version='1.0.0', url='https://example.invalid/'):
        return f'<target><locations><location><repository location="{url}"/><unit id="example" version="{version}"/></location></locations></target>'

    def test_valid_locked_public_target(self):
        repo, payloads = self.fixture()
        content = PREP.validate_metadata(repo, payloads)
        PREP.validate_target(self.target(), [repo], {'example': content})

    def test_changed_metadata_rejected(self):
        for name in ('content.xml.xz', 'artifacts.xml.xz'):
            with self.subTest(name=name):
                repo, payloads = self.fixture()
                payloads[name] += b'tamper'
                with self.assertRaisesRegex(ValueError, 'metadata changed'):
                    PREP.validate_metadata(repo, payloads)

    def test_artifact_without_checksum_rejected(self):
        repo, payloads = self.fixture(checksum=False)
        with self.assertRaisesRegex(ValueError, 'lacks SHA-256'):
            PREP.validate_metadata(repo, payloads)

    def test_missing_artifact_rejected(self):
        repo, payloads = self.fixture(missing=True)
        with self.assertRaisesRegex(ValueError, 'missing artifact'):
            PREP.validate_metadata(repo, payloads)

    def test_unpinned_or_missing_root_rejected(self):
        repo, payloads = self.fixture()
        content = PREP.validate_metadata(repo, payloads)
        for version in ('0.0.0', '2.0.0'):
            with self.subTest(version=version), self.assertRaises(ValueError):
                PREP.validate_target(self.target(version), [repo], {'example': content})

    def test_unlocked_repository_rejected(self):
        repo, payloads = self.fixture()
        with self.assertRaisesRegex(ValueError, 'unlocked repository'):
            PREP.validate_target(self.target(url='https://other.invalid/'), [repo],
                                 {'example': PREP.validate_metadata(repo, payloads)})

    def test_both_descriptors_required(self):
        repo, payloads = self.fixture()
        del repo['metadata']['artifacts.xml.xz']
        with self.assertRaisesRegex(ValueError, 'Both content and artifact'):
            PREP.validate_metadata(repo, payloads)

    def test_alternate_descriptor_format_must_match(self):
        repo, payloads = self.fixture()
        out = io.BytesIO()
        with zipfile.ZipFile(out, 'w') as archive:
            archive.writestr('content.xml', '<repository><units/></repository>')
        payloads['content.jar'] = out.getvalue()
        repo['metadata']['content.jar'] = hashlib.sha256(out.getvalue()).hexdigest()
        with self.assertRaisesRegex(ValueError, 'formats disagree'):
            PREP.validate_metadata(repo, payloads)

    def test_target_cannot_omit_locked_repository(self):
        repo, payloads = self.fixture()
        other = dict(id='other', url='https://other.invalid/', metadata=repo['metadata'])
        with self.assertRaisesRegex(ValueError, 'repository sets differ'):
            PREP.validate_target(self.target(), [repo, other],
                                 {'example': PREP.validate_metadata(repo, payloads)})


if __name__ == '__main__':
    unittest.main()
