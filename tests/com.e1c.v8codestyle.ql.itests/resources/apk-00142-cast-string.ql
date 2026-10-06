SELECT
    CAST(Prices.Price AS STRING(100)) AS PriceString,
    ВЫРАЗИТЬ(Prices.Product КАК СТРОКА(1000)) AS ProductString
FROM
    InformationRegisters.Prices AS Prices
