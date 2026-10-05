# Using the OS users method

Checks the use of the `OSUsers` method. Getting information about operating system users is potentially dangerous: it requires justification and an access restriction.

Object methods are not checked — as in BSL Language Server. The check has no quick fix.

## Noncompliant Code Example

```bsl
Users = OSUsers();
```

## See

- [BSL Language Server: OSUsersMethod](https://1c-syntax.github.io/bsl-language-server/diagnostics/OSUsersMethod/)
- Ported from the BSL Language Server diagnostic `OSUsersMethod`.
