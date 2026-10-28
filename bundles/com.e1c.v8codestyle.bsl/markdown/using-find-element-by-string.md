# Lookup by a literal argument

Checks receiver calls to FindByDescription/Code/Number (RU/EN). The first argument is suspicious when empty or starting with a string/decimal token, as in LS. Variables, negative numbers and parenthesized literals are excluded. No constant propagation or metadata uniqueness analysis. Zero arguments are safely handled as an empty argument. MAJOR/CODE_STYLE, enabled by default, no parameters or quick fix. This is not the typed UnsafeFindByCode diagnostic.

## See

- [BSL LS UsingFindElementByString](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingFindElementByString/)
