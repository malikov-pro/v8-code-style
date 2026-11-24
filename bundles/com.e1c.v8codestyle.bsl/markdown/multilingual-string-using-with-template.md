# Partially localized NStr is used by StrTemplate

Enabled by default, ERROR / MAJOR. An empty localized template can cause a runtime
exception. Parameter `declaredLanguages` defaults to `ru`; it does not read the
configuration's languages. Whitespace is removed from the comma-separated list;
case, repeated and empty entries follow the pinned LS lexical semantics.

The marker covers the whole global NStr call, not the StrTemplate call. Language
keys are extracted lexically, without executing code or fully parsing NStr format.
Nonliteral first arguments and member methods are excluded. Calls with local
syntax-recovery errors are skipped, unlike the LS recovery corpus.

Template use means a global StrTemplate ancestor before the enclosing statement,
or an assignment's whole left-hand expression used as an argument of a later-line
global StrTemplate in the same code block, including nested blocks. Parentheses
or concatenation in that argument do not count as a whole-expression match.
This is a lexical lookup: no reachability, reference identity or reassignment kill.

The ordinary `multilingual-string-has-all-declared-languages` check uses the same
classifier with the opposite branch. With equal parameters the two are disjoint.
Their parameters can be configured independently. The existing
`bsl-nstr-string-literal-format` and optional `checkEmptyInterface` remain unchanged;
manually enabling the project-language audit can add markers. No automatic
deduplication, disabling, generated translation or quick fix is provided.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/MultilingualStringUsingWithTemplate/)
- [Localization requirements](https://its.1c.ru/db/v8std#content:763:hdoc)
