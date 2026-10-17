SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() V
FULL JOIN Catalog.Products P ON TRUE
LEFT JOIN Catalog.Products W ON TRUE;
SELECT 1 AS Value FROM Catalog.Products P
INNER JOIN InformationRegister.Prices.SliceLast(, Price = &Price) V ON TRUE;
SELECT 1 AS Value FROM Catalog.Products P
INNER JOIN (SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() V
    LEFT JOIN Catalog.Products P ON TRUE) Q ON TRUE
