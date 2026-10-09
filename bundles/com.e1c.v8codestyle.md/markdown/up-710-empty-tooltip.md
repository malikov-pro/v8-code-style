# The popup hint is not filled

To explain the purpose of a control to the user, a popup hint
(рус. «Всплывающая подсказка») is assigned to the attribute shown in the
interface. Filling the hint is mandatory (standard 506): the hint is shown
when the user hovers over the form field bound to the attribute and helps
to understand the purpose of the attribute without opening help.

An attribute that has a synonym (so it is named for the user) but no popup
hint is reported.

The formalizable part of the standard is implemented, as in the sibling
check of the АПК rule 00134 (the hint matches the synonym):

- only attributes of objects included in at least one subsystem with the
  "Include in command interface" flag are checked; for an attribute of a
  tabular section the owner is the object of the tabular section;
- an attribute without a synonym is not checked;
- of standard attributes only Code, Description, Parent, Owner and Period
  are checked, whereas the "Period" of a non-periodic information register
  is not checked.

Checked are object attributes, tabular section attributes, register
dimensions and resources, and the listed standard attributes. Objects
adopted in extension configurations are not checked. The hint length limit
of 8 words is not checked here (the АПК rule 1238 and the column header
hints of table boxes are out of the scope of this check).

## Wrong

The attribute `Курс` has a synonym but no hint:

```xml
<attributes uuid="...">
  <name>Курс</name>
  <synonym>
    <key>ru</key>
    <value>Курс валюты</value>
  </synonym>
</attributes>
```

## Right

```xml
<attributes uuid="...">
  <name>Курс</name>
  <synonym>
    <key>ru</key>
    <value>Курс валюты</value>
  </synonym>
  <toolTip>
    <key>ru</key>
    <value>Официальный курс валюты</value>
  </toolTip>
</attributes>
```

## See also

- [Standard 506. Hints](https://its.1c.ru/db/v8std#content:506:hdoc)
- Port of the АПК rule `АПК_1136` ([upstream issue #710](https://github.com/1C-Company/v8-code-style/issues/710)).
- See also the sibling check `apk-00134-attribute-hint` — a hint that
  matches the synonym.
- Checks are not applied to objects adopted in extension configurations.
