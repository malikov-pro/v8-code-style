ВЫБРАТЬ
    Док.Description КАК Описание,
    Строки.Product КАК Товар
ИЗ Документ.ProductArraival КАК Док
    ЛЕВОЕ СОЕДИНЕНИЕ Документ.ProductArraival.PurchaseItems КАК Строки
        ПО ИСТИНА
