SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(&Period) AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(&Period, ) AS V;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast(, Price = &Price) AS V;
SELECT 1 AS Value FROM Catalog.Products AS P LEFT JOIN InformationRegister.Prices.SliceLast() AS V ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V INNER JOIN Catalog.Products AS P ON TRUE;
SELECT 1 AS Value FROM InformationRegister.Prices AS R
