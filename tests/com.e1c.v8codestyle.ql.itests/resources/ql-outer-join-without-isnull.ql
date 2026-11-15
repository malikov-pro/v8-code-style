ВЫБРАТЬ
    P.Description КАК ProductDescription,
    T.Description КАК TargetDescription
ИЗ Справочник.Products КАК P
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
