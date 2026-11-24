Function Probe(Value) Export
    Via = NStr("en='Number %1'");
    Result = StrTemplate(Via, Value);
    Alone = NStr("en='Hello'");
    Nested = StrTemplate("%1", NStr("en='Nested'"));
    SameLine = NStr("en='Same'"); Result = StrTemplate(SameLine);
    Reassigned = NStr("en='Old'");
    Reassigned = "New"; Result = StrTemplate(Reassigned);
    Target.Text = NStr("en='Field'");
    Result = StrTemplate(Target.Other);
    Return Alone;
EndFunction
