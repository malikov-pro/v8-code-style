ВЫБРАТЬ
    ИСТИНА КАК Flag
ИЗ Справочник.Products КАК P
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
ГДЕ ИСТИНА В
    (ВЫБРАТЬ ПЕРВЫЕ 1
        ИСТИНА
    ИЗ Справочник.Products КАК P2
    ГДЕ P2.Ref = T.Ref)
