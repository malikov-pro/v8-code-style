Procedure SecurityBoundaries(Flag, Receiver) Export
    SetPrivilegedMode(True);
    SetPrivilegedMode(False);
    SetPrivilegedMode(Flag);
    SetPrivilegedMode((False));
    SetPrivilegedMode(Not False);
    SetPrivilegedMode(False Or True);
    SetPrivilegedMode(False = True);
    SetPrivilegedMode(False, True);
    SetPrivilegedMode();
    Receiver.SetPrivilegedMode(False);
    Receiver.SetPrivilegedMode(True);
    Receiver.SetPrivilegedMode(Flag);
    УстановитьПривилегированныйРежим(Ложь);
    УстановитьПривилегированныйРежим(Истина);
    уСтАнОвИтЬпРиВиЛеГиРоВаНнЫйРеЖиМ(Флаг);
    Receiver.УстановитьПривилегированныйРежим(Ложь);
    Text = "SetPrivilegedMode(True)";
    // SetPrivilegedMode(True);
    SetPrivilegedModeExtra(True);
    Receiver.Other(True);
EndProcedure
