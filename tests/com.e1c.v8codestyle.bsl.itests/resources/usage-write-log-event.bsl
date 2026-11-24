Procedure Arity() Export
    WriteLogEvent("Event");
EndProcedure

Procedure Level() Export
    WriteLogEvent("Event",,,,"Comment");
EndProcedure

Procedure Comment() Export
    WriteLogEvent("Event", EventLogLevel.Error,,,);
EndProcedure

Procedure WrongLevel() Export
    Try
        Message("try");
    Except
        WriteLogEvent("Event", EventLogLevel.Information,,,"Comment");
    EndTry;
EndProcedure

Procedure MissingDetails() Export
    Try
        Message("try");
    Except
        WriteLogEvent("Event", EventLogLevel.Error,,,"Comment");
    EndTry;
EndProcedure

Procedure Detailed() Export
    Try
        Message("try");
    Except
        WriteLogEvent("Event", EventLogLevel.Error,,,DetailErrorDescription(ErrorInfo()));
    EndTry;
EndProcedure

Procedure FirstAssignment() Export
    Try
        Message("try");
    Except
        Text = BriefErrorDescription(ErrorInfo());
        WriteLogEvent("Event", Level,,,Text);
    EndTry;
EndProcedure

Procedure LaterAssignment() Export
    Try
        Message("try");
    Except
        Message("first");
        Text = BriefErrorDescription(ErrorInfo());
        WriteLogEvent("Event", Level,,,Text);
    EndTry;
EndProcedure

Procedure Rethrow() Export
    Try
        Message("try");
    Except
        WriteLogEvent("Event", Level,,,"Comment");
        Raise;
    EndTry;
EndProcedure

Procedure Member(Receiver) Export
    Receiver.WriteLogEvent("Event");
EndProcedure

Процедура РусскийПример() Экспорт
    ЗаписьЖурналаРегистрации("Событие");
КонецПроцедуры

WriteLogEvent("ModuleBody");
