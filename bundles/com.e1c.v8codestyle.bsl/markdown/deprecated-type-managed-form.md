# Deprecated managed form type name

Enabled by default, CODE_STYLE / TRIVIAL. In compatibility mode 8.3.14 and
older, `Тип("УправляемаяФорма")` / `Type("ManagedForm")` names the managed form
type by its legacy alias.

The check follows the pinned LS scope: only a global `Тип`/`Type` call with the
single legacy string argument is reported. The marker covers the whole call
(LS anchors the parameter list). The deprecated name itself is a silent alias;
there is no automatic replacement in v1.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/DeprecatedTypeManagedForm/)
