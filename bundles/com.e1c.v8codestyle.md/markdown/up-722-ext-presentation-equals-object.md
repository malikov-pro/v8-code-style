# Extended object presentation equals object presentation

The **extended object presentation** (for a register — the extended record
presentation, рус. «Расширенное представление объекта» / «Расширенное
представление записи») is filled with the full name of the object in the
singular. It is shown in object form titles and hints instead of the object
presentation, so it makes sense only when it is more informative than the
object presentation (or the synonym, when the object presentation is not
filled). Filling it with the same text as the object presentation is
meaningless: in this case it should not be filled at all.

For example, the object presentation is "Реализация" and the extended object
presentation is "Реализация товаров и услуг" — the extended presentation adds
information. If both properties contain "Реализация", the extended object
presentation adds nothing.

The presentations are compared in every language where both are specified,
ignoring case and leading/trailing spaces. One issue is added per object.

Checked are top DB objects (catalogs, documents, etc.) and information
registers. Accumulation, accounting and calculation registers have no record
presentation properties in the metadata model. The case where the object
presentation is not filled and the extended object presentation equals the
synonym is not checked (the separate АПК rule 1213).

## Wrong

```xml
<objectPresentation>
  <key>ru</key>
  <value>Реализация</value>
</objectPresentation>
<extendedObjectPresentation>
  <key>ru</key>
  <value>реализация</value>
</extendedObjectPresentation>
```

## Right

```xml
<objectPresentation>
  <key>ru</key>
  <value>Реализация</value>
</objectPresentation>
<extendedObjectPresentation>
  <key>ru</key>
  <value>Реализация товаров и услуг</value>
</extendedObjectPresentation>
```

Or the extended object presentation is not filled at all.

## See also

- [Standard 468. Custom object presentations](https://its.1c.ru/db/v8std#content:468:hdoc:3)
- Port of the АПК rule `АПК_1211` ([upstream issue #722](https://github.com/1C-Company/v8-code-style/issues/722)).
- Checks are not applied to objects adopted in extension configurations.
