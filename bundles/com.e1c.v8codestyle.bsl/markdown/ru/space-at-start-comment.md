# Пробел в начале комментария

Проверяет, что текст комментария отделён от `//` пробелом — так комментарии легче читать.

Не фиксируются (как в BSL Language Server): комментарии-аннотации (параметр `commentsAnnotation`, по умолчанию `//@,//(c),//©`) и комментарии, похожие на закомментированный код.

Quick fix вставляет пробел после `//`.

## Неправильно

```bsl
//ОшибкаБезПробела
```

## Правильно

```bsl
// Ошибка без пробела
```

## См. также

- [BSL Language Server: SpaceAtStartComment](https://1c-syntax.github.io/bsl-language-server/diagnostics/SpaceAtStartComment/)
- Перенос диагностики BSL Language Server `SpaceAtStartComment`.
