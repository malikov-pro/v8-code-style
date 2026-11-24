# Event log call completeness

WARNING/TRIVIAL, enabled by default, no parameters and no quick fix. Audits
global WriteLogEvent / ЗаписьЖурналаРегистрации in methods and module-level code.
Member methods with the same name are excluded. The whole call is highlighted.

One message per call, in order: fewer than five arguments; empty second argument;
empty fifth argument; a known non-error level inside Except; missing detailed
error information inside Except. Optional platform arguments remain legal:
this check recommends useful logging, not syntactic validity or a security ban.

The pinned LS lexical limits are preserved: unknown variable/function levels
are not automatically rejected; a Raise in the same code block can waive the
details requirement. DetailErrorDescription(ErrorInfo()) and the corresponding
ErrorProcessing helper are recognized in RU/EN. A comment variable is traced
only through the first statement of the same block; an unknown assignment is
accepted, not resolved across methods. This is not full data-flow analysis.

Source: [BSL Language Server: UsageWriteLogEvent](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/UsageWriteLogEvent/).
