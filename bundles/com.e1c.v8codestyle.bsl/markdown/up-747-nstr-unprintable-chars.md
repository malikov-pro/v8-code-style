# Localized string starts or ends with an unprintable character

Checks that the message text of every language entry of the localized string passed to the `NStr` (`НСтр`) function does not start or end with an unprintable character (a space, tab or line feed).

Non-language characters at the edges of the message must be moved into separate string literals concatenated with the localized one: a translator does not see the whole context, so an edge space or line feed is easily lost or misplaced in translation (standard 761).

## Noncompliant Code Example

```bsl
// A trailing line feed inside the message text.
Текст = НСтр("ru = 'Не удалось сохранить файл по причине:
|'") + Details;

// A trailing space inside the message text.
Текст = НСтр("ru='Текст сообщения '");

// A leading space in one of the language entries.
Текст = НСтр("ru='Текст сообщения', en=' Message text'");
```

## Compliant Solution

```bsl
// Non-language characters are separate string literals.
Текст = НСтр("ru = 'Не удалось сохранить файл по причине:'")
	+ Символы.ПС + Details;

Текст = НСтр("ru='Текст сообщения'");

Текст = НСтр("ru='Текст сообщения', en='Message text'");
```

Entries that do not match the localized string syntax (a value in double quotes or without quotes, a missing language code, an unterminated text, a trailing separator) are reported by the **NStr syntax** check; the first parameter that is not a string literal is reported by the **NStr string literal format** check. One issue is reported per string literal.

## See also

- [Standard 761. Interface texts in code: localization requirements](https://its.1c.ru/db/v8std#content:761:hdoc:1)
- Ported from the upstream issue [1C-Company/v8-code-style#747](https://github.com/1C-Company/v8-code-style/issues/747).
