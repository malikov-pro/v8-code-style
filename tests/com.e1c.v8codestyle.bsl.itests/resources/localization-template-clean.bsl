Function Probe(Value) Export
    Via = NStr("ru='Номер %1';en='Number %1'");
    Result = StrTemplate(Via, Value);
    Alone = NStr("en='Hello'");
    Nested = StrTemplate("%1", NStr("ru='Вложенный';en='Nested'"));
    SameLine = NStr("en='Same'"); Result = StrTemplate(SameLine);
    Reassigned = NStr("ru='Старый';en='Old'");
    Reassigned = "New"; Result = StrTemplate(Reassigned);
    Target.Text = NStr("en='Field'");
    Result = StrTemplate(Target.Other);
    Return Alone;
EndFunction
