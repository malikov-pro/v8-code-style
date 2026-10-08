#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export
	
	WriteParameters = Undefined;
	BaseLibraryModule.BeforeWrite(DataObject, WriteParameters);
	
EndProcedure

#EndRegion
