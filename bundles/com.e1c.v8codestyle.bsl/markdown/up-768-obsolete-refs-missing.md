# Deprecated procedure (function) references a non-existent procedure (function)

A deprecated procedure (function) — marked `Устарела.`/`Deprecated.`, placed in
the `Deprecated` (rus. `УстаревшиеПроцедурыИФункции`) region or flagged
deprecated in the module context — should reference an **existing** replacement.
The check inspects the description comment above the method declaration and
resolves each replacement reference `См. Модуль.ИмяМетода` (rus. "see",
case-insensitive; English `see` is also recognized; one intermediate word is
allowed — `См. также …`, `see also …`). If the target common module or the
target method is not found in the configuration, an issue is reported: the
reference leads nowhere and misleads developers.

A reference to an existing but deprecated method is reported by the separate
check `up-769-obsolete-refs-obsolete`.

Simplifications: targets are resolved against the common modules of the
configuration; only common modules are checked; references to object module
methods and to the global context are not resolved.

## Noncompliant Code Example

```bsl
#Region Public

// Deprecated. Use the new function (see CommonModule.NonExistentFunction).
Function OldFunction() Export

	Return True;

EndFunction

#EndRegion
```

## Compliant Solution

```bsl
#Region Public

// Deprecated. Use the new function (see CommonModule.ActualFunction).
Function OldFunction() Export

	Return True;

EndFunction

// The actual replacement function.
Function ActualFunction() Export

	Return True;

EndFunction

#EndRegion
```

## See

- [Standard 453. Descriptions of procedures and functions, section 5.7](https://its.1c.ru/db/v8std#content:453:hdoc:5.7)
- Based on the upstream issue 1C-Company/v8-code-style#768 (APK 1334).
