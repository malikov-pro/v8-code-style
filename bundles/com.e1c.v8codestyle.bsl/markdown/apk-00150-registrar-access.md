# Register self-sufficiency: access to the Recorder attribute

A register must be logically independent from its recorders (optional). Any logic based on the register data, as well as any reports on the register, must not access the **Recorder** (**Регистратор**) fields — the register data alone must be sufficient.

Dereferencing the recorder by dot leads to implicit joins with additional tables. Besides, in a distributed infobase the recorder may be missing: register records migrate between nodes while recorders do not.

The check is performed in the register record set module. The search covers the module text including comments and string literals with query texts. Each occurrence of `.Регистратор.` / `.Recorder.` is reported.

Allowed accesses (not reported):

- `Отбор.Регистратор` / `Filter.Recorder`
- `СтандартныеРеквизиты.Регистратор` / `StandardAttributes.Recorder`
- `Регистратор.ТипЗначения` / `Recorder.ValueType`
- `Регистратор.Метаданные()` / `Recorder.Metadata()`
- `Регистратор.ПолучитьОбъект()` / `Recorder.GetObject()`
- `Регистратор.МоментВремени()` / `Recorder.MomentOfTime()`

## Noncompliant Code Example

```bsl
Procedure BeforeWrite(Cancel, WriteMode)

    For Each Record In ThisObject Do
        If Record.Recorder.Date > CurrentSessionDate() Then
            Cancel = True;
        EndIf;
    EndDo;

EndProcedure
```

## Compliant Solution

```bsl
Procedure BeforeWrite(Cancel, WriteMode)

    If ThisObject.Filter.Recorder.SetValue(Value) Then
        // filtering by the recorder is allowed
    EndIf;

EndProcedure
```

## See

- [Standard 477. Register self-sufficiency](https://its.1c.ru/db/v8std#content:477:hdoc)
- Ported from the APK check `АПК_00150`.
