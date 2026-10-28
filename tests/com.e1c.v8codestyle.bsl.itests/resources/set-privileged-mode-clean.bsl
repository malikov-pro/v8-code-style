Procedure SecurityClean(Receiver) Export
    SetPrivilegedMode(False);
    УстановитьПривилегированныйРежим(Ложь);
    Text = "SetPrivilegedMode(True)";
    // УстановитьПривилегированныйРежим(Истина);
    Receiver.Other(True);
EndProcedure
