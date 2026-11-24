# Metadata field name matches its owner

A storage field must not have the same name as its owning metadata object.
A tabular-section attribute is compared with its **section**, not the top owner.
The section's own name is not compared with the top owner; this follows the
actual pinned LS algorithm rather than the broader documentation example.

## Scope

Twelve kinds: the four registers, BusinessProcess, Catalog, the three charts,
Document, ExchangePlan and Task. Attributes include register dimensions and
resources and chart-of-accounts accounting flags. Sibling-name conflicts,
standard attributes, forms and commands are outside this rule.

Case-insensitive comparison. Enabled by default, ERROR / CRITICAL, no parameters.
Initially base configurations only; extension projects are excluded. Vendor
support restrictions are preserved. The marker targets the field's metadata
name, including objects without BSL source, unlike LS's BSL-range delivery.
A reserved-name violation can independently produce another marker.
No automatic renaming or disabling of other checks is performed.

## References

- [BSL Language Server: SameMetadataObjectAndChildNames](https://1c-syntax.github.io/bsl-language-server/diagnostics/SameMetadataObjectAndChildNames/)
