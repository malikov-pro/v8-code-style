#!/usr/bin/env bash
# Точечный прогон одного itest-класса переноса без полного reactor-прогона.
# Использование: bash scripts/test-check.sh ApkYoLetterCheckTest
set -euo pipefail
[ $# -ge 1 ] || { echo "usage: $0 <TestClassName>"; exit 2; }
TEST="$1"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVN="${MVN:-$HOME/tools/apache-maven-3.9.9/bin/mvn}"
cd "$ROOT"
"$MVN" -B clean verify -Dtest="$TEST" -DfailIfNoTests=false \
  -Dtycho.localArtifacts=ignore -Dtycho.p2.httptransport.type=JavaUrl \
  -pl targets/edt-2026.1,bundles/com.e1c.v8codestyle,bundles/com.e1c.v8codestyle.ui,bundles/com.e1c.v8codestyle.md,bundles/com.e1c.v8codestyle.md.ui,bundles/com.e1c.v8codestyle.form,bundles/com.e1c.v8codestyle.bsl,bundles/com.e1c.v8codestyle.bsl.ui,bundles/com.e1c.v8codestyle.autosort,bundles/com.e1c.v8codestyle.autosort.ui,bundles/com.e1c.v8codestyle.ql,bundles/com.e1c.v8codestyle.right,docs,features/com.e1c.v8codestyle.feature,repositories/com.e1c.v8codestyle.repository,tests/com.e1c.v8codestyle.bsl.itests
