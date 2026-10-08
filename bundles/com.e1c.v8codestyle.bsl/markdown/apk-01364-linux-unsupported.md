# Methods and objects not supported on Linux

Some platform methods and objects rely on Windows-only technologies (COM, OLE) or OS-specific window management and do not work on Linux. Use cross-platform mechanisms instead (standard 723):

- instead of COM (`COMОбъект`/`COMObject`) use file exchange, web/HTTP services or an administration server;
- external components shall be developed with Native API (`AttachAddIn` with `AddInType.Native`);
- instead of the `Mail` (Почта) object use `InternetMail` (ИнтернетПочта) or a mail client add-in;
- the `TextExtraction` (ИзвлечениеТекста) object and the `File.SetHidden` (`УстановитьНевидимость`), `File.SetHiddenAsync`, `ClientApplication.GetOSCaptionRepresentation` (`ПолучитьОтображениеЗаголовкаОС`), `ClientApplication.SetOSCaptionRepresentation` methods are not supported on Linux.

The check scans the whole module text including comments and string literals, like the original APK algorithm, and reports each occurrence (case-insensitive, Russian and English method names):

- `.SetHidden(` (`.УстановитьНевидимость(`), `.SetHiddenAsync(` (`.УстановитьНевидимостьАсинх(`)
- `.GetOSCaptionRepresentation(` (`.ПолучитьОтображениеЗаголовкаОС(`), `.SetOSCaptionRepresentation(` (`.УстановитьОтображениеЗаголовкаОС(`)
- `LoadAddIn(` (`ЗагрузитьВнешнююКомпоненту(`), `BeginAttachingAddIn(` (`НачатьПодключениеВнешнейКомпоненты(`), `AttachAddIn(` (`ПодключитьВнешнююКомпоненту(`), `AttachAddInAsync(` (`ПодключитьВнешнююКомпонентуАсинх(`)
- `AddInType.COM` (`ТипВнешнейКомпоненты.COM`)
- `New Mail` (`Новый Почта`), `New TextExtraction` (`Новый ИзвлечениеТекста`), `New COMObject` (`Новый COMОбъект`)

The check is disabled by default: the APK precondition "the rule runs only on Linux" (`ЭтоLinux()`) cannot be reproduced statically — enable it for solutions that must run on Linux.

## Noncompliant Code Example

```bsl
Procedure LoadAddInForExchange()

    AttachAddIn("Exchange", "Type", AddInType.COM); // COM is not available on Linux
    MailClient = New Mail;                          // the Mail object is not supported on Linux

EndProcedure
```

## Compliant Solution

```bsl
Procedure LoadAddInForExchange()

    AttachAddIn("Exchange", "Type", AddInType.Native); // Native API supports Linux
    // mail is sent via InternetMail or a Linux-capable mail add-in

EndProcedure
```

## See

- [Standard 723. Developing configurations for Linux and macOS](https://its.1c.ru/db/v8std#content:723:hdoc)
- Ported from the APK check `АПК_01364`.
