Procedure TemporaryDirectoryAudit(Receiver) Export
    Directory = КаталогВременныхФайлов();
    DirectoryEN = TempFilesDir();
    DirectoryMixed = tEmPfIlEsDiR();
    DirectoryRu = кАтАлОгВрЕмЕнНыХфАйЛоВ();
    Member = Receiver.TempFilesDir();
    MemberRu = Receiver.КаталогВременныхФайлов();
    Text = "TempFilesDir() КаталогВременныхФайлов()";
    // TempFilesDir(); КаталогВременныхФайлов();
    Other = TempFilesDirExtra();
    FileName = ПолучитьИмяВременногоФайла("xml");
EndProcedure
