# Duplicate adding or pasting a value to a collection

Enabled by default, CODE_STYLE / MAJOR. Port of the BSL LS diagnostic
`DuplicatedInsertionIntoCollection`.

A value inserted into a collection twice with no changes in between is either a
copy-paste mistake or a useless second call: the collection already contains the
value. The check reports the second insertion.

## What counts as an insertion

- A call statement of the form `Collection.Method(...)`; a bare `Method(...)`
  call has no collection and is ignored.
- The method name is exactly `Вставить`/`Добавить`/`Insert`/`Add`
  (case-insensitive). The LS parameter `isAllowedMethodADD` is not ported: the
  default LS behaviour (analyze `Add` as well) is used.
- The value is the **first** actual parameter. It is compared by its token text
  with whitespace ignored, case-insensitively outside string literals.
  `Undefined`/`0`/`Chars.*` and an empty or blank-string parameter never take
  part.

Insertions are compared only inside one code block: the module body, a method
body, one `If`/`ElsIf`/`Else` branch, one loop body or one `Try`/`Except` branch.
Insertions in different branches are never compared.

A pair of insertions is not reported when something between them can change the
value or the collection:

- an assignment to the collection or to its parent (`Объект.Коллекция = ...`,
  `Объект = ...`);
- an assignment to the value or to any of its parent prefixes
  (`Строка = ...`, `Данные.Реквизит = ...` for the value `Данные.Реквизит.Поле`);
- `Return`/`Break`/`Continue`/`Raise` (`Возврат`/`Прервать`/`Продолжить`/
  `ВызватьИсключение`);
- another call between them that passes the collection or the value as an
  argument (the call can change them).

## Example

```bsl
Procedure Fill(SourceRows)

    Rows = New Array;
    Rows.Add(SourceRows);
    Rows.Add(SourceRows); // reported: the same value is added twice

EndProcedure
```

For a structure the tracked value is the key, exactly as in the LS: two
`Insert("Key", ...)` calls with the same key are reported even when the second
argument differs.

## Limitations

- There is no data-flow or CFG analysis: only the statements between the two
  insertions are inspected. A value changed inside a called method, through a
  parameter or through a global variable is not tracked.
- The "value passed as an argument" rule looks at the root identifier of each
  argument only, so a change made through a longer expression may be missed.
- Preprocessor branches (`#Если`/`#If`) are not merged the way the LS parser
  merges them.
- A `Break`/`Continue` inside an inner loop between the insertions is treated as
  a change, while the LS ignores a breaker that belongs to an enclosing loop.
- The LS parameter `isAllowedMethodADD` is not implemented.
- Variables of the same name in different scopes are treated as the same value
  (the comparison is by text, as in the LS).

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/DuplicatedInsertionIntoCollection/)
