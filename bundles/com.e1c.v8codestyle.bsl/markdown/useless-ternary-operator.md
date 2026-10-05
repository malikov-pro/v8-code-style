# Useless ternary operator

Checks the ternary operator `?(Condition, Branch1, Branch2)` whose result is known in advance:

- the condition is a Boolean constant: `?(True, A, B)`;
- both branches are Boolean constants: `?(X, True, False)` is `X`, `?(X, False, True)` is `NOT X`;
- both branches are the same: `?(X, True, True)`.

The quick fix simplifies the first two cases with Boolean branch constants. The other cases are up to the user.

## Noncompliant Code Example

```bsl
Result = ?(Flag, True, False);
Value = ?(True, 1, 2);
```

## Compliant Solution

```bsl
Result = Flag;
Value = 1;
```

## See

- [BSL Language Server: UselessTernaryOperator](https://1c-syntax.github.io/bsl-language-server/diagnostics/UselessTernaryOperator/)
- Ported from the BSL Language Server diagnostic `UselessTernaryOperator`.
