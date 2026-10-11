# Logical OR in join conditions
Reports OR in a join condition when its logical group contains more than one distinct column text. Repeated comparisons of the same column with different values are allowed, as are conditions without columns. Aliases are part of the column text: `A.Field` and `B.Field` are distinct. A separate equality outside the parentheses does not affect the count inside them. OR in a joined subquery's WHERE is not part of the enclosing join condition.

All OR keywords in a multi-column group are reported. In QL, the position is the OR keyword. For BSL query literals, the EDT bridge preserves the line but highlights the whole literal line: it discards the QL offset and length. This check also examines unparenthesized join OR expressions; the pinned LS implementation only selects parenthesized logical predicates directly under the join condition. Query schema and other EDT query owners extend LS's module-literal scope.

## Noncompliant Code Example

```sdbl
ON A.Field = B.Field
    AND (A.Field = 0 OR B.Field = 1)
```

## Compliant Solution

```sdbl
ON A.Field = B.Field
    AND (A.Field = 0 OR A.Field = 1)
```

## See

- [BSL Language Server: LogicalOrInJoinQuerySection](https://1c-syntax.github.io/bsl-language-server/diagnostics/LogicalOrInJoinQuerySection/)
