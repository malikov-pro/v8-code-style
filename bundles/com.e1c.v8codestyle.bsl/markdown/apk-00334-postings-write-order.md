# Explicit writing of document postings in Posting handler

The document posting handler (**ОбработкаПроведения** / **Posting**) must not write register record sets explicitly with the **Write** method (рус. **Записать**). Postings are written by the system implicitly when the posting handler completes. An explicit write may cause record deadlocks when several users post documents in parallel.

Writing through a local variable assigned from the postings collection (`Движения` / `RegisterRecords`), including alias chains, is reported as well. Writes outside the posting handler and commented-out writes are not reported.

Exception: data stored in registers is needed by subsequent algorithms executed before leaving the posting handler.

## Noncompliant Code Example

```bsl
Procedure Posting(Refusing, PostingMode)

    RegisterRecords.Sales.Write();

    SalesSet = RegisterRecords.Sales;
    SalesSet.Write();

EndProcedure
```

## Compliant Solution

```bsl
Procedure Posting(Refusing, PostingMode)

    RegisterRecords.Sales.Write = True; // the system writes postings on handler completion

EndProcedure
```

## See

- [Standard 450. Document posting record writing order](https://its.1c.ru/db/v8std#content:450:hdoc)
- Ported from the APK check `АПК_00334`.
