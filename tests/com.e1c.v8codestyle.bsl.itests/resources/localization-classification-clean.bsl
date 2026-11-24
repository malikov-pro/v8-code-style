Function Probe(Value) Export
    Via = NStr("en='Number %1'");
    Result = StrTemplate(Via, Value);
    Alone = NStr("ru='Привет';en='Hello'");
    Nested = StrTemplate("%1", NStr("en='Nested'"));
    SameLine = NStr("ru='Так';en='Same'"); Result = StrTemplate(SameLine);
    Reassigned = NStr("en='Old'");
    Reassigned = "New"; Result = StrTemplate(Reassigned);
    Target.Text = NStr("ru='Поле';en='Field'");
    Result = StrTemplate(Target.Other);
    Return Alone;
EndFunction
