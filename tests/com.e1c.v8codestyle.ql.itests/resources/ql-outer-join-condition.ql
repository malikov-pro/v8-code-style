ВЫБРАТЬ
    ИСТИНА КАК Flag
ИЗ Справочник.Products КАК P
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T2
        ПО T.Description = T2.Description
