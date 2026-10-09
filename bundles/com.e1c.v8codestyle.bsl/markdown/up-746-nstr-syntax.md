# Syntax of the localized string of the NStr function

Checks that the string literal passed to the `NStr` (`НСтр`) function matches the localized string syntax (standard 761): one or more entries of a language code, the equal sign and the message text enclosed in single quotes, separated by commas. Single quotes inside the message text must be doubled.

A language code is 2-5 letters with an optional region suffix, for example `ru`, `en`, `zh-CN`.

## Non-compliant

```bsl
// The message text is in double quotes instead of single quotes.
Текст = НСтр("ru=""Текст в двойных кавычках""");

// The message text is without quotes.
Текст = НСтр("ru=Текст");

// The language code is missing.
Текст = НСтр("Просто текст");

// The message text is not closed or a single quote inside the text is not doubled.
Текст = НСтр("ru='Незакрытый текст");

// A separator without the next language entry.
Текст = НСтр("ru='Текст',");
```

## Compliant

```bsl
Текст = НСтр("ru='Текст сообщения'");
Текст = НСтр("ru = 'Текст о ''цитатах'''");
Текст = НСтр("ru = 'Текст', en = 'Text'");
```

The first parameter that is not a string literal (including a nested `NStr` call — double localization) is reported by the **NStr string literal format** check and is not checked here.

## See also

- [Standard 761. Interface texts in code: localization requirements](https://its.1c.ru/db/v8std#content:761:hdoc:1)
- Ported from the upstream issue [1C-Company/v8-code-style#746](https://github.com/1C-Company/v8-code-style/issues/746).
