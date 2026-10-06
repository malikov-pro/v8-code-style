# French quotes (guillemets) in form elements

In interface texts use only straight double quotes. French quotes (guillemets) are allowed only in help texts.

The check reports a form whose **title**, the **titles** or **tooltips** of its items, the **titles** of its attributes, or the **titles** or **tooltips** of its commands contain the French quotes `«` or `»`. One issue is added per property, and the issue message contains the path to the property (for example `Items.Name.Title`), as in the source АПК algorithm.

Form parameters are not checked (they have no interface texts); names of items, attributes and commands are not checked. For interface texts in modules see the related check `apk-01194-no-french-quotes` (NStr literals).

## Noncompliant

A form field with the title `Количество «шт»`:

## Compliant

A form field with the title `Количество "шт"`:

## See

- [Standard 598. Refusing to use the letter "ё" and French quotes](https://its.1c.ru/db/v8std#content:598:hdoc)
- Ported from the APK check `АПК_01195`.
