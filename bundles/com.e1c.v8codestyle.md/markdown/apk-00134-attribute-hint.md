# The hint of an attribute matches its synonym

Hints are set for attributes that are exposed to the user as interface
elements and require an explanation of their purpose. A hint that just
repeats the synonym of the attribute carries no additional information —
fill the hint with a meaningful explanation of the attribute purpose
or leave it empty.

As in the source АПК algorithm:

- attributes with an empty hint are not checked;
- of standard attributes only **Code**, **Description**, **Parent**,
  **Owner** and **Period** are checked, whereas the **Period** of
  a non-periodic information register is not checked;
- only attributes of objects included in at least one subsystem with
  the **Include in command interface** flag are checked; for an attribute
  of a tabular section the owner is the object of the tabular section.

## Noncompliant Code Example

The attribute `Price` with the synonym and the hint that are the same:

```xml
<attributes uuid="...">
  <name>Price</name>
  <synonym>
    <key>en</key>
    <value>Price</value>
  </synonym>
  <toolTip>
    <key>en</key>
    <value>Price</value>
  </toolTip>
  ...
</attributes>
```

## Compliant Solution

The hint explains the purpose of the attribute:

```xml
<attributes uuid="...">
  <name>Price</name>
  <synonym>
    <key>en</key>
    <value>Price</value>
  </synonym>
  <toolTip>
    <key>en</key>
    <value>Sale price per unit, excluding VAT</value>
  </toolTip>
  ...
</attributes>
```

## See

- Ported from the APK check `АПК_00134`.
- The check does not report objects adopted in extensions.
