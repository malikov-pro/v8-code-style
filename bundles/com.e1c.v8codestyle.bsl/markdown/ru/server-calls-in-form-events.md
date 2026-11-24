# Серверный вызов в событии формы

Включена по умолчанию, ERROR / CRITICAL. Вызов локального метода с
`&НаСервере` из `ПриАктивизацииСтроки`/`НачалоВыбора` (`OnActivateRow`/
`OnStartChoice`) добавляет серверный вызов при каждой активизации строки или
начале выбора.

Вызовы `&НаСервереБезКонтекста`/`&AtServerNoContext` по контракту pinned LS
разрешены и не помечаются. Разрешаются только локальные методы модуля.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/ServerCallsInFormEvents/)
