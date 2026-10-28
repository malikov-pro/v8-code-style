#!/usr/bin/env python3
"""Verify public p2 metadata against the reviewed lock before invoking Tycho.

No private EDT installation, archive or credentials are required. Tycho downloads
artifacts from the public sites using their locked p2 SHA-256 checksums.
"""
import argparse
import concurrent.futures
import hashlib
import io
import json
import lzma
from pathlib import Path
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent.parent
LOCK = ROOT / 'targets/default/public-platform-lock.json'
TARGET = ROOT / 'targets/default/default.target'


def decode(name, body):
    if name.endswith('.xml.xz'):
        return lzma.decompress(body)
    if name.endswith('.jar'):
        with zipfile.ZipFile(io.BytesIO(body)) as archive:
            return archive.read(name.removesuffix('.jar') + '.xml')
    return body


def validate_metadata(repository, payloads):
    documents = {}
    for name, expected in repository['metadata'].items():
        body = payloads[name]
        if hashlib.sha256(body).hexdigest() != expected:
            raise ValueError('Public p2 metadata changed: ' + repository['id'] + '/' + name)
        kind = name.split('.')[0]
        document = ET.fromstring(decode(name, body))
        if kind in documents and ET.tostring(documents[kind]) != ET.tostring(document):
            raise ValueError('Public p2 descriptor formats disagree')
        documents[kind] = document
    if set(documents) != {'content', 'artifacts'}:
        raise ValueError('Both content and artifact metadata must be locked')
    artifacts = {}
    for artifact in documents['artifacts'].findall('./artifacts/artifact'):
        key = tuple(artifact.get(k) for k in ('classifier', 'id', 'version'))
        checksums = [p.get('value') for p in artifact.findall('./properties/property')
                     if p.get('name') == 'download.checksum.sha-256']
        if len(checksums) != 1 or not re.fullmatch(r'[0-9a-f]{64}', checksums[0]):
            raise ValueError('Public p2 artifact lacks SHA-256: ' + repository['id'])
        if key in artifacts:
            raise ValueError('Duplicate public p2 artifact')
        artifacts[key] = checksums[0]
    for artifact in documents['content'].findall('./units/unit/artifacts/artifact'):
        key = tuple(artifact.get(k) for k in ('classifier', 'id', 'version'))
        if key not in artifacts:
            raise ValueError('Public p2 content references a missing artifact')
    return documents['content']


def validate_target(target, repositories, contents):
    expected = {r['url']: r['id'] for r in repositories}
    used = set()
    for location in ET.fromstring(target).findall('./locations/location'):
        repo = location.find('repository')
        if repo is None or repo.get('location') not in expected:
            raise ValueError('Target contains an unlocked repository')
        url = repo.get('location')
        used.add(url)
        units = {(u.get('id'), u.get('version')) for u in contents[expected[url]].findall('./units/unit')}
        for unit in location.findall('unit'):
            if unit.get('version') in (None, '0.0.0'):
                raise ValueError('Public target root must have an exact version')
            if (unit.get('id'), unit.get('version')) not in units:
                raise ValueError('Target root not found in locked public content: ' + unit.get('id'))
    if used != set(expected):
        raise ValueError('Target and public lock repository sets differ')


def fetch_repository(repository):
    url = repository['url']
    if urllib.parse.urlparse(url).scheme != 'https':
        raise ValueError('Public platform requires HTTPS')
    payloads = {}
    for name in repository['metadata']:
        if name not in ('content.xml.xz', 'artifacts.xml.xz', 'content.jar', 'artifacts.jar',
                        'content.xml', 'artifacts.xml'):
            raise ValueError('Unexpected public p2 descriptor name')
        with urllib.request.urlopen(url + name, timeout=120) as response:
            if urllib.parse.urlparse(response.url).scheme != 'https':
                raise ValueError('Public p2 redirected outside HTTPS')
            payloads[name] = response.read()
    return repository['id'], validate_metadata(repository, payloads)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--lock', type=Path, default=LOCK)
    parser.add_argument('--target', type=Path, default=TARGET)
    args = parser.parse_args()
    repositories = json.loads(args.lock.read_text())['repositories']
    if len({r['id'] for r in repositories}) != len(repositories):
        raise ValueError('Duplicate public p2 repository id')
    if len({r['url'] for r in repositories}) != len(repositories):
        raise ValueError('Duplicate public p2 repository URL')
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        contents = dict(pool.map(fetch_repository, repositories))
    validate_target(args.target.read_bytes(), repositories, contents)
    print('Public p2 verified: locked content/artifacts, exact target roots, SHA-256 artifact metadata')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, ET.ParseError, lzma.LZMAError, zipfile.BadZipFile) as error:
        raise SystemExit('Public platform verification failed: ' + str(error)) from None
