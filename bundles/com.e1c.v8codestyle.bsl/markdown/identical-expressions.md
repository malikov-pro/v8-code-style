# Identical expressions on the left and right of the operator

Checks binary expressions with identical left and right operands, for example `A = A` or `A = 1 And A = 1`. Usually this is an error: the operator does not do what the author expected.

Allowed (as in BSL Language Server):

- repeating an operand in addition and multiplication (`A + A` is doubling);
- division by popular divisors — the `popularDivisors` parameter, by default `60, 1024` (scaling of time and bytes).

Operands are compared structurally. Redundant parentheses are ignored; swapping operands is allowed only for addition, multiplication, equality, And and Or, following LS's comparer. Repeated terms in a chain of the same logical operator are detected; explicit groups and mixed operators are preserved. Unary operators, global call names/arguments and skipped arguments are compared recursively. Property/index accesses are deliberately not considered equal, even when their text matches, as in LS.

String case, internal whitespace and escaped quotes remain exact; identifiers ignore case and hidden comments/whitespace are ignored. Popular-divisor exemptions apply only to numeric literals, including parenthesized ones, not strings. Methods and module-level expressions are checked. Comparison nesting is limited to 128 as a defensive guard. Constructors, preprocessor edge cases, arithmetic-chain associativity and full LS expression-context ranges are not fully ported; markers retain the current EDT binary-expression scope. The adapted linear LS corpus is covered, but full engine differential equivalence is not certified.

## Noncompliant Code Example

```bsl
If A = A Then
    Result = True;
EndIf;
Result = A = 1 And A = 1;
```

## Compliant Solution

```bsl
If A = 1 Then
    Result = True;
EndIf;
Result = A = 1;
```

## See

- [BSL Language Server: IdenticalExpressions](https://1c-syntax.github.io/bsl-language-server/diagnostics/IdenticalExpressions/)
- Ported from the BSL Language Server diagnostic `IdenticalExpressions`.
