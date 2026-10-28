SELECT
    P.DoubleTarget.Description AS DoubleDescription,
    P.TripleTarget.Description AS TripleDescription,
    P.AnyTarget.Description AS AnyDescription,
    P.AnyCatalogTarget.Description AS CatalogDescription,
    P.SingleTarget.Description AS SingleDescription,
    P.MixedTarget.Description AS MixedDescription,
    CAST(P.TripleTarget AS Catalog.Products).Description AS CastDescription,
    P.Description AS PlainDescription,
    "P.TripleTarget.Description" AS LiteralText
FROM Catalog.Products AS P
// P.TripleTarget.Description is a comment
