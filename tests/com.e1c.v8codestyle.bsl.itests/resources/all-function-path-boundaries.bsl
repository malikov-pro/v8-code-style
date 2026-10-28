Function LoopFall(Flag) Export
    While Flag Do
        Return 1;
    EndDo;
EndFunction

Function EndlessWhile() Export
    While True Do
        Return 1;
    EndDo;
EndFunction

Function GotoPath(Flag) Export
    If Flag Then
        Goto ~Skip;
    EndIf;
    Return 1;
    ~Skip:
    Value = 2;
EndFunction

Function TryClean(Flag) Export
    Try
        If Flag Then
            Raise "x";
        EndIf;
    Except
        Return 1;
    EndTry;
    Return 2;
EndFunction
