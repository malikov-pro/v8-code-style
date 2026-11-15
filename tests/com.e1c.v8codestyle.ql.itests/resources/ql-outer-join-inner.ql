ВЫБРАТЬ
    T.Description КАК TargetDescription
ИЗ Справочник.Products КАК P
    ВНУТРЕННЕЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
