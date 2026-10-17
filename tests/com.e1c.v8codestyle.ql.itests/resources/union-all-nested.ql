SELECT InnerQuery.Value AS Value
FROM (SELECT 1 AS Value
    UNION
    SELECT 2 AS Value) AS InnerQuery
UNION ALL
SELECT 3 AS Value;
ВЫБРАТЬ 4 КАК Значение
// ОБЪЕДИНИТЬ ВСЕ inside a comment is not syntax
ОбЪеДиНиТь
ВЫБРАТЬ 5 КАК Значение
ОБЪЕДИНИТЬ // a comment between UNION and ALL
ВСЕ
ВЫБРАТЬ 6 КАК Значение
