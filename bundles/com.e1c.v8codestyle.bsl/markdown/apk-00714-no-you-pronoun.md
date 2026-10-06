# Messages should not address the user with "You" pronouns

Checks that user-facing message texts (string literals) do not use the second-person pronouns **Вы**, **Вас**, **Вам**, **Вами**, **Ваш**: messages are composed as impersonal sentences. Only whole words are reported; words like "Вашего" or "Выполнить" are not. Comments are not checked. One issue per source line.

## Noncompliant Code Example

```bsl
Message = "You do not have enough permissions to run this export";
```

## Compliant Solution

```bsl
Message = "Not enough permissions to run this export";
```

## See

- [Standard 585. Message code](https://its.1c.ru/db/v8std#content:585:hdoc)
- Ported from the APK check `АПК_00714`.
