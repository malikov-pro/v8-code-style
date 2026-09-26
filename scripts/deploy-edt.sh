#!/usr/bin/env bash
# Установка собранного v8-code-style в тестовую инсталляцию 1С:EDT через p2 director
# (без GUI «Установить новое ПО»). EDT во время установки должна быть ЗАКРЫТА.
#
# v8-cs поставляется в комплекте EDT: инсталляция тем же IU-именем с более
# свежим квалификатором обновляет комплектную копию на месте (проверяется по
# bundles.info). Откат к комплектной — «Проверить обновления» недоступен,
# откатывать переустановкой EDT или повторным деплоем комплектного p2.
#
# Использование:
#   bash scripts/deploy-edt.sh --edt "/путь/к/1cedt" --workspace "/путь/к/ws"
#   bash scripts/deploy-edt.sh --edt ".../1C_EDT 2026.2/1cedt" --workspace "$HOME/edt/2026_test"
#
# Репозиторий берётся последний собранный:
#   repositories/com.e1c.v8codestyle.repository/target/repository

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_REPO="$ROOT/repositories/com.e1c.v8codestyle.repository/target/repository"
FEATURE_IU="com.e1c.v8codestyle.feature.feature.group"
BUNDLE_PREFIX="com.e1c.v8codestyle"

REPO=""
EDT=""
WORKSPACE=""

usage() { sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; exit 0; }
log() { echo "[deploy] $*"; }
die() { echo "[deploy] ОШИБКА: $*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
    case "$1" in
        --edt)       [[ $# -ge 2 ]] || die "--edt требует значение"; EDT="$2"; shift 2 ;;
        --repo)      [[ $# -ge 2 ]] || die "--repo требует значение"; REPO="$2"; shift 2 ;;
        --workspace) [[ $# -ge 2 ]] || die "--workspace требует значение"; WORKSPACE="$2"; shift 2 ;;
        -h|--help)   usage ;;
        *)           die "Неизвестный флаг: $1 (см. --help)" ;;
    esac
done

[[ -d "$REPO" ]] || REPO="$DEFAULT_REPO"
[[ -d "$REPO" ]] || die "p2-репозиторий не найден: $REPO (сначала bash compile.sh)"

if [[ -z "$EDT" ]]; then
    BASE="$HOME/.local/share/1C/1cedtstart/installations"
    EDT="$(ls -d "$BASE"/1C_EDT*/*/ 2>/dev/null | tail -n 1 || true)"
    [[ -n "$EDT" ]] || die "инсталляция 1C_EDT не найдена в $BASE — передайте --edt"
fi

EDT="${EDT%/}"
[[ -x "$EDT/1cedt" ]] || die "$EDT/1cedt не найден или не исполняемый"

# Сверка JavaSE: версия EE собранных бандлов не должна быть выше JVM инсталляции
# (форк собирается как JavaSE-17 → встанет и в 2026.1 (JVM 17), и в 2026.2 (JVM 25)).
# В собранном jar BREE превращается Tycho в Require-Capability: osgi.ee…version=N.
TARGET_JVM="$(grep -o 'requiredJavaVersion=[0-9]*' "$EDT/1cedt.ini" 2>/dev/null | head -n 1 | cut -d= -f2 || true)"
CORE_JAR="$(ls "$REPO"/plugins/${BUNDLE_PREFIX}_[0-9]*.jar 2>/dev/null | head -n 1 || true)"
[[ -n "$CORE_JAR" ]] || die "в $REPO/plugins нет бандлов $BUNDLE_PREFIX (сначала bash compile.sh)"
MANIFEST_TXT="$(unzip -p "$CORE_JAR" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r' || true)"
ARTIFACT_BREE="$(echo "$MANIFEST_TXT" | grep -o 'Bundle-RequiredExecutionEnvironment: JavaSE-[0-9]*' | head -n 1 | grep -o '[0-9]*$' || true)"
[[ -n "$ARTIFACT_BREE" ]] || ARTIFACT_BREE="$(echo "$MANIFEST_TXT" | grep -o 'osgi.ee=JavaSE)(version=[0-9]*' | head -n 1 | grep -o '[0-9]*$' || true)"
if [[ -n "$TARGET_JVM" && -n "$ARTIFACT_BREE" ]] && (( ARTIFACT_BREE > TARGET_JVM )); then
    die "бандлы собраны под JavaSE-$ARTIFACT_BREE, а инсталляция требует JVM $TARGET_JVM — пересоберите под нужный профиль (bash compile.sh [--profile edt-2026.2])"
fi
log "JavaSE: бандлы EE ${ARTIFACT_BREE:-?}, инсталляция JVM ${TARGET_JVM:-?} — совместимо"

# EDT должна быть закрыта: p2 блокирует профиль. Проверяем по пути ИМЕННО этой
# инсталляции (имя процесса "1cedt" совпадает у всех инсталляций, -x ловит чужую).
if pgrep -f "$EDT/1cedt" >/dev/null 2>&1; then
    die "инсталляция $EDT запущена — закройте EDT и повторите"
fi

REPO_URI="file://$(cd "$REPO" && pwd)"
log "EDT        : $EDT"
log "Репозиторий: $REPO_URI"

# Логи прошлых прогонов мешают разбору: чистим журнал воркспейса до запуска EDT.
if [[ -n "$WORKSPACE" ]]; then
    WS_LOG_DIR="$WORKSPACE/.metadata"
    if [[ -d "$WS_LOG_DIR" ]] && compgen -G "$WS_LOG_DIR/.log*" >/dev/null; then
        rm -f "$WS_LOG_DIR"/.log*
        log "Очищен журнал EDT-воркспейса: $WS_LOG_DIR/.log"
    else
        log "Журнала воркспейса ещё нет (первый запуск?): $WORKSPACE"
    fi
else
    log "Воркспейс не задан (--workspace) — журнал не чистился"
fi

# Комплектная копия (не-корневой IU от oomph-сетапа) мешает установке точной
# версии фичи; снимаем, если получится — если p2 откажет из-за зависимостей
# продукта, установка ниже всё равно сработает как update (версия выше).
log "Снятие прежней копии (если допустимо)…"
"$EDT/1cedt" -nosplash \
    -application org.eclipse.equinox.p2.director \
    -uninstallIU "$FEATURE_IU" \
    -profileProperties org.eclipse.update.reconcile=true 2>/dev/null || \
    log "Прежнюю копию снять не удалось (зависимость продукта) — p2 выполнит update"

log "Установка IU: $FEATURE_IU"
"$EDT/1cedt" -nosplash \
    -application org.eclipse.equinox.p2.director \
    -repository "$REPO_URI" \
    -installIU "$FEATURE_IU" \
    -profileProperties org.eclipse.update.reconcile=true

# Инсталляции 1cedtstart хранят артефакты в общем пуле ~/.p2/pool, а список
# активных бандлов ведут в bundles.info — сверяем ВСЕ бандлы v8-cs по нему.
BUNDLES_INFO="$EDT/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info"
MISSING=0
for jar in "$REPO"/plugins/${BUNDLE_PREFIX}_*.jar; do
    [[ -f "$jar" ]] || continue
    base="$(basename "$jar")"
    if ! grep -qF "$base" "$BUNDLES_INFO"; then
        log "  нет в bundles.info: $base"
        MISSING=$((MISSING + 1))
    fi
done
[[ "$MISSING" -eq 0 ]] || die "$MISSING бандл(ов) не попали в bundles.info — деплой не зафиксировался"

# Дочистка пула: устаревшие jar v8-cs, на которые не ссылается ни один
# bundles.info известных инсталляций (комплектные 0.7.0 EDT 2026.1 остаются).
REFERENCED="$(mktemp)"
for info in "$BUNDLES_INFO" \
    "$HOME/.local/share/1C/1cedtstart/installations/"1C_EDT*/*/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info; do
    [[ -f "$info" ]] || continue
    grep -hF "$BUNDLE_PREFIX," "$info" >> "$REFERENCED" || true
done

REMOVED=0
for jar in "$HOME/.p2/pool/plugins/${BUNDLE_PREFIX}_"*.jar; do
    [[ -f "$jar" ]] || continue
    base="$(basename "$jar")"
    if ! grep -qF "$base" "$REFERENCED"; then
        rm -f "$jar"
        REMOVED=$((REMOVED + 1))
    fi
done
rm -f "$REFERENCED"
if [[ "$REMOVED" -gt 0 ]]; then
    log "Удалено устаревших jar из пула: $REMOVED"
fi

INSTALLED_VER="$(grep -m1 -o "${BUNDLE_PREFIX},[0-9][^,]*" "$BUNDLES_INFO")"
log "ГОТОВО: v8-cs обновлён в $EDT — $INSTALLED_VER"
log "Запустите эту EDT с воркспейсом и проверьте:"
log "  Справка → О платформе → Установленное ПО: com.e1c.v8codestyle* со свежим квалификатором;"
log "  Свойства проекта → Проверки: список проверок «Стандарты разработки» на месте;"
log "  журнал: ${WORKSPACE:-<ws>}/.metadata/.log — без новых ошибок com.e1c.v8codestyle."
