#!/usr/bin/env python3
"""Verify/import a private locked p2 snapshot; stdout is its URI for Tycho.

No platform binaries or credentials enter git. Metadata and a canonical complete
artifact SHA-256 inventory pin the snapshot (some p2 entries have no checksum).
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import tarfile
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent.parent
LOCK = ROOT / 'targets/default/platform-lock.json'


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def artifact_files(repository, lock):
    for name, expected in lock['metadata'].items():
        if not (repository / name).is_file() or digest(repository / name) != expected:
            raise ValueError(f'Platform metadata hash mismatch: {name}')
    with zipfile.ZipFile(repository / 'artifacts.jar') as archive:
        metadata = ET.fromstring(archive.read('artifacts.xml'))
    artifacts = metadata.find('artifacts')
    if len(artifacts) != lock['artifacts']:
        raise ValueError('Unexpected platform artifact count')
    files = dict(lock['metadata'])
    for artifact in artifacts:
        folder = {'osgi.bundle': 'plugins', 'org.eclipse.update.feature': 'features'}.get(
            artifact.get('classifier'))
        identifier, version = artifact.get('id'), artifact.get('version')
        if folder is None or any('/' in s or '\\' in s or s in ('.', '..') for s in (identifier, version)):
            raise ValueError('Unsupported platform artifact path')
        properties = {p.get('name'): p.get('value') for p in artifact.findall('properties/property')}
        expected = properties.get('download.checksum.sha-256')
        files[f'{folder}/{identifier}_{version}.jar'] = expected
    return files


def verify(repository, lock):
    files = artifact_files(repository, lock)
    inventory = []
    for name, expected in files.items():
        path = repository / name
        if not path.is_file():
            raise ValueError(f'Missing platform artifact: {name}')
        actual = digest(path)
        if expected and actual != expected:
            raise ValueError(f'Platform artifact hash mismatch: {name}')
        if name not in lock['metadata']:
            inventory.append(f'{actual}  {name}\n')
    if hashlib.sha256(''.join(sorted(inventory, key=lambda line: line.split('  ', 1)[1])).encode()).hexdigest() != lock['inventory_sha256']:
        raise ValueError('Platform inventory hash mismatch')
    return files


def copy_snapshot(source, destination, lock):
    for name in lock['metadata']:
        shutil.copy2(source / name, destination / name)
    files = artifact_files(destination, lock)
    for name in files:
        if name in lock['metadata']:
            continue
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        try:
            target.hardlink_to(source / name)
        except OSError:
            shutil.copy2(source / name, target)
    verify(destination, lock)


def extract_snapshot(archive, destination):
    # Only regular files in the expected p2 layout; no links, traversal or devices.
    with tarfile.open(archive, 'r:*') as tar:
        for member in tar:
            name = Path(member.name)
            if name.is_absolute() or '..' in name.parts:
                raise ValueError('Unsafe platform archive path')
            if member.isdir():
                continue
            if not member.isfile() or (str(name) not in ('content.jar', 'artifacts.jar')
                                      and name.parts[0] not in ('plugins', 'features')):
                raise ValueError('Unexpected platform archive entry')
            target = destination / name
            if target.exists():
                raise ValueError('Duplicate platform archive entry')
            target.parent.mkdir(parents=True, exist_ok=True)
            with tar.extractfile(member) as source, target.open('wb') as output:
                shutil.copyfileobj(source, output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=os.environ.get('EDT_P2_SOURCE'))
    parser.add_argument('--archive', type=Path, default=os.environ.get('EDT_P2_ARCHIVE'))
    parser.add_argument('--destination', type=Path, default=os.environ.get(
        'EDT_P2_CACHE', str(Path.home() / '.cache/v8-code-style/p2-2026.2.2')))
    parser.add_argument('--verify', type=Path, help='Verify an existing snapshot without importing it')
    args = parser.parse_args()
    lock = json.loads(LOCK.read_text())
    if args.verify:
        verify(args.verify, lock)
        print(args.verify.resolve().as_uri() + '/')
        return
    destination = args.destination.expanduser().absolute()
    if destination.exists():
        verify(destination, lock)
        print(destination.as_uri() + '/')
        return
    url = os.environ.get('EDT_P2_ARCHIVE_URL')
    if not (args.source or args.archive or url):
        parser.error('Private EDT p2 snapshot required: set EDT_P2_SOURCE, EDT_P2_ARCHIVE or '
                     'EDT_P2_ARCHIVE_URL; see docs/build-edt-2026.2.2.md')
    if sum(bool(x) for x in [args.source, args.archive, url]) != 1:
        parser.error('Choose exactly one snapshot source')
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.p2-import-', dir=destination.parent) as temporary:
        staging = Path(temporary) / 'repository'
        staging.mkdir()
        if args.source:
            copy_snapshot(args.source.expanduser().resolve(), staging, lock)
        else:
            archive = args.archive
            if url:
                if not url.startswith('https://'):
                    raise ValueError('Private platform download requires HTTPS')
                archive = Path(temporary) / 'platform.tar'
                # Do not put signed URLs, tokens or headers into output/evidence.
                with urllib.request.urlopen(url, timeout=120) as response, archive.open('wb') as output:
                    shutil.copyfileobj(response, output)
            extract_snapshot(archive, staging)
            verify(staging, lock)
        staging.rename(destination)
    print(destination.as_uri() + '/')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, tarfile.TarError, zipfile.BadZipFile, ET.ParseError):
        # A download exception can contain a signed URL; never echo it.
        raise SystemExit('Platform preparation failed: source/archive unreadable or locked snapshot mismatch. '
                         'No existing snapshot was modified.') from None
