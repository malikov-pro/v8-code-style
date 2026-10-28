# Missing temp storage deletion

Checks that data received from temp storage (GetFromTempStorage) is later deleted (DeleteFromTempStorage): without deletion the data stays in the server memory until the session ends. Each retrieval needs a deletion with matching arguments whose statement starts on a line after the retrieval ends. A deletion of another address, an earlier deletion or a deletion on the same line does not suppress the issue.

Argument tokens are compared ignoring whitespace/comments and identifier case; string literals remain case-sensitive. Nested statements are searched throughout the method, as in the pinned LS implementation: this is not control-flow analysis, and a conditional deletion can suppress the issue. Module-level statements are not checked. The existing enabled default is retained; LS disables this diagnostic by default. These scope/default differences remain explicit. No quick fix is provided.

## See

- [BSL Language Server: MissingTempStorageDeletion](https://1c-syntax.github.io/bsl-language-server/diagnostics/MissingTempStorageDeletion/)
- Ported from the BSL Language Server diagnostic `MissingTempStorageDeletion`.
