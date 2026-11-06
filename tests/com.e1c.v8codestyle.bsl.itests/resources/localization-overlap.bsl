Function Probe(Number) Export
    Full = NStr("ru='Привет';en='Hello'");
    RussianOnly = NStr("ru='Привет'");
    EnglishOnly = NStr("en='Hello'");
    RussianTemplate = StrTemplate(NStr("ru='Номер %1'"), Number);
    EnglishTemplate = StrTemplate(NStr("en='Number %1'"), Number);
    ViaVariable = NStr("ru='Номер %1'");
    Result = StrTemplate(ViaVariable, Number);
    Return Full + RussianOnly + EnglishOnly + RussianTemplate + EnglishTemplate + Result;
EndFunction
