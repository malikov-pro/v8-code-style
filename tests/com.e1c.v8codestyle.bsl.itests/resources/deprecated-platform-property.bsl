Procedure Probe(Unknown) Export
    Sheet = New SpreadsheetDocument();
    OldName = Sheet.PrintParametersName;
    Док = Новый ТабличныйДокумент();
    Имя = Док.ИмяПараметровПечати;
    Sheet.PrintParametersName = "probe";
    NewName = Sheet.PrintParametersKey;
    Unknown.PrintParametersName = "custom";
    Custom = New Structure("PrintParametersName", "custom");
    Custom.PrintParametersName = "current";
    Description = BriefErrorDescription(ErrorInfo());
    Secure = New NSSSecureConnection("");
EndProcedure
