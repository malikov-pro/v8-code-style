&AtClient
Procedure FillData(FillParameters)
    FormAttribute = FillParameters.SourceAttribute;
EndProcedure

&AtServer
Procedure ЗаполнитьДанные(ПараметрыЗаполнения)
    FormAttribute2 = ПараметрыЗаполнения.Источник;
EndProcedure

&AtServer
Procedure ПараметрыНеСовпадают(ПараметрыФормы1)
    // Only the exact name "Параметры"/"Parameters" is reported.
    Value = ПараметрыФормы1;
КонецПроцедуры
