ВЫБРАТЬ
    T.Description КАК TargetDescription
ИЗ Справочник.Products КАК P
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
ГДЕ T.Description ЕСТЬ НЕ NULL
