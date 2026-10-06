# Cut service comments formatting

Checks the formatting of the cut service comments — "// Локализация",
"// Конец Локализация", "// _Демо начало примера", "// _Демо конец примера".
They frame code fragments that are removed when building the international
release of a configuration.

Rules:

- a comment similar to a cut service comment but not written canonically
  (other case, separators, spaces) is reported as an invalid format;
- the "Локализация"/"Конец Локализация" comments are forbidden in modules
  without the "Локализация" postfix: national specifics are extracted into
  separate common modules with the "Локализация" postfix only;
- a common module with the "Локализация" postfix must contain the
  "// Локализация" + "// Конец Локализация" pair;
- opening and closing comments must be balanced.

## Noncompliant Code Example

```bsl
// In an ordinary module (without the "Локализация" postfix)
Procedure CloudServiceAddress(Service, ObjectAddress) Export

    If Service = "Box" Then
        ObjectAddress = "https://app.box.com/files/0/";
    // Локализация
    ElseIf Service = "Yandex" Then
        ObjectAddress = "https://disk.yandex.ru/client/disk/";
    // Конец Локализация
    EndIf;

EndProcedure
```

```bsl
// In the FilesLocalization module
Procedure DefineCloudServiceAddress(Service, ObjectAddress)

    //Локализация       ← invalid format (no space)
    If Service = "Yandex" Then
        ObjectAddress = "https://disk.yandex.ru/client/disk/";
    EndIf;

EndProcedure
```

## Compliant Solution

```bsl
// In an ordinary module: call the postfix module for national specifics
Procedure CloudServiceAddress(Service, ObjectAddress) Export

    If Service = "Box" Then
        ObjectAddress = "https://app.box.com/files/0/";
    Else
        FilesLocalization.DefineCloudServiceAddress(Service, ObjectAddress);
    EndIf;

EndProcedure
```

```bsl
// In the FilesLocalization module
Procedure DefineCloudServiceAddress(Service, ObjectAddress)

    // Локализация
    If Service = "Yandex" Then
        ObjectAddress = "https://disk.yandex.ru/client/disk/";
    EndIf;
    // Конец Локализация

EndProcedure
```

## See

- [Standard 769. International configuration release](https://its.1c.ru/db/v8std#content:769:hdoc)
- Ported from the APK check `АПК_01219`.
