# Слишком большая вложенность операторов

Проверяет глубину вложенности операторов «Если», циклов и «Попытки»: глубокая вложенность затрудняет чтение кода (параметр, по умолчанию 4, как в BSL LS). Фиксируются операторы на уровнях глубже максимума. Проверка не имеет quick fix.

## См. также

- [BSL Language Server: NestedStatements](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedStatements/)
- Перенос диагностики BSL Language Server `NestedStatements`.
