Procedure Audit(Receiver) Export
    Receiver.MyRunApp("probe");
    Receiver.ЗапуститьВнешнееПриложение("probe");
    Value = Receiver.RunApp;
    Text = "RunApp('probe')";
    // RunApp("probe");
EndProcedure
