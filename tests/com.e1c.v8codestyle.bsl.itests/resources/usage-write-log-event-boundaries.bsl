Procedure Boundaries(Level, Text, Other) Export
    Try
        Message("try");
    Except
        WriteLogEvent("Event",Other.Level,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Other.Level.Value,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",EventLogLevel.Ошибка,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",УровеньЖурналаРегистрации.Error,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",(EventLogLevel.Information),,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",EventLogLevel.Information + Level,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Not EventLogLevel.Information,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Level(),,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Other.Level(),,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Level,,,ErrorProcessing.DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Level,,,Other.DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Level,,,ErrorProcessing.Factory().DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event",Level,,,(Text));
        WriteLogEvent("Event",Level,,,Not Text);
        WriteLogEvent("Event",Level,,,Text[0]);
        WriteLogEvent("Event",Level,,,Text.Value);
        WriteLogEvent("Event",Level,,,DetailErrorDescription(Other.ErrorInfo()));
        WriteLogEvent("Event",Level,,,DetailErrorDescription(ErrorInfo()) + BriefErrorDescription(ErrorInfo()));
    EndTry;
EndProcedure

Procedure Nested(Level, Flag) Export
    Try
        Message("outer");
    Except
        If Flag Then
            WriteLogEvent("Event",Level,,,"text");
        EndIf;
        Try
            WriteLogEvent("Event",Level,,,"text");
        Except
            WriteLogEvent("Event",Level,,,"text");
            Raise;
        EndTry;
        If Flag Then
            WriteLogEvent("Event",Level,,,"text");
        EndIf;
        Raise;
    EndTry;
EndProcedure

Procedure Assignment(Level, Text) Export
    Try
        Message("try");
    Except
        Text = "literal";
        WriteLogEvent("Event",Level,,,Text);
    EndTry;
    Try
        Message("try");
    Except
        ; Text = "literal";
        WriteLogEvent("Event",Level,,,Text);
    EndTry;
    Try
        Message("try");
    Except
        Text = UnknownValue;
        WriteLogEvent("Event",Level,,,Text);
    EndTry;
    Try
        Message("try");
    Except
        Text = DetailErrorDescription(ErrorInfo());
        WriteLogEvent("Event",Level,,,Text);
    EndTry;
EndProcedure
