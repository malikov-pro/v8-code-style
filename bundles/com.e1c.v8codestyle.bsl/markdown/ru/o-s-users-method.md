# Использование метода ПользователиОС

Проверяет использование метода «ПользователиОС». Получение информации о пользователях операционной системы потенциально опасно: требует обоснования и ограничения доступа.

Методы объекта не проверяются — как в BSL Language Server. Проверка не имеет quick fix.

## Неправильно

```bsl
Пользователи = ПользователиОС();
```

## См. также

- [BSL Language Server: OSUsersMethod](https://1c-syntax.github.io/bsl-language-server/diagnostics/OSUsersMethod/)
- Перенос диагностики BSL Language Server `OSUsersMethod`.
