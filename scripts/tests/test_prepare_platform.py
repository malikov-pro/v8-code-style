"""Infrastructure unit tests; no EDT, network or Maven required."""
import hashlib
import importlib.util
import io
from pathlib import Path
import tarfile
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location('prepare_platform', Path(__file__).parents[1] / 'prepare-platform.py')
PREP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREP)


class PlatformPreparationTest(unittest.TestCase):
    def fixture(self, root):
        (root / 'plugins').mkdir()
        (root / 'plugins/example_1.0.0.jar').write_bytes(b'bundle')
        (root / 'plugins/no-checksum_1.0.0.jar').write_bytes(b'second')
        sha = PREP.digest(root / 'plugins/example_1.0.0.jar')
        xml = ('<repository><artifacts>'
               '<artifact classifier="osgi.bundle" id="example" version="1.0.0">'
               f'<properties><property name="download.checksum.sha-256" value="{sha}"/></properties>'
               '</artifact><artifact classifier="osgi.bundle" id="no-checksum" version="1.0.0"/>'
               '</artifacts></repository>')
        for name, data in [('artifacts', xml), ('content', '<repository/>')]:
            with zipfile.ZipFile(root / (name + '.jar'), 'w') as archive:
                archive.writestr(name + '.xml', data)
        inventory = ''.join(f'{PREP.digest(p)}  {p.relative_to(root).as_posix()}\n'
                            for p in sorted((root / 'plugins').iterdir()))
        return dict(metadata={n: PREP.digest(root/n) for n in ['content.jar', 'artifacts.jar']},
                    artifacts=2, inventory_sha256=hashlib.sha256(inventory.encode()).hexdigest())

    def test_verified_snapshot_import(self):
        with tempfile.TemporaryDirectory() as temporary:
            source, destination = Path(temporary)/'source', Path(temporary)/'destination'
            source.mkdir()
            destination.mkdir()
            lock = self.fixture(source)
            PREP.copy_snapshot(source, destination, lock)
            self.assertEqual(4, len(PREP.verify(destination, lock)))

    def test_metadata_tampering_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            lock = self.fixture(root)
            (root/'content.jar').write_bytes(b'other metadata')
            with self.assertRaisesRegex(ValueError, 'metadata hash mismatch'):
                PREP.verify(root, lock)

    def test_artifact_without_p2_checksum_still_verified(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            lock = self.fixture(root)
            (root/'plugins/no-checksum_1.0.0.jar').write_bytes(b'tampered')
            with self.assertRaisesRegex(ValueError, 'inventory hash mismatch'):
                PREP.verify(root, lock)

    def test_missing_artifact_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            lock = self.fixture(root)
            (root/'plugins/example_1.0.0.jar').unlink()
            with self.assertRaisesRegex(ValueError, 'Missing platform artifact'):
                PREP.verify(root, lock)

    def test_regular_archive_import(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source, destination = root/'source', root/'destination'
            source.mkdir()
            destination.mkdir()
            lock = self.fixture(source)
            archive = root/'platform.tar'
            with tarfile.open(archive, 'w', dereference=True) as tar:
                for name in PREP.verify(source, lock):
                    tar.add(source/name, arcname=name)
            PREP.extract_snapshot(archive, destination)
            PREP.verify(destination, lock)

    def test_unsafe_archive_entries_rejected(self):
        for name, kind in [('../escape', tarfile.REGTYPE), ('/absolute', tarfile.REGTYPE),
                           ('plugins/link.jar', tarfile.SYMTYPE), ('plugins/hard.jar', tarfile.LNKTYPE),
                           ('unexpected.txt', tarfile.REGTYPE)]:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                archive = root/'bad.tar'
                with tarfile.open(archive, 'w') as tar:
                    item = tarfile.TarInfo(name)
                    item.type = kind
                    item.linkname = '../escape'
                    item.size = 0
                    tar.addfile(item, io.BytesIO())
                with self.assertRaises(ValueError):
                    PREP.extract_snapshot(archive, root/'destination')


if __name__ == '__main__':
    unittest.main()
