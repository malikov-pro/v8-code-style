# Code (number) length of configuration objects

The length of the code (number) of configuration objects is set according to its applied purpose.

- If coding (numbering) is not needed for the object, the code (number) length is set to zero.
- For typical configurations the recommended (but not mandatory) lengths are: 3, 5, 9, 11.
- When numbering, take into account the length of numbering prefixes: infobase prefix, organization prefix, etc. If the configuration uses the **Object prefixing** subsystem of the **Standard Subsystems Library**, the total length (including prefixes) of document numbers and catalog codes should be at least 11 characters (11, 13, 15, ...).

## Noncompliant Code Example

Catalog **Users** stores the short user name in its code, but the code length is 9 instead of the applied length.

## Compliant Code Example

Catalog **Users**: code length is set according to the short name length. Catalog **Nomenclature**: code length is 9 (or 11 with prefixes).

## See

- [Using configuration object codes (numbers)](https://its.1c.ru/db/v8std#content:473)
- APK rule: АПК_00137 (АПК_00138 for numbering)
