Procedure InLoop(Array) Export
    For Each Item In Array Do
        If Item > 0 Then
            Break;
            DeadBreak = 2;
        EndIf;
        Continue;
        DeadContinue = 3;
    EndDo;
    AfterLoop = 4;
EndProcedure

Procedure WithLabel() Export
    Goto ~Label1;
    DeadGoto = 8;
    ~Label1:
    AfterLabel = 9;
EndProcedure

Procedure TerminalNative() Export
    Return;
    DeadReturn = 1;
EndProcedure

Procedure RaiseNative() Export
    Raise "x";
    DeadRaise = 5;
EndProcedure
