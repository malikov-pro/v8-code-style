# Reserved metadata names

The check reports the 64 Russian/English metadata names reserved by the pinned
BSL Language Server `ForbiddenMetadataName` rule. Matching is case-insensitive
and applies to the **whole name**, not a substring or a general SQL keyword list.

## Scope

The 16 supported top kinds are the four registers, BusinessProcess, Catalog,
ChartOfAccounts, ChartOfCalculationTypes, ChartOfCharacteristicTypes, Constant,
Document, DocumentJournal, Enum, ExchangePlan, FilterCriterion and Task.
Storage fields include attributes, register dimensions/resources, accounting
flags, document journal columns, tabular sections and their attributes.
Forms, commands, enum values and standard attributes are not checked.

Enabled by default, ERROR / CRITICAL, without parameters or quick fixes.
Initially only base configurations are checked; extension projects are excluded.
Vendor support restrictions are preserved. Markers target the actual metadata
name, even without BSL source; LS instead delivers issues through a BSL range.
An independent owner-name violation can produce a second marker. The check
does not rename objects or disable other checks.

## References

- [BSL Language Server: ForbiddenMetadataName](https://1c-syntax.github.io/bsl-language-server/diagnostics/ForbiddenMetadataName/)
