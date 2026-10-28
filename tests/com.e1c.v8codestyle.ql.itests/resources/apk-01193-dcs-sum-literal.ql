SELECT
    SUM(1) AS CountOne,
    СУММА(2.5) AS CountDecimal,
    SUM(-1) AS CountNegative,
    SUM(+0) AS PlusZero,
    SUM(-0) AS MinusZero,
    SUM(0.0) AS DecimalZero,
    SUM(0) AS ExactZero
FROM Catalog.Products AS P
