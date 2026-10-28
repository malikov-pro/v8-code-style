SELECT
    CAST(P.TripleTarget.SingleTarget AS Catalog.Products).Description AS InnerCast,
    P.OverlappingTarget.Description AS OverlapDescription
FROM Catalog.Products AS P
