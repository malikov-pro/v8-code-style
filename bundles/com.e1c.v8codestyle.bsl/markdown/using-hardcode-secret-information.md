# Storing confidential information in code

Checks the code for storing confidential information (passwords) in plain text:

```bsl
Password = "12345";                      // variable
Object["Password"] = "12345";            // map key
Object.Password = "12345";               // property
Object.Insert("Password", "12345");      // Insert method
Map = New Structure("Password", "12345");
Connection = New FTPConnection(Host, Port, User, "12345"); // connection password
```

Keywords are configurable (the `searchWords` parameter, by default `Пароль|Password`). Masked values (`"***"`) are not reported.

The check has no quick fix: how to store a password is up to the user (SecureStorage/settings, input on demand).

## Noncompliant Code Example

```bsl
Password = "12345";
```

## Compliant Solution

```bsl
Password = RequestPasswordFromUser();
```

## See

- [BSL Language Server: UsingHardcodeSecretInformation](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingHardcodeSecretInformation/)
- Ported from the BSL Language Server diagnostic `UsingHardcodeSecretInformation`.
