# Using global context methods for file operations

In BSP-based solutions use the **FileSystemClient** (ФайловаяСистемаКлиент) program interface for file scenarios instead of the global context methods. The check reports the calls of the global methods:

- "PutFile" (ПоместитьФайл) / "BeginPutFile" (НачатьПомещениеФайла) - use `FileSystemClient.LoadFile()` / `LoadFiles()` instead;
- "PutFiles" (ПоместитьФайлы) / "BeginPuttingFiles" (НачатьПомещениеФайлов) - use `FileSystemClient.LoadFiles()` instead;
- "GetFile" (ПолучитьФайл) / "GetFiles" (ПолучитьФайлы) / "BeginGettingFiles" (НачатьПолучениеФайлов) - use `FileSystemClient.SaveFile()` / `SaveFiles()` instead.

The check also reports the "Show" call of a variable holding "New FileDialog" (Новый ДиалогВыбораФайла) - use `FileSystemClient.ChooseFolder()` and asynchronous analogues instead. The tracking is one level deep (a variable assigned in the same method), as in APK.

Common modules FileSystemClient (ФайловаяСистемаКлиент) and FileSystemServiceClient (ФайловаяСистемаСлужебныйКлиент) are not checked.

The check is **disabled by default**: the APK precondition "the solution contains the StandardSubsystems (BSP) subsystem" cannot be reproduced statically, so enable it for BSP-based solutions.

## Noncompliant Code Example

```bsl
PutFile(StorageAddress, FileName, True);

Dialog = New FileDialog(FileDialogMode.Open);
If Dialog.Show() Then
	FilePath = Dialog.SelectedFiles[0];
EndIf;
```

## Compliant Solution

```bsl
LoadParameters = FileSystemClient.GetLoadFileParameters();
FileSystemClient.LoadFile(LoadParameters);
```

## See

- [Standard 700. Installing add-ins and platform extensions](https://its.1c.ru/db/v8std#content:700:hdoc)
- Ported from the APK check `АПК_01149`.
