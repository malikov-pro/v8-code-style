# Service tags in comments

Checks only comment tokens for service tags after `//` and whitespace, using the LS `serviceTags` regex and flags. Default tags include TODO, FIXME, debug markers, constructor markers and generated handler placeholders. An empty regex matches all comment prefixes, as in LS; it is not a disable switch. Invalid regex is ignored with WARNING instead of aborting validation. Highlights the comment excluding its trailing newline. TRIVIAL/CODE_STYLE, enabled by default; no quick fix. Unlike BadWords, source code and string contents are not searched.

## See

- [BSL LS UsingServiceTag](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingServiceTag/)
