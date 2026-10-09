
Procedure BeforeWrite(Cancel)
	
	// issue #791: the check in ANY If condition counts as checked
	If Not DataExchange.Load Then
		BeforeWriteHandler(Cancel);
	EndIf;
	
EndProcedure

Procedure OnWrite(Cancel)
	
	// issue #791: the check in the ElsIf part counts as checked
	If Cancel Then
		DoNothing();
	ElsIf DataExchange.Load Then
		Return;
	EndIf;
	
EndProcedure
