# Non-canonical keyword spelling

Enabled by default, CODE_STYLE / TRIVIAL. The BSL parser is case-insensitive,
so `ЕслИ`, `ТогдА` and `НЕОПРЕделено` parse, but mixed case hurts readability and
search.

Allowed spellings are generated from the pinned LS canonical keyword table:
the Russian and English forms plus the upper-case variants that LS accepts.
Only words whose lower-case form is a known keyword are checked, so identifiers,
strings and comments are untouched. Preprocessor (`#Если`) and annotation
(`&НаКлиенте`) forms are checked without the leading `#`/`&`.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/CanonicalSpellingKeywords/)
