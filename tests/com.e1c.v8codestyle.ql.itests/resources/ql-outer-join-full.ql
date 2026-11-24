ВЫБРАТЬ
    P.Description КАК ProductDescription,
    T.Description КАК TargetDescription
ИЗ Справочник.Products КАК P
    ПОЛНОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
