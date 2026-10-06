# Хранение ip-адресов в коде

Проверяет строковые литералы на IPv4/IPv6-адреса, например `"192.168.0.1"`. Хардкодный адрес зависит от окружения, в котором выполняется код — используйте настройки или константы.

Не фиксируется (как в BSL Language Server): URL; значения, похожие на версии (параметр `searchPopularVersionExclusion`); операторы со словами-исключениями (параметр `searchWordsExclusion`: Верси/Version/Драйвер/Driver/…).

Проверка не имеет quick fix: способ хранения адреса — решение пользователя.

## Неправильно

```bsl
АдресСервера = "192.168.0.1";
```

## Правильно

```bsl
АдресСервера = ПолучитьАдресСервера();
```

## См. также

- [BSL Language Server: UsingHardcodeNetworkAddress](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingHardcodeNetworkAddress/)
- Перенос диагностики BSL Language Server `UsingHardcodeNetworkAddress`.
