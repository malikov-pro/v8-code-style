&AtClient
Function ClientProbe(ServerProvider)
    ServerProvider = 1;
    Return ServerProvider;
EndFunction

&AtServerNoContext
Function DefaultProbe(ServerProvider = Undefined)
    ServerProvider = 1;
    Return ServerProvider;
EndFunction
