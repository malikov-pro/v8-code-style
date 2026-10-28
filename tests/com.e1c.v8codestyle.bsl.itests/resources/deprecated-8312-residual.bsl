Procedure Probe(Unknown) Export
    Диаграмма = Новый Диаграмма();
    Диаграмма.ОтображатьЛегенду = Истина;
    Chart = New Chart();
    Chart.ShowLegend = True;
    Alias = Chart;
    Alias.ShowTitle = True;
    Chart = New Structure("ShowLegend", True);
    Chart.ShowLegend = True;
    Unknown.ShowLegend = True;
    ОбластьПостроенияДиаграммы = Диаграмма.ОбластьПостроения;
    ОбластьПостроенияДиаграммы.ОтображатьШкалу = Истина;
    Ориентация = ОриентацияМетокДиаграммы.Авто;
    Группа = ГруппировкаПодчиненныхЭлементовФормы.Горизонтальная;
    Group = ChildFormItemsGroup.Horizontal;
    CurrentGroup = ChildFormItemsGroup.AlwaysHorizontal;
EndProcedure
