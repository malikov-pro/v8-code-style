SELECT
    P.SingleTarget.Description AS SingleDescription,
    P.MixedTarget.Description AS MixedDescription,
    CAST(P.AnyTarget AS Catalog.Products).Description AS CastDescription,
    P.UnknownTarget.Description AS UnknownDescription,
    P.DoubleTarget AS NoDereference
FROM Catalog.Products AS P
