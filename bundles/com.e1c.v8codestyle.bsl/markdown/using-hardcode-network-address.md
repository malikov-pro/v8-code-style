# Using hardcode ip addresses in code

Checks string literals for IPv4/IPv6 addresses, for example `"192.168.0.1"`. A hardcoded address depends on the environment the code runs on — use settings or constants instead.

Not reported (as in BSL Language Server): URLs; values that look like versions (the `searchPopularVersionExclusion` parameter); statements containing exclusion words (the `searchWordsExclusion` parameter: Верси/Version/Драйвер/Driver/…).

The check has no quick fix: how to store the address is up to the user.

## Noncompliant Code Example

```bsl
ServerAddress = "192.168.0.1";
```

## Compliant Solution

```bsl
ServerAddress = GetServerAddress();
```

## See

- [BSL Language Server: UsingHardcodeNetworkAddress](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingHardcodeNetworkAddress/)
- Ported from the BSL Language Server diagnostic `UsingHardcodeNetworkAddress`.
