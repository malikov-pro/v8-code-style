SELECT TOP 5 InnerQuery.Value AS Value
FROM (SELECT TOP 10 1 AS Value) AS InnerQuery
ORDER BY Value;
SELECT TOP 5 InnerQuery.Value AS Value
FROM (SELECT 1 AS Value) AS InnerQuery;
ВЫБРАТЬ ПЕРВЫЕ 2 1 КАК Значение;
SELECT TOP 2 1 AS Value WHERE 1 = 1;
SELECT TOP 0 1 AS Value WHERE 1 = 1
UNION ALL
SELECT TOP 1 2 AS Value WHERE 2 = 2;
SELECT TOP 01 1 AS Value;
SELECT 1 AS Value ORDER BY Value
