#!/usr/bin/env bash
# Смоук-петля порта проверки: сборка → установка в EDT → запуск EDT на /apk.
# После запуска EDT проверка маркеров — через MCP (clean_project +
# get_project_errors) силами агента; ожидания — в шапках модулей-примеров
# АПК.ОшибкиДляПроверки (ош_ПримерЕ, ош_ПримерФранцКавычки, ...).
#
# Использование:
#   bash scripts/smoke-edt.sh                 # сборка+установка+запуск EDT
#   bash scripts/smoke-edt.sh --skip-build    # установка+запуск из готового repository
#   bash scripts/smoke-edt.sh --skip-deploy   # только запуск EDT (уже установлено)
#
# Требования: EDT при установке должна быть ЗАКРЫТА (скрипт проверит).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDT="${EDT_HOME:-$HOME/.local/share/1C/1cedtstart/installations/1C_EDT 2026.2/1cedt}"
WS="${WS_HOME:-$HOME/edt/apk_2026}"
RUNTIME_JAVA_HOME="${EDT_JAVA_HOME:-/opt/1C/1CE/components/axiom-jdk-full-25.0.4+5-x86_64}"

SKIP_BUILD=0; SKIP_DEPLOY=0
for arg in "$@"; do
  case "$arg" in
    --skip-build)  SKIP_BUILD=1 ;;
    --skip-deploy) SKIP_DEPLOY=1 ;;
    *) echo "Неизвестный флаг: $arg" >&2; exit 2 ;;
  esac
done

log()  { echo "[smoke] $*"; }
die()  { echo "[smoke] ОШИБКА: $*" >&2; exit 1; }

workbench_running() {
  # Воркбенч EDT = java с equinox.launcher (демон 1cedtstart не в счёт).
  pgrep -f "[e]quinox.launcher" >/dev/null 2>&1
}

if [[ "$SKIP_DEPLOY" -eq 0 ]] && workbench_running; then
  die "EDT запущена — закрой её перед установкой (деплой меняет jar в пуле)."
fi

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  log "Сборка (без itests — быстрый цикл; полный гейт гонять отдельно)"
  bash "$ROOT/compile.sh" --skip-tests \
    > /tmp/opencode/smoke-build.log 2>&1 || { tail -20 /tmp/opencode/smoke-build.log >&2; die "сборка упала (лог: /tmp/opencode/smoke-build.log)"; }
  log "Сборка OK"
fi

if [[ "$SKIP_DEPLOY" -eq 0 ]]; then
  log "Установка в EDT (p2 director)"
  JAVA_HOME="$RUNTIME_JAVA_HOME" PATH="$RUNTIME_JAVA_HOME/bin:$PATH" bash "$ROOT/scripts/deploy-edt.sh" --edt "$EDT" --workspace "$WS"
fi

if workbench_running; then
  log "EDT уже запущена — пропускаю старт"
else
  log "Запуск EDT на воркспейсе $WS (полная Java 25)"
  ( cd "$EDT" && NO_AT_BRIDGE=1 nohup ./1cedt -data "$WS" -vm "$RUNTIME_JAVA_HOME/bin/java" \
      > /tmp/opencode/smoke-edt.out 2>&1 & )
fi

log "Жду готовности MCP-сервера (появление проектов в логе)..."
for i in $(seq 1 60); do
  if grep -q "Workbench: Http server" /tmp/opencode/smoke-edt.out 2>/dev/null \
     || grep -q "refreshing finished\|Workspace restoration" "$WS/.metadata/.log" 2>/dev/null; then
    break
  fi
  sleep 5
done

cat <<'EOF'

[smoke] EDT запускается. Когда MCP-сервер поднимется (1-3 мин), проверка:
  агентом (рекомендуется): clean_project + get_project_errors по
  АПК.ОшибкиДляПроверки, сверка с шапками модулей-примеров.
  ВАЖНО: после установки новой версии маркеры новых проверок появляются
  только после clean build проекта (revalidate недостаточно).
EOF
