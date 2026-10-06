# Using modal windows

Checks the use of modal methods (QueryBox, OpenFormModal, OpenValue, DoMessageBox, InputDate, InputValue, InputString, InputNumber): they do not work in the web client. Use asynchronous analogues (ShowQueryBox, OpenForm, ShowValue, etc.), as in BSL LS. The check has no quick fix: asynchronous analogues change the code structure.

## See

- [BSL Language Server: UsingModalWindows](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingModalWindows/)
- Ported from the BSL Language Server diagnostic `UsingModalWindows`.
