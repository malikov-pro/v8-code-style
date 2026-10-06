# The letter "ё" in the name, synonym or comment of a metadata object

The letter **ё** (in any case, **ё**/**Ё**) is not allowed in names, synonyms
and comments of metadata objects: replace it with **е**. The synonym is
checked in every language of the configuration.

The issue is placed on the metadata object, and the message tells which
property (name, synonym or comment) contains the letter.

## Wrong

```xml
<name>ElkaCatalog</name>
<synonym>
  <key>ru</key>
  <value>Справочник «Ёлка»</value>
</synonym>
<comment>Хранит список ёлок</comment>
```

## Right

```xml
<name>ElkaCatalog</name>
<synonym>
  <key>ru</key>
  <value>Справочник «Елка»</value>
</synonym>
<comment>Хранит список елок</comment>
```

## See also

- [Standard 598. Refusing the letter "ё" in the metadata object names](https://its.1c.ru/db/v8std#content:598:hdoc)
- Port of the АПК rule `АПК_00126`.
- For the letter "ё" in module texts (code, string literals, comments) see
  the check `apk-00260-no-yo-letter`.
- Checks are not applied to objects adopted in extension configurations.
- Note: a neighbor check `mdo-ru-name-unallowed-letter` covers top-level
  objects only (lowercase "ё", Russian synonym); this check follows the
  source АПК algorithm and covers all metadata objects and both cases.
