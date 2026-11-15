ВЫБРАТЬ
    P.Description КАК ProductDescription
ИЗ Справочник.Products КАК P
    ЛЕВОЕ СОЕДИНЕНИЕ Справочник.Products КАК T
        ПО P.SingleTarget = T.Ref
ГДЕ T.Description = "Warehouse"
