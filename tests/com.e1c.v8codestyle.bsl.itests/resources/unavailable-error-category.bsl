Procedure Probe(Unknown) Export
    Old = ErrorCategory.AllErrors;
    NewValue = ErrorCategory.InvalidPassword;
    Значение = КатегорияОшибки.НеправильныйПароль;
    Custom = New Structure("InvalidPassword", True);
    CustomValue = Custom.InvalidPassword;
    UnknownValue = Unknown.InvalidPassword;
    Similar = New Structure("AllErrors", True);
    SimilarValue = Similar.AllErrors;
EndProcedure
