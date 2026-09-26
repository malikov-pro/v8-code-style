#!/usr/bin/env bash
# Канонический локальный сценарий сборки v8-code-style (форк malikov-pro).
# Воспроизводит то, что делает апстримный CI (.github/workflows/build.yml):
#   mvn clean verify -Dtycho.localArtifacts=ignore -Dtycho.p2.httptransport.type=JavaUrl
# затем печатает путь к готовому p2-артефакту для установки в EDT.
#
# Профили платформ:
#   EDT 2026.1 (основная, по умолчанию) — targets/edt-2026.1 (ruby/2026.1)
#   EDT 2026.2 (пре-релиз)              — targets/default (ruby/2026.2), флаг --profile edt-2026.2
# Исходник один на обе платформы: бандлы собираются как JavaSE-17 и встают
# в EDT 2026.1 (JVM 17) и EDT 2026.2 (JVM 25). JDK 25 — только тулчейн сборки.
#
# Использование:
#   bash compile.sh                    # EDT 2026.1 (основная цель)
#   bash compile.sh --profile edt-2026.2   # EDT 2026.2 (пре-релиз)
#   bash compile.sh --full             # в точности CI: -PSDK,find-bugs (дольше)
#   bash compile.sh --skip-tests       # без itests (быстрая проверка сборки)
#   bash compile.sh --java-home ... --maven-home ...

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$ROOT/repositories/com.e1c.v8codestyle.repository/target"

FULL=0
SKIP_TESTS=0
PROFILE=""

usage() {
    sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
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

# --- toolchain (без sudo; живёт в ~/tools) -----------------------------------
if [[ -z "${JAVA_HOME_ARG:-}" ]]; then
    for cand in "$HOME/tools/jdk-25.0.4.1+1" /opt/1C/1CE/components/axiom-jdk-full-17.0.16+12-x86_64; do
        [[ -x "$cand/bin/java" ]] || continue
        # EE профиля — JavaSE-25: JDK 17 не подходит, ищем только его
        "$cand/bin/java" -version 2>&1 | grep -q 'version "25' || continue
        JAVA_HOME_ARG="$cand"
        break
    done
fi

if [[ -n "${JAVA_HOME_ARG:-}" ]]; then
    [[ -x "$JAVA_HOME_ARG/bin/java" ]] || die "bin/java не найден в --java-home: $JAVA_HOME_ARG"
    export JAVA_HOME="$JAVA_HOME_ARG"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -n "${MAVEN_HOME_ARG:-}" ]]; then
    [[ -x "$MAVEN_HOME_ARG/bin/mvn" ]] || die "bin/mvn не найден в --maven-home: $MAVEN_HOME_ARG"
    MVN="$MAVEN_HOME_ARG/bin/mvn"
elif [[ -x "$HOME/tools/apache-maven-3.9.9/bin/mvn" ]]; then
    MVN="$HOME/tools/apache-maven-3.9.9/bin/mvn"
else
    command -v mvn >/dev/null || die "mvn не найден в PATH, передайте --maven-home (нужен Maven 3.9.9+)"
    MVN="$(command -v mvn)"
fi

JAVA_VER="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 || java -version 2>&1 | head -n 1)"
log "JAVA_HOME : ${JAVA_HOME:-<из PATH>} ($JAVA_VER)"
log "Maven     : $MVN"

# --- build -------------------------------------------------------------------
case "$PROFILE" in
    ""|edt-2026.1|edt-2026.2) ;; # пусто = EDT 2026.1 (основная)
    *) die "неизвестный профиль: $PROFILE (допустимо: edt-2026.1, edt-2026.2)" ;;
esac
CMD=(clean verify --batch-mode -Dtycho.localArtifacts=ignore -Dtycho.p2.httptransport.type=JavaUrl)
[[ -n "$PROFILE" ]] && CMD+=(-P"$PROFILE")
[[ "$SKIP_TESTS" -eq 1 ]] && CMD+=(-DskipTests)
[[ "$FULL" -eq 1 ]] && CMD+=(-PSDK,find-bugs)

log "Запуск: mvn ${CMD[*]} (профиль: ${PROFILE:-edt-2026.1})"
(cd "$ROOT" && "$MVN" "${CMD[@]}")

# --- artifact ----------------------------------------------------------------
ZIP="$(ls -t "$REPO_DIR"/*.zip 2>/dev/null | head -n 1 || true)"
[[ -n "$ZIP" ]] || die "p2-zip не найден в $REPO_DIR"

log "ГОТОВО: p2-репозиторий:"
echo "  $ZIP"
if [[ -n "$PROFILE" ]]; then
    echo "  профиль: $PROFILE"
else
    echo "  профиль: edt-2026.1 (по умолчанию)"
fi
echo "Установка: bash scripts/deploy-edt.sh --edt <инсталляция> --workspace <ws>"
