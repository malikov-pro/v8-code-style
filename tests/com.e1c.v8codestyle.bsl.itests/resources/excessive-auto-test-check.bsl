Procedure Audit(Data, Flag) Export
    If Data.Property("AutoTest") Then
        Return;
    EndIf;
    Если Данные.Свойство("АвтоТест") Тогда
        Возврат;
    КонецЕсли;
    If Data = "AutoTest" Then
        Return;
    EndIf;
    If Data = "АвтоТест" Then
        Return;
    EndIf;
    If Data.Property("АвтоТест") Then
        Return;
    EndIf;
    If Data.Свойство("AutoTest") Then
        Return;
    EndIf;
    If Data.Property("AutoTest") Then
        Message("continue");
        Return;
    EndIf;
    If Data.Property("AutoTest") Then
        // A comment is not an executable statement.
        Return;
    Else
        Message("else");
    EndIf;
    If Flag Then
        Message("first");
    ElsIf Data.Property("AutoTest") Then
        Return;
    EndIf;
    If (Data.Property("AutoTest")) Then
        Return;
    EndIf;
    If Flag Or Data.Property("AutoTest") Then
        Return;
    EndIf;
    If Data.Property("AutoTest") And Flag Then
        Return;
    EndIf;
    If Not Data.Property("AutoTest") Then
        Return;
    EndIf;
    If Data.pRoPeRtY("aUtOtEsT") Then
        Return;
    EndIf;
    If Data.Property("Auto Test") Then
        Return;
    EndIf;
    If Data.Property("AutoTest") Then
        If Data.Property("AutoTest") Then
            Return;
        EndIf;
    EndIf;
EndProcedure

Function ReturnValue(Data) Export
    If Data.Property("AutoTest") Then
        Return 1;
    EndIf;
    Return 0;
EndFunction

If Data.Property("AutoTest") Then
    Return;
EndIf;
