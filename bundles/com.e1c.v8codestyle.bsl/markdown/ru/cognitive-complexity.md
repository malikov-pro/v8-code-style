# Когнитивная сложность метода выше допустимой

Проверяет когнитивную сложность метода: каждая структура «Если»/«ИначеЕсли»/цикл/«Попытка» добавляет 1 плюс свою глубину вложенности, каждое «И»/«ИЛИ» — 1 (упрощённый перенос BSL LS, порог по умолчанию 15). Сложный метод разбивайте на несколько. Проверка не имеет quick fix.

## См. также

- [BSL Language Server: CognitiveComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/CognitiveComplexity/)
- Перенос диагностики BSL Language Server `CognitiveComplexity`.
