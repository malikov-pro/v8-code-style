Function Probe(Value) Export
    Ordinary = NStr("en='Hello'");
    Full = НСтр("ru='Привет';en='Hello'");
    CaseKey = NStr("RU='Привет'");
    NoKeys = NStr("not a language string");
    Empty = NStr("");
    Zero = NStr();
    Variable = NStr(Value);
    Parenthesized = NStr(("en='Hello'"));
    Concatenated = NStr("en='Hello'" + "ru='Привет'");
    ExtraArgument = NStr("en='Hello'", "en");
    Member = Value.NStr("en='Hello'");
    Nested = StrTemplate("%1", Wrap(NStr("en='Nested'")));
    MemberTemplate = Value.StrTemplate(NStr("en='Not global'"));
    Wrapped = NStr("en='Wrapped'");
    Result = StrTemplate((Wrapped));
    Composed = NStr("en='Composed'");
    Result = StrTemplate(Composed + "!");
    CaseVariable = NStr("en='Case'");
    Result = сASEVARIABLE;
    Result = StrTemplate(cASEVARIABLE);
    SameLine = NStr("en='Same'"); Result = StrTemplate(SameLine);
    Reassigned = NStr("en='Old'");
    Reassigned = "New";
    Result = StrTemplate(Reassigned);
    Target.Text = NStr("en='Field'");
    Result = StrTemplate(Target.Text);
    Target.Other = NStr("en='Other'");
    Result = StrTemplate(Target.Text);
    Outer = NStr("en='Outer'");
    If Value Then
        Result = StrTemplate(Outer);
        Inner = NStr("en='Inner'");
    Else
        Result = StrTemplate(Inner);
        OtherBranch = NStr("en='Else'");
    EndIf;
    Result = StrTemplate(OtherBranch);
    If Value Then
        Branch = NStr("en='Branch'");
        If Value Then
            Result = StrTemplate(Branch);
        EndIf;
    EndIf;
    While Value Do
        LoopText = NStr("en='Loop'");
    EndDo;
    Result = StrTemplate(LoopText);
    Try
        TryText = NStr("en='Try'");
    Except
        Result = StrTemplate(TryText);
        ExceptText = NStr("en='Except'");
    EndTry;
    Result = StrTemplate(ExceptText);
    Multiline = NStr("en='Hello'
        |;ru='Привет'");
    QuirkPipe = NStr("ru=|en='Hello'");
    QuirkBrace = NStr("ru={text");
    QuotedKeys = NStr("en='ru=''not really a key'''");
    DoubleQuotes = NStr("ru=""Привет"";en=""Hello""");
    Array[Value] = NStr("en='Indexed'");
    Result = StrTemplate(Array[Value]);
    IgnoredAssignment = Wrap(NStr("en='Wrapped RHS'"));
    Result = StrTemplate(IgnoredAssignment);
    Return Ordinary;
EndFunction

Function DifferentMethod(Value) Export
    Result = StrTemplate(Ordinary);
    Return Result;
EndFunction
