# String assembled from several fragments

Enabled by default, CODE_STYLE / MAJOR. A string split into `|` continuations or
written as adjacent literals is hard to read and easy to break when edited: a
missing `|` silently changes the value.

The marker covers the whole literal. The check is intentionally lexical: it does
not judge the resulting text, only the fragment count.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/CrazyMultilineString/)
