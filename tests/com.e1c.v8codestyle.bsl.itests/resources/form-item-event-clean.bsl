&AtClient
Procedure FieldChange(Item)
    Result = Item;
EndProcedure

&AtClient
Procedure FieldChoice(Item, ChoiceData, StandardProcessing)
    StandardProcessing = False;
EndProcedure

&AtClient
Procedure CommandAction(Command)
    Result = Command;
EndProcedure
