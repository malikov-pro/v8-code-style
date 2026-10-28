SELECT 1 AS Value FROM Catalog.Products P
LEFT JOIN AccountingRegister.QueryAccounting.ExtDimensions E ON TRUE;
SELECT 1 AS Value FROM FilterCriterion.QueryFilter(&Filter) F
LEFT JOIN Catalog.Products P ON TRUE
