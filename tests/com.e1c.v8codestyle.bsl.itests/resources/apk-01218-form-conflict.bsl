&AtClient
Function ClientProbe(ServerProvider)
    ClientProvider = 1;
    CallProvider = 1;
    ServerProvider = 1;
    Return ServerProvider;
EndFunction

&AtServerNoContext
Function ServerProbe(ServerProvider)
    ServerProvider = 1;
    ClientProvider = 1;
    Return ServerProvider;
EndFunction

&AtServerNoContext
Function DefaultProbe(ServerProvider = Undefined)
    ServerProvider = 1;
    Return ServerProvider;
EndFunction
