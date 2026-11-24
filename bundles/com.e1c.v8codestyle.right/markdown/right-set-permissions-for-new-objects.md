# Set permissions for new objects is allowed only for the full access role

Enabled by default, SECURITY / CRITICAL. Port of the BSL LS diagnostic
`SetPermissionsForNewObjects` (v8std 532).

The role flag "Set permissions for new objects" (`setForNewObjects`) makes a role
automatically receive rights for every object added to the configuration later.
For a role other than the full access role this accumulates excessive rights and
gives them to the users of that role.

The check reports a role when the flag is enabled and the role is not listed in
the `namesFullAccessRole` parameter.

## Parameters

| Name | Type | Default | Meaning |
|---|---|---|---|
| `namesFullAccessRole` | String | `FullAccess,ПолныеПрава` | Comma-separated names of the roles that are allowed to have the flag (compared case-insensitively) |

## Example

```text
Role.CustomRole
    Set permissions for new objects      = true    <- reported
Role.FullAccess
    Set permissions for new objects      = true    <- allowed (full access role)
Role.PlainRole
    Set permissions for new objects      = false   <- nothing to report
```

Fix: clear the flag for the role, or grant the missing rights explicitly.

## Limitations

- The check inspects only the configuration roles' own flag. Roles of configuration
  extensions are checked by the same rule (the flag is a role property), but no
  special handling for extension roles or roles on support is implemented.
- The second flag "Set permissions for attributes by default"
  (`setForAttributesByDefault`) is a different property and is not analyzed.
- Role names are compared case-insensitively (1C identifiers are case-insensitive);
  the LS compares the configured list case-sensitively, which is a deliberate
  deviation of the port.
- The marker is placed on the role itself (its flag feature), while the LS reports
  the managed application module range once per configuration.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/SetPermissionsForNewObjects/)
- [v8std 532: Установка прав для новых объектов и полей объектов](https://its.1c.ru/db/v8std/content/532/hdoc)
