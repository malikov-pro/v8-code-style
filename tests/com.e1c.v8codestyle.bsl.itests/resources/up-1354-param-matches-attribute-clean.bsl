Procedure FillByCode(Code)
    // Standard attributes are not compared.
    Message(Code);
EndProcedure

Procedure SumQuantity(Quantity)
    // Attributes of tabular sections are not compared.
    Message(Quantity);
EndProcedure

Procedure BeforeWrite(CheckRef)
    // The name matches nothing.
    Message(CheckRef);
EndProcedure
