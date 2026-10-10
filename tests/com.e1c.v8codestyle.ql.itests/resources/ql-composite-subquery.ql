SELECT Q.Target.Description AS Description
FROM (
    SELECT P.TripleTarget AS Target
    FROM Catalog.Products AS P
) AS Q
