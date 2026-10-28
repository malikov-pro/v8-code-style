Procedure TemporaryDirectoryClean(Receiver) Export
    Member = Receiver.TempFilesDir();
    MemberRu = Receiver.КаталогВременныхФайлов();
    Text = "TempFilesDir() КаталогВременныхФайлов()";
    // TempFilesDir(); КаталогВременныхФайлов();
    FileName = ПолучитьИмяВременногоФайла("xml");
EndProcedure
