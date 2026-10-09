# Path separator and all-files mask specified manually

In Windows the path separator is the backslash ("\") and the all-files mask is "*.*"; in Linux the separator is the slash ("/") and the all-files mask is "*". Hard-coded separators and masks break the solution when it migrates between operating systems (standard 723, clause 2.5.2).

Use the platform functions instead:

- `GetPathSeparator` (ПолучитьРазделительПути) for the path separator;
- `GetAllFilesMask` (ПолучитьМаскуВсеФайлы) for the all-files mask;
- in BSP-based solutions use the functions of the `CommonFunctions` (ОбщегоНазначения) and `CommonFunctionsClient` (ОбщегоНазначенияКлиент) common modules.

The check inspects literal string parameters of the file search methods, as in the original APK algorithm:

- `FindFiles` (НайтиФайлы) — parameters 1 (folder) and 2 (mask);
- `BeginFindingFiles` (НачатьПоискФайлов) — parameters 2 (folder) and 3 (mask);
- the `New File` (Новый Файл) constructor — parameter 1 (path).

Each violating parameter is reported once. Parameters that are variables or expressions are not checked.

## Noncompliant Code Example

```bsl
Files = FindFiles("C:\Exchange\", "*.*");     // manual separator and mask
File = New File("C:\Exchange\Data.xml");      // manual separators
```

## Compliant Solution

```bsl
Folder = "C:\Exchange";
Files = FindFiles(Folder + GetPathSeparator(), GetAllFilesMask());
File = New File(Folder + GetPathSeparator() + "Data.xml");
```

## See

- [Standard 723. Developing configurations for Linux and macOS](https://its.1c.ru/db/v8std#content:723:hdoc) (clause 2.5.2)
- Ported from the APK check `АПК_01167`.
