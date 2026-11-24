Procedure Valid(Level, Text) Export
    WriteLogEvent("Event", Level,,,"Comment");
    Try
        Message("try");
    Except
        WriteLogEvent("Event", EventLogLevel.Error,,,DetailErrorDescription(ErrorInfo()));
        WriteLogEvent("Event", Level,,,Text);
        WriteLogEvent("Event", ErrorLevel(),,,ErrorProcessing.DetailErrorDescription(ErrorInfo()));
    EndTry;
EndProcedure
