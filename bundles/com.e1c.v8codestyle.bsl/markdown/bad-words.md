# Prohibited words

Checks that the module text contains no prohibited words. The word list is a regular expression set by the **badWords** check parameter; the search is case-insensitive.

The check scans **every line of the module text**, including comments and string literals; each occurrence (including occurrences inside identifiers) is reported on its position in the line.

The list is empty by default and the check is **disabled by default**: enable it in the check settings and configure the word list according to your team or project standards.

Parameter example: `лотус|шмотус` — two words separated by `|`; arbitrary regular expression syntax is supported.

## Sources

Port of the BSL Language Server diagnostic `BadWords` (in LS the diagnostic is also disabled by default with an empty word list).

`findInComments=true` is the default. When false, only lines containing non-comment code tokens are scanned, matching LS's NCLOC-row policy. The complete text of such a line is still searched, including an inline comment. Continuation lines of multiline string literals count as code. This is not a removal of comment substrings. Invalid regular expressions are ignored with a warning rather than aborting validation.

No quick fix is provided: rewording the text is a user decision.

## See

- [BSL Language Server: BadWords](https://1c-syntax.github.io/bsl-language-server/diagnostics/BadWords/)
