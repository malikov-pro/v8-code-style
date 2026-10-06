#Region Public

Procedure OverrideOnBeforeWrite(ChangeObject, WriteParameters) Export
	
	OverrideOnBeforeWrite(ChangeObject, WriteParameters);
	
EndProcedure

Procedure InternalAdjustment(ChangeObject)
	
	OverrideOnBeforeWrite(ChangeObject, Undefined);
	
EndProcedure

Function GetBaseModuleState() Export
	
	Return Undefined;
	
EndFunction

#EndRegion
