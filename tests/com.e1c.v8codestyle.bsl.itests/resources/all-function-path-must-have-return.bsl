Function AllBranchesReturn(Flag) Export
    If Flag Then
        Return 1;
    Else
        Return 2;
    EndIf;
EndFunction

Function FallThroughNoElse(Flag) Export
    If Flag Then
        Return 1;
    EndIf;
    Value = 2;
EndFunction

Function ElsIfNoElse(Flag) Export
    If Flag Then
        Return 1;
    ElsIf Flag > 1 Then
        Return 2;
    EndIf;
    Value = 3;
EndFunction
