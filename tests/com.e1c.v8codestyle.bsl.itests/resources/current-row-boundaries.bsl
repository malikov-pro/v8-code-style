&AtClient
Procedure Probe(Unknown) Export
    Result = Items.List.CurrentRow.Description;
    Items.List.CurrentRow.Write();
    Indexed = Items.List.CurrentRow[0];
    Current = Items.List.CurrentRow;
    Data = Items.List.CurrentData.Description;
    Ordinary = Items.RowsTable.CurrentRow.Description;
    UnknownResult = Unknown.CurrentRow.Description;
    Mixed = iTeMs.List.cUrReNtRoW.Description;
    Multi = Items.List.
        CurrentRow.Description;
    Comment = Items.List.CurrentRow. // read a field
        Description;
    Nested = String(Items.List.CurrentRow.Description);
EndProcedure
&AtClient
Procedure Shadow(Items) Export
    Result = Items.List.CurrentRow.Description;
EndProcedure
