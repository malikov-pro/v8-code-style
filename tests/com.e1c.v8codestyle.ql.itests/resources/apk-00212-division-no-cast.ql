SELECT
    Prices.Product,
    Prices.Price / 2 AS HalfPrice
FROM
    InformationRegisters.Prices AS Prices
