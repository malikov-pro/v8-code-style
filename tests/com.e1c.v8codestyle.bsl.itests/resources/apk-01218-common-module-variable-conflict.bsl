Function Probe(ServerProvider = Undefined) Export
    ServerProvider = 1;
    ClientProvider = 2;
    CallProvider = 3;
    Target = New Structure;
    Target.ServerProvider = 1;
    Text = "ServerProvider = 1;";
    // ServerProvider = 1;
    Return ServerProvider;
EndFunction
