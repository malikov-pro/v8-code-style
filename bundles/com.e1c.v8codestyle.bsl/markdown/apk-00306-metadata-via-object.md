# Object metadata is obtained via the global context property Metadata

Checks that object metadata is obtained by calling the **Metadata()** method of the object itself, not by accessing the **Metadata** global context property. The global property access (e.g. `Metadata.Catalogs[Name]`, `Metadata.Documents.FindByCode(...)`) is significantly slower.

The `FindByType` method is allowed: when the metadata object type is unknown in advance, use `Metadata.FindByType(...)`.

## Noncompliant Code Example

```bsl
Catalog = Metadata.Catalogs[CatalogName];
Item = Metadata.Documents.FindByCode(DocumentCode);
```

## Compliant Solution

```bsl
Catalog = CatalogObject.Metadata();
Item = DocumentRef.Metadata();
// Type unknown in advance:
Name = Metadata.FindByType(Type(ValueOf(Ref))).FullName();
```

## See

- [Standard 445. Getting metadata of configuration objects](https://its.1c.ru/db/v8std#content:445:hdoc)
- Ported from the APK check `АПК_00306`.
