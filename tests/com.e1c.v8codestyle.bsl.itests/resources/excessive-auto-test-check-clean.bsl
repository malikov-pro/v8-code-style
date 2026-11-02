Procedure Audit(Data) Export
    If Data.Property("AutoTest") Then
        Message("continue");
        Return;
    EndIf;
    If Data.Property("АвтоТест") Then
        Return;
    EndIf;
EndProcedure
