ВЫБРАТЬ
    P.Description КАК ProductDescription
ИЗ Справочник.Products КАК P
    ПРАВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
