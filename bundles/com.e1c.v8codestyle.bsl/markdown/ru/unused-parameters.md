# Неиспользуемый параметр метода

Проверяет, что все параметры метода используются в его теле. Неиспользуемый параметр вводит в заблуждение: вызывающие обязаны его передавать без пользы (как в BSL LS). Проверка не имеет quick fix: удаление параметра меняет сигнатуру и вызовы.

## См. также

- [BSL Language Server: UnusedParameters](https://1c-syntax.github.io/bsl-language-server/diagnostics/UnusedParameters/)
- Перенос диагностики BSL Language Server `UnusedParameters`.
