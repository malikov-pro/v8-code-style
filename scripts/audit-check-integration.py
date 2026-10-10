#!/usr/bin/env python3
"""Check registry structure and delivery of checks from feature commits.

Usage: python3 scripts/audit-check-integration.py --source-commit <sha> ...
Feature ancestry alone is insufficient after cherry-picks; check the actual
source files and registrations instead. No files are modified.
"""

import argparse
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


def audit(root, commits):
    errors = []
    registered = set()
    for plugin in sorted((root / "bundles").glob("*/plugin.xml")):
        try:
            checks = ET.parse(plugin).findall(".//check")
        except ET.ParseError as error:
            errors.append(f"{plugin.relative_to(root)}: {error}")
            continue
        classes = set()
        for check in checks:
            entry = check.get("class", "")
            if not entry or any(char.isspace() for char in entry) or entry.count(":") > 1 or list(check):
                errors.append(f"{plugin.relative_to(root)}: malformed check {check.attrib}")
                continue
            class_name = entry.split(":")[-1]
            if class_name in classes:
                errors.append(f"{plugin.relative_to(root)}: duplicate class {class_name}")
            classes.add(class_name)
            source = plugin.parent / "src" / (class_name.replace(".", "/") + ".java")
            if not source.is_file():
                errors.append(f"{plugin.relative_to(root)}: source missing for {class_name}")
            registered.add(source.relative_to(root).as_posix())
        print(f"{plugin.parent.name}: {len(checks)} checks")

    for commit in commits:
        try:
            output = subprocess.check_output(
                ["git", "diff-tree", "--no-commit-id", "--name-only", "--diff-filter=A", "-r", commit],
                cwd=root, text=True,
            )
        except subprocess.CalledProcessError:
            errors.append(f"Cannot read source commit {commit}")
            continue
        candidates = [
            name for name in output.splitlines()
            if name.startswith("bundles/") and "/src/" in name and name.endswith("Check.java")
        ]
        if not candidates:
            errors.append(f"{commit}: no added check classes; verify the source commit")
        for name in candidates:
            if not (root / name).is_file():
                errors.append(f"{commit}: check source not delivered: {name}")
            elif name not in registered:
                errors.append(f"{commit}: check not registered: {name}")
        print(f"{commit}: {len(candidates)} expected check classes")

    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    return 1 if errors else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-commit", action="append", default=[], metavar="SHA")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    return audit(root, args.source_commit)


if __name__ == "__main__":
    sys.exit(main())
