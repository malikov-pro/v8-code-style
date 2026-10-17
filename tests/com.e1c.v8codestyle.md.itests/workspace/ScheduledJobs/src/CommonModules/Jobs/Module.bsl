

Procedure PredefinedJob() Export
	// Insert hanlder code.
EndProcedure

Procedure WithParameter(Value = Undefined) Export
    If Value = Undefined Then
        Return;
    EndIf;
EndProcedure

Procedure NotPredefinedJob() Export
	// Insert hanlder code.
EndProcedure

Function WithFunction(Value = Undefined) Export
    Return Value;
EndFunction

Procedure PrivateHandler(Value)
    If Value = Undefined Then
        Return;
    EndIf;
EndProcedure
