Function Contract(First, Second, Third = Undefined)
    Return First;
EndFunction

Procedure Test()
    Result = Contract(,
        2);
    Result = Contract(Contract(, 2), 2);
    Result = Contract(1, 2, );
    Result = Contract(1, 2, Undefined);
    Result = Contract(, 2, 3, 4);
    Result = Contract(, );
    Result = Contract(1, 2, ) + Contract(, 2);
EndProcedure

Result = Contract(, 2);
