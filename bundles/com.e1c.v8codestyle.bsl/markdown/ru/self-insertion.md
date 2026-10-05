# Вставка коллекции в саму себя

Проверяет, что коллекция не вставляется (добавляется) в саму себя: вставка коллекции в саму себя приводит к возникновению циклических ссылок.

## Неправильно

```bsl
Список.Добавить(Список);
```

## Правильно

```bsl
Список.Добавить(Элемент);
```

## См. также

- [BSL Language Server: SelfInsertion](https://1c-syntax.github.io/bsl-language-server/diagnostics/SelfInsertion/)
- [Поиск циклических ссылок](https://its.1c.ru/db/metod8dev#content:5859:hdoc)
- Перенос диагностики BSL Language Server `SelfInsertion`.
