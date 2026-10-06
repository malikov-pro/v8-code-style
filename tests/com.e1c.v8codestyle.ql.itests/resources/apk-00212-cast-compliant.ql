SELECT
    CAST(Prices.Price / 2 AS NUMBER(15, 2)) AS HalfPrice,
    AVG(CAST(Prices.Price AS NUMBER(15, 2))) AS AveragePrice,
    Prices.Price * 2 AS DoublePrice
FROM
    InformationRegisters.Prices AS Prices
