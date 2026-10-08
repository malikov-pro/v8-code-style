#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export
	
	BaseLibraryModule.BeforeWrite(DataObject, WriteParameters);
	
	// _Демо начало примера
	WriteParameters.Insert("Demo", True);
	// _Демо конец примера
	
EndProcedure

#EndRegion
