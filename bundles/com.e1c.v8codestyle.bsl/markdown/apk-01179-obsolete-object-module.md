# Code in module of obsolete metadata object

Checks that all modules of an obsolete metadata object are empty. An object is
considered obsolete when its name (or the name of its top parent, e.g. for a
form module) starts with the prefix **"Удалить"** (English **"Delete" /
"Obsolete"**) — case-insensitive. A common module is checked by its own name.

Each non-empty module of an obsolete object is reported as a single issue.

A module is empty when it contains no executable statements: comments and blank
lines are allowed. (The original APK algorithm treats any text, including
comments, as non-empty — this port intentionally allows comment-only stub
modules.)

## Noncompliant Code Example

```bsl
// The "УдалитьСкладскойУчёт" (obsolete warehouse accounting) object
// is marked obsolete, but its module still has code
Procedure OnWrite(Cancel)
    Message("The write-off logic is no longer needed");
EndProcedure
```

## Compliant Solution

The module of an obsolete object must be empty (comments are allowed),
and the object itself is to be deleted in a next version.

```bsl
// Obsolete object: the code is deleted,
// the object will be removed in the next version
```

## See

- [Standard 534. Deleting obsolete metadata objects from configuration](https://its.1c.ru/db/v8std#content:534:hdoc)
- Ported from the APK check `АПК_01179`.
