# The "Удалить" name prefix of an obsolete object does not match the "(не используется)" synonym prefix

An obsolete metadata object that cannot be deleted from the configuration yet
is renamed with the **Удалить** ("Delete") name prefix, and the
**(не используется)** prefix is added to its synonym. For example, the
attribute `MainAgreement` becomes `УдалитьОсновнойДоговор` with the synonym
"(не используется) Основной договор".

The prefixes are a pair: an object whose name starts with **Удалить**/**Delete**
(in any case) must have a synonym that starts with **(не используется)** — and
vice versa. A synonym with the "(не используется)" prefix means that the object
is obsolete and the data migration to new structures is finished, so the name
must be marked with the delete prefix as well.

An object with an empty synonym is not checked.

## Wrong

The attribute `УдалитьОсновнойДоговор` has the synonym "Основной договор"
(the synonym prefix is missing):

```xml
<attributes uuid="...">
  <name>УдалитьОсновнойДоговор</name>
  <synonym>
    <key>ru</key>
    <value>Основной договор</value>
  </synonym>
</attributes>
```

## Right

```xml
<attributes uuid="...">
  <name>УдалитьОсновнойДоговор</name>
  <synonym>
    <key>ru</key>
    <value>(не используется) Основной договор</value>
  </synonym>
</attributes>
```

## See also

- [Standard 534. Deleting metadata objects](https://its.1c.ru/db/v8std#content:534:hdoc)
- Port of the АПК rule `АПК_00156`.
- Checks are not applied to objects adopted in extension configurations.
