# Using synchronous calls

Checks the use of synchronous global context methods in client modules when the configuration restricts synchronous calls: synchronous calls do not work in the web client. Instead of a synchronous method, use its asynchronous analogue.

The checked methods include: `QueryBox`/`Вопрос`, `OpenFormModal`/`ОткрытьФормуМодально`, `OpenValue`/`ОткрытьЗначение`, `DoMessageBox`/`Предупреждение`, `InputDate`, `InputValue`, `InputString`, `InputNumber`, `InstallAddIn`, `InstallFileSystemExtension`, `InstallCryptoExtension`, `AttachCryptoExtension`, `AttachFileSystemExtension`, `PutFile`, `FileCopy`, `MoveFile`, `FindFiles`, `DeleteFiles`, `CreateDirectory`, `TempFilesDir`, `DocumentsDir`, `UserDataWorkDir`, `GetFiles`, `PutFiles`, `RequestUserPermission`, `RunApp`.

The check does not report:

- modules that are strictly server-side (object modules, record set modules, session module, etc.);
- calls in methods with the `&AtServer` (`&НаСервере`) or `&AtServerNoContext` (`&НаСервереБезКонтекста`) compiler directives;
- configurations where the use of synchronous calls is allowed (configuration property «Synchronous platform extension and add-in call use mode» = Use).

## Noncompliant Code Example

```bsl
&AtClient
Procedure AskUser()
    If Question("Continue?", DialogReturnCode.Yes) = DialogReturnCode.Yes Then
        // continue
    EndIf;
EndProcedure
```

## Compliant Code Example

```bsl
&AtClient
Procedure AskUser()
    Notify = New NotifyDescription("AfterQuestion", ThisObject);
    ShowQueryBox(Notify, "Continue?", DialogReturnCode.Yes);
EndProcedure
```

The check has no quick fix: asynchronous analogues change the code structure (notification handler).

## See

- [Limitations on the use of modal windows and synchronous calls](https://its.1c.ru/db/v8std#content:703:hdoc)
- [Correspondence of synchronous methods to asynchronous analogues](https://its.1c.ru/db/v838doc#bookmark:dev:TI000000438)
- BSL Language Server: [UsingSynchronousCalls](https://1c-syntax.github.io/bsl-language-server/diagnostics/UsingSynchronousCalls/)
