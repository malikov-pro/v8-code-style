Procedure Probe(Code, Unknown) Export
    Result = cAtAlOgS.Products.fInDbYcOdE(Code);
    Manager = Catalogs.Products;
    AliasResult = Manager.FindByCode(Code);
    UnknownResult = Unknown.FindByCode(Code);
    OtherMethod = Catalogs.Products.FindByDescription(Code);
    EmptyArgument = Catalogs.Products.FindByCode();
    Multiline = Catalogs.Products.
        FindByCode(Code);
    If Catalogs.Products.FindByCode(Code) = Undefined Then
        Return;
    EndIf;
EndProcedure
Procedure Shadow(Catalogs, Code) Export
    Result = Catalogs.Products.FindByCode(Code);
EndProcedure
Result = Catalogs.Products.FindByCode("001");
