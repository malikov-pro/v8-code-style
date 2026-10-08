#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export
	
	Library = Common.CommonModule("BaseLibraryModule");
	Library.BeforeWrite(DataObject);
	
EndProcedure

#EndRegion
