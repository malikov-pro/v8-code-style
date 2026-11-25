# The letter "ё" in the name or synonym of a metadata object

The letter **ё** (in any case, **ё**/**Ё**) is not allowed in names and
synonyms of metadata objects: replace it with **е**. The synonym is checked
in every language of the configuration. Comments of metadata objects are
not checked: the letter "ё" is allowed there.

The issue is placed on the metadata object, and the message tells which
property (name or synonym) contains the letter.

## Wrong

```xml
<name>ElkaCatalog</name>
<synonym>
  <key>ru</key>
  <value>Справочник «Ёлка»</value>
</synonym>
```

## Right

```xml
<name>ElkaCatalog</name>
<synonym>
  <key>ru</key>
  <value>Справочник «Елка»</value>
</synonym>
```

## See also

- [Standard 598. Refusing the letter "ё" in the metadata object names](https://its.1c.ru/db/v8std#content:598:hdoc)
- Port of the АПК rule `АПК_00126`.
- For the letter "ё" in the module code (identifiers, string literals) see
  the check `apk-00260-no-yo-letter` (comments are excluded there too).
- Checks are not applied to objects adopted in extension configurations.
- Note: a neighbor check `mdo-ru-name-unallowed-letter` covers top-level
  objects only (lowercase "ё", Russian synonym); this check follows the
  source АПК algorithm and covers all metadata objects and both cases.
