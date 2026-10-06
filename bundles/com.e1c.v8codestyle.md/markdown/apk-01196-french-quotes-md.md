# French quotes (guillemets) in metadata object properties

In interface texts use only straight double quotes. French quotes (guillemets) are allowed only in help texts.

The check reports metadata objects whose **Synonym**, **Comment** or **Tooltip** property contains the French quotes `«` or `»`. One issue is added per property.

Covered objects: all metadata objects (both top objects such as catalogs, documents, common modules, and contained ones — attributes, tabular sections, commands, etc.), their standard attributes and standard tabular section descriptions. The Tooltip property is checked for the object classes that have it (attributes, tabular sections, commands, constants, etc.). For interface texts in modules see the related check `apk-01194-no-french-quotes` (NStr literals).

## Noncompliant

A catalog with the synonym `Товары «склад»`:

## Compliant

A catalog with the synonym `Товары "склад"`:

## See

- [Standard 598. Refusing to use the letter "ё" and French quotes](https://its.1c.ru/db/v8std#content:598:hdoc)
- Ported from the APK check `АПК_01196`.
