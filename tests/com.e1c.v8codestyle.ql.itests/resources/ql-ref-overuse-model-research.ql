ВЫБРАТЬ
    Doc.Ref.Description КАК DocDescription,
    Lines.Product.Ref КАК ProductRef,
    Balances.Product КАК BalanceProduct
ИЗ Документ.ProductArraival КАК Doc
    ЛЕВОЕ СОЕДИНЕНИЕ Документ.ProductArraival.PurchaseItems КАК Lines
        ПО ИСТИНА
    ЛЕВОЕ СОЕДИНЕНИЕ РегистрНакопления.Stocks.BalanceAndTurnovers КАК Balances
        ПО ИСТИНА
