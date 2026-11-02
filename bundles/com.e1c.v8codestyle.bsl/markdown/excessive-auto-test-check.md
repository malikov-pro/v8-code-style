# Obsolete AutoTest guard

Opt-in CODE_STYLE/MINOR audit, disabled by default. The source contract is
BSL Language Server ExcessiveAutoTestCheck. No automatic fix is provided:
removing a guard is a project-specific decision.

Only form, object, record-set and common modules are checked, including their
module-level code. Manager, session, command and other modules are excluded.
The marker covers the whole If statement, including Else/ElsIf when present.
Only the first If branch is examined; its body must contain exactly one Return,
with or without a return expression. Comments do not count as statements.

Matching is lexical, case-insensitive, against the end of the condition's
significant-token text: `.Свойство("АвтоТест")`, `.Property("AutoTest")`,
`="АвтоТест"` or `="AutoTest"`. Mixed `.Property("АвтоТест")` and
`.Свойство("AutoTest")` are deliberately not added. Outer parentheses can
prevent a match; Not or a preceding Or operand do not prevent a suffix match.
This is not proof of the receiver's type or of an actual automated-test context.
Keep these pinned LS boundaries in mind when enabling the audit.

Source: [BSL Language Server: ExcessiveAutoTestCheck](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/ExcessiveAutoTestCheck/).
