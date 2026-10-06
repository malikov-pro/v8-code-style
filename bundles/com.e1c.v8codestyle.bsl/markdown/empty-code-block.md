# Empty code block

Checks that conditional and loop code blocks are not empty. An empty block is a sign of a forgotten implementation or deleted content: the block should be filled in or removed.

Not checked: method bodies (see "Empty method" check), "Except" blocks (see "Empty except statement" check) and the module file block.

Parameter **Treat comments as code** (off by default): a block containing comments is not considered empty.

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
