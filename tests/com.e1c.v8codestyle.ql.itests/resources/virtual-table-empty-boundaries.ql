SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(, ) AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(&Period, ) AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(&Period) AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(, Price = &Price) AS V;
SELECT 1 AS Value FROM FilterCriterion.QueryFilter() AS F;
SELECT 1 AS Value FROM FilterCriterion.QueryFilter(&Filter) AS F;
SELECT 1 AS Value FROM AccountingRegister.QueryAccounting.ExtDimensions AS E;
SELECT 1 AS Value FROM AccountingRegister.QueryAccounting.Balance() AS B;
SELECT 1 AS Value FROM Catalog.Products AS P;
ВЫБРАТЬ 1 КАК Значение ИЗ РегистрСведений.Prices.СрезПоследних( // comment only
) КАК Р
