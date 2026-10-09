# Form field properties for objects with disabled choice history on input

After the choice history on input is disabled in the properties of a metadata object (`История выбора при вводе` = `Не использовать`), all form input fields referring to this object must have the properties:

- `Кнопка выпадающего списка` (drop-down list button) — `Нет`;
- `Кнопка выбора` (choice button) — `Да`;
- `Отображение кнопки выбора` (choice button representation) — `В поле ввода`.

Otherwise, before the actual selection starts, the user gets into a menu with the recent values where they have to press `Показать все` (show all) every time.

The value that is not set explicitly is treated as the default (`авто`): the drop-down list button is shown and the choice button representation is not `В поле ввода`, so such a field is reported.

Not reported:

- fields with the list choice mode and a choice list filled in metadata (a choice list filled programmatically cannot be detected statically);
- fields referring to metadata objects with quick choice enabled;
- fields (and fields inside groups or tables) with the `Только просмотр` (read-only) flag set, as such elements are not available for the value selection;
- base forms of extensions.

The form-level read-only flag is a runtime concept and is not stored in the form model, so it cannot be taken into account statically.

## Noncompliant

An input field referring to a document with the disabled choice history, properties left as default:

## Compliant

The same field with the properties set explicitly: drop-down list button off, choice button on, choice button representation `В поле ввода`.

## See

- [Standard 744. Choice history on input, clause 2.2](https://its.1c.ru/db/v8std#content:744:hdoc:2.2)
- Ported from the APK check `АПК_00617`.
- For the metadata side of the same standard clause see the check `apk-00616-input-history-dont-use` (md channel).
