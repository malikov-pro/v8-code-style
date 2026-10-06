# Messages should not contain exclamation marks

Checks that user-facing message texts (string literals) do not contain exclamation marks: they sound commanding and make the user feel blamed. Warnings of dangerous or critical actions are the only allowed case.

Exceptions (not reported): the warning word **Внимание!** ("Attention!"), a mark followed by an operator or xsl code (`!=` and similar), bat-file parameters (`!args!`, `!setup.exe!`, `!directory!`), and a mark not preceded by a letter, digit or `()` method call. Comments are not checked. One issue per source line.

## Noncompliant Code Example

```bsl
ShowMessageBox(, "Select an item, not a group!");
```

## Compliant Solution

```bsl
ShowMessageBox(, "Select an item, not a group");
Message = "Attention! Loading the database may result in data loss";
```

## See

- [Standard 585. Message code](https://its.1c.ru/db/v8std#content:585:hdoc)
- Ported from the APK check `АПК_00715`.
