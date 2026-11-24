# Incorrect expression line break

Enabled by default, CODE_STYLE / TRIVIAL. Port of the BSL LS diagnostic
`IncorrectLineBreak`.

A line break before a binary or logical operator reads better than a line break
after it, and a closing brace, semicolon or comma belongs to the previous line.
The check reports both directions.

## What is reported

The check works on the source text lines, as the LS does:

- **line start**: a line matching `^\s*(:?<listOfIncorrectFirstSymbol>)` — by
  default a line starting with `)`, `;`, `,` followed by a non-space, or `);`;
- **line end**: a line matching `\s+(:?<listOfIncorrectLastSymbol>)\s*(?://.*)?$` —
  by default a line ending with `ИЛИ`, `И`, `OR`, `AND`, `+`, `-`, `/`, `%`, `*`;
  a trailing line comment after the operator is allowed.

The matched fragment is reported, both rules are case-insensitive, and one
marker per line per rule is produced.

## What is skipped

- A match inside a line comment.
- A match inside a string literal (the LS checks the line with its own
  string-literal expression).
- The line before a multi-line string literal: concatenating a query text
  (`Text = Text +` / `"SELECT ..."`) is not reported.

## Parameters

| Name | Type | Default | Meaning |
|---|---|---|---|
| `checkFirstSymbol` | Boolean | `true` | Check the beginning of a line |
| `listOfIncorrectFirstSymbol` | String | `\)|;|,\s*\S+|\);` | Vertical bar separated characters that must not start a line |
| `checkLastSymbol` | Boolean | `true` | Check the end of a line |
| `listOfIncorrectLastSymbol` | String | `ИЛИ|И|OR|AND|\+|-|/|%|\*` | Vertical bar separated characters that must not end a line |

## Example

```bsl
// reported: the operator is at the end of the line
Total = FirstValue +
    SecondValue;

// reported: the line starts with a closing brace
Value = ?(FirstValue > 0,
    FirstValue,
    SecondValue
    );

// not reported: the operator starts the next line
Total = FirstValue
    + SecondValue;
```

## Limitations

- The LS skips the line before a parsed **query**; the port skips the line before
  any multi-line string literal, which is wider (documented deviation).
- A match inside a string that started on a previous line is recognized only as
  far as the line-local string expression sees it.
- The check is purely textual and does not use the AST; a line inside a
  preprocessor directive is checked as ordinary text.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/IncorrectLineBreak/)
