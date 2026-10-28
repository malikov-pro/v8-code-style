SELECT Q.Target.Description AS Description
FROM (
    SELECT CAST(P.TripleTarget AS Catalog.Products) AS Target
    FROM Catalog.Products AS P
) AS Q
