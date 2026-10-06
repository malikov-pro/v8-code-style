# Using hardcode file paths in code

Checks string literals for Windows/Unix file paths, for example `"C:\Program Files (x86)\1cv8"` or `"/etc/". A hardcoded path depends on the machine the code runs on — use settings, constants or standard functions instead.

URLs (`ftp://`, `http://`, `https://`) are not checked. Unix paths are only reported for standard root folders (the `searchWordsStdPathsUnix` parameter).

The check has no quick fix: how to store the path is up to the user.

## Noncompliant Code Example

```bsl
Path = "C:\Program Files (x86)\1cv8\";
```

## Compliant Solution

```bsl
Path = GetCommonPath(ProgramFilesPath) + "1cv8\";
```

## See

- [BSL Language Server: UsingHardcodePath](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingHardcodePath/)
- Ported from the BSL Language Server diagnostic `UsingHardcodePath`.
