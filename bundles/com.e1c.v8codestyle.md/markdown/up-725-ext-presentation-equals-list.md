# Extended list presentation equals list presentation

The **extended list presentation** (рус. «Расширенное представление списка»)
is filled with the full name of the object list. It is shown in list form
titles and hints instead of the list presentation, so it makes sense only
when it is more informative than the list presentation (or the synonym, when
the list presentation is not filled). Filling it with the same text as the
list presentation is meaningless: in this case it should not be filled at all.

For example, the synonym is "Номенклатура" and the extended list presentation
is "Номенклатура (товары и услуги)" — the extended presentation adds
information. If both properties contain "Номенклатура", the extended list
presentation adds nothing.

The presentations are compared in every language where both are specified,
ignoring case and leading/trailing spaces. One issue is added per object.

Checked are top metadata objects that have list presentation properties:
DB objects (catalogs, documents, etc.), information, accumulation, accounting
and calculation registers, enums, document journals and filter criteria.
The case where the list presentation is not filled and the extended list
presentation equals the synonym is not checked (the separate АПК rule 1216).

## Wrong

```xml
<listPresentation>
  <key>ru</key>
  <value>Номенклатура</value>
</listPresentation>
<extendedListPresentation>
  <key>ru</key>
  <value>номенклатура</value>
</extendedListPresentation>
```

## Right

```xml
<listPresentation>
  <key>ru</key>
  <value>Номенклатура</value>
</listPresentation>
<extendedListPresentation>
  <key>ru</key>
  <value>Номенклатура (товары и услуги)</value>
</extendedListPresentation>
```

Or the extended list presentation is not filled at all.

## See also

- [Standard 468. Custom object presentations](https://its.1c.ru/db/v8std#content:468:hdoc:5)
- Port of the АПК rule `АПК_1215` ([upstream issue #725](https://github.com/1C-Company/v8-code-style/issues/725)).
- Checks are not applied to objects adopted in extension configurations.
