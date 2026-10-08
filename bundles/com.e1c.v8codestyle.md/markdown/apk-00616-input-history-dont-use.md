# The "History choice on input" property is not "Don't use"

The **History choice on input** (рус. «История выбора при вводе») property of a metadata object must be set to **Don't use** (рус. «Не использовать»):

- for every **Document** — unconditionally: repeated selection of one of the 5 previously chosen values is unlikely for documents;
- for every **Catalog** or **Document** whose manager module contains the data selection processing handler (**ОбработкаПолученияДанныхВыбора** / ObtainDataSelectionProcessing): the handler conditions are not taken into account by the input history mechanism, so the user can pick a value that otherwise cannot be picked.

The input history is stored on disk and keeps growing regardless of whether the user needs it. A value that is not set in the project is treated as "Auto" and reported, too: the only allowed value is "Don't use". One issue per object. Objects adopted in extension configurations are not checked.

## Noncompliant Code Example

Document property: **History choice on input** = `Auto`.

## Compliant Solution

Document property: **History choice on input** = `Don't use`.

## See

- [Standard 744. History choice on input](https://its.1c.ru/db/v8std#content:744:hdoc)
- Ported from the APK check `АПК_00616` (codes 412, 413).
