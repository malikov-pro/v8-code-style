SELECT
    SUM(0) AS ExactZero,
    SUM(P.Ref) AS FieldSum,
    SUM(1 + 2) AS ExpressionSum,
    SUM(CAST(1 AS NUMBER(17, 0))) AS SafeSum,
    SUM(DISTINCT 1) AS DistinctSum,
    "SUM(1)" AS Text,
    COUNT(*) AS RecordCount
FROM Catalog.Products AS P
// SUM(1) is just a comment
