Procedure Audit(TypeName, Suffix, Receiver) Export
    Value = New Structure;
    Value = New("Structure" + Suffix);
    Receiver.DeleteFiles("probe");
    Receiver.TempFilesDir();
    Value = New(("File"));
    Value = New(TypeName);
    Value = New("File2");
    Value = New("File""x");
    Text = "DeleteFiles New File";
EndProcedure
