# Latin and Cyrillic letters mixed in a word

Enabled by default, CODE_STYLE / MINOR. Identifiers that look alike in Cyrillic
and Latin are a classic source of "why is this undefined" defects: `Namе` and
`Name` differ by one invisible glyph.

Parameters match the pinned LS defaults: `excludeWords` lists platform compound
names (`ЧтениеXML`, `HTTPСоединение`, ...), and
`allowTrailingPartsInAnotherLanguage` (default `true`) allows names that start
in one alphabet and end in the other, such as `ВИмениEnglish`.

Checked v1 scope: method names, formal parameters, module and local variables,
annotation names and named annotation parameters, region names and goto label
targets. Assignment targets of undeclared variables are not checked. Names
shorter than two characters are ignored; the marker covers the identifier only.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/LatinAndCyrillicSymbolInWord/)
