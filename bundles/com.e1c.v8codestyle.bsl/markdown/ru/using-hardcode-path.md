# Хранение путей к файлам в коде

Проверяет строковые литералы на пути к файлам и каталогам Windows/Unix, например `"C:\Program Files (x86)\1cv8"` или `"/etc/". Хардкодный путь зависит от машины, на которой выполняется код — используйте настройки, константы или стандартные функции.

URL (`ftp://`, `http://`, `https://`) не проверяются. Unix-пути фиксируются только для стандартных корневых каталогов (параметр `searchWordsStdPathsUnix`).

Проверка не имеет quick fix: способ хранения пути — решение пользователя.

## Неправильно

```bsl
Путь = "C:\Program Files (x86)\1cv8\";
```

## Правильно

```bsl
Путь = ПолучитьСтандартныйПуть(ProgramFilesPath) + "1cv8\";
```

## См. также

- [BSL Language Server: UsingHardcodePath](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingHardcodePath/)
- Перенос диагностики BSL Language Server `UsingHardcodePath`.
