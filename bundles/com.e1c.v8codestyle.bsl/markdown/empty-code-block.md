# Empty code block

Checks that conditional, loop and Try code blocks are not empty. An empty block is a sign of a forgotten implementation or deleted content: the block should be filled in or removed.

Not checked: method bodies (see "Empty method" check), "Except" blocks (see "Empty except statement" check) and the module file block.

Parameter **Treat comments as code** (`commentAsCode`, false by default): a block containing a comment in that specific section is not considered empty. Comments in another branch or the Except section do not fill the empty block; text resembling a comment within a string is not a comment.

The primary range follows LS's parent-terminal rule: it spans the header's own terminals on its final line, excluding a trailing comment. A multiline header can therefore highlight only Then/Do, or To/In through Do for a multiline For header. For an empty loop closed on the same line, EndDo is included; EndIf is not part of an If-branch range. An empty Try highlights Try. Regression tests assert exact line, offset and length in RU/EN, nested and inline cases, and after incremental header/body changes. Preprocessor/recovery and the complete LS differential corpus are not certified; full equivalence is not claimed.

## Noncompliant Code Example

```bsl
If ItemAmount > Limit Then
    
EndIf;
```

## Compliant Solution

```bsl
If ItemAmount > Limit Then
    DoSomething(ItemAmount);
EndIf;
```

## See

- [BSL Language Server: EmptyCodeBlock](https://1c-syntax.github.io/bsl-language-server/diagnostics/EmptyCodeBlock/)
- Ported from the BSL Language Server diagnostic `EmptyCodeBlock`.
