# Missing temp storage deletion

Checks that data received from temp storage (GetFromTempStorage) is later deleted (DeleteFromTempStorage): without deletion the data stays in the server memory until the session ends. Simplification: all GetFromTempStorage calls of a method are reported if the method has no deletion at all. The check has no quick fix.

## See

- [BSL Language Server: MissingTempStorageDeletion](https://1c-syntax.github.io/bsl-language-server/diagnostics/MissingTempStorageDeletion/)
- Ported from the BSL Language Server diagnostic `MissingTempStorageDeletion`.
