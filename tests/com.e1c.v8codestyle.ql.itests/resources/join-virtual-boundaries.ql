SELECT 1 AS Value FROM Catalog.Products AS P
LEFT JOIN InformationRegister.Prices.SliceLast() AS V ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V
INNER JOIN Catalog.Products AS P ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V
RIGHT JOIN InformationRegister.Prices.SliceFirst() AS W ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices AS R
LEFT JOIN Catalog.Products AS P ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V;
SELECT 1 AS Value FROM Catalog.Products AS P
LEFT JOIN (SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V) AS Q ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V, Catalog.Products AS P;
ВЫБРАТЬ 1 КАК Значение ИЗ Справочник.Products КАК Т
ЛЕВОЕ СОЕДИНЕНИЕ РегистрСведений.Prices.СрезПоследних(&Период) КАК Р ПО ИСТИНА
