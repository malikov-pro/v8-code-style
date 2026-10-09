#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export
	
	Common.CommonModule("BaseLibraryModule").BeforeWriteObject(DataObject, WriteParameters);
	
EndProcedure

#EndRegion
