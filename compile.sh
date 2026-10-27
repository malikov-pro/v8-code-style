#!/usr/bin/env bash
# Канонический локальный сценарий сборки v8-code-style (форк malikov-pro).
# Воспроизводит то, что делает апстримный CI (.github/workflows/build.yml):
#   mvn clean verify -Dtycho.localArtifacts=ignore -Dtycho.p2.httptransport.type=JavaUrl
# затем печатает путь к готовому p2-артефакту для установки в EDT.
#
# Поддерживается только EDT 2026.2.2 (linux/gtk/x86_64), JavaSE-25.
# Приватная платформа: scripts/prepare-platform.py, docs/build-edt-2026.2.2.md.
#
# Использование:
#   bash compile.sh                    # EDT 2026.2.2
#   bash compile.sh --profile edt-2026.2   # совместимый алиас, та же платформа
#   bash compile.sh --full             # в точности CI: -PSDK,find-bugs (дольше)
#   bash compile.sh --skip-tests       # без itests (быстрая проверка сборки)
#   bash compile.sh --java-home ... --maven-home ...

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$ROOT/repositories/com.e1c.v8codestyle.repository/target"

FULL=0
SKIP_TESTS=0
PROFILE="edt-2026.2"

usage() {
    sed -n '2,/^set -euo pipefail/{ /^set -euo pipefail/d; s/^# \{0,1\}//; p; }' "$0"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --full)         FULL=1; shift ;;
        --skip-tests)   SKIP_TESTS=1; shift ;;
        --profile)      [[ $# -ge 2 ]] || { echo "--profile требует значение" >&2; exit 1; }; PROFILE="$2"; shift 2 ;;
        --profile=*)    PROFILE="${1#*=}"; shift ;;
        --java-home)    [[ $# -ge 2 ]] || { echo "--java-home требует значение" >&2; exit 1; }; JAVA_HOME_ARG="$2"; shift 2 ;;
        --java-home=*)  JAVA_HOME_ARG="${1#*=}"; shift ;;
        --maven-home)   [[ $# -ge 2 ]] || { echo "--maven-home требует значение" >&2; exit 1; }; MAVEN_HOME_ARG="$2"; shift 2 ;;
        --maven-home=*) MAVEN_HOME_ARG="${1#*=}"; shift ;;
        -h|--help)      usage ;;
        *)              echo "Неизвестный флаг: $1" >&2; usage >&2; exit 2 ;;
    esac
done

log()  { echo "[compile] $*"; }
die()  { echo "[compile] ОШИБКА: $*" >&2; exit 1; }

# Reject obsolete profiles before platform preparation/downloads.
[[ "$PROFILE" == edt-2026.2 ]] || die "поддерживается только EDT 2026.2.2 (--profile edt-2026.2)"
source "$ROOT/scripts/build-environment.sh"
v8cs_build_environment
log "JAVA_HOME : $JAVA_HOME"
log "Maven     : $MVN"

# --- build -------------------------------------------------------------------
CMD=(clean verify --batch-mode -Dtycho.localArtifacts=ignore -Dtycho.p2.httptransport.type=JavaUrl)
[[ "$SKIP_TESTS" -eq 1 ]] && CMD+=(-DskipTests)
[[ "$FULL" -eq 1 ]] && CMD+=(-PSDK,find-bugs)

log "Запуск: mvn ${CMD[*]} (EDT 2026.2.2)"
(cd "$ROOT" && "$MVN" "${CMD[@]}")

# --- artifact ----------------------------------------------------------------
ZIP="$(ls -t "$REPO_DIR"/*.zip 2>/dev/null | head -n 1 || true)"
[[ -n "$ZIP" ]] || die "p2-zip не найден в $REPO_DIR"

log "ГОТОВО: p2-репозиторий:"
echo "  $ZIP"
echo "  платформа: EDT 2026.2.2 / Java 25"
echo "Установка: bash scripts/deploy-edt.sh --edt <инсталляция> --workspace <ws>"
