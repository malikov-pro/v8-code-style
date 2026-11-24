# Module variable has no description

Enabled by default, CODE_STYLE / MINOR. A module variable without a description
leaves its purpose and expected value type to guesswork.

A description is a comment directly above the declaration, or a comment on the
declaration line itself. Annotations (`&...`) may stand between the comment and
the declaration: the comment just has to stay attached, without a blank line.
A blank line between the comment and the declaration breaks the attachment.

The marker covers the variable name. v1 works line-based: multi-line annotation
arguments between the comment and the declaration are not resolved; the check is
about presence, not about accuracy of the described meaning.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/MissingVariablesDescription/)
