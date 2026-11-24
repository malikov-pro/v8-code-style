Procedure Boundaries(Data) Export
    If Data.Property("AutoTest") Then Return;; EndIf;
    If Data.Property("AutoTest") Then ; Return; EndIf;
    If Data.Property("AutoTest") Then
        Return;
    Else
        If Data.Property("AutoTest") Then
            Return;
        EndIf;
    EndIf;
EndProcedure
