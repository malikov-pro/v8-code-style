#Region Public

Procedure OverrideOnBeforeWrite(ChangeObject, WriteParameters) Export
	
	OverrideOnBeforeWrite(ChangeObject, WriteParameters);
	
EndProcedure

#EndRegion

#Region Private

Procedure InternalServiceCall()
	
	OverrideOnBeforeWrite(Undefined, Undefined);
	
EndProcedure

#EndRegion
