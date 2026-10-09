# Data lock is created but never locked

Checks that a data lock created with the `New DataLock` operator (Russian `Новый БлокировкаДанных`) and filled with lock items via the `Add()` method is locked somewhere in the same method by the `Lock()` method call. A created but never locked data lock does not protect data from concurrent changes.

The tracking is lexical within one method: if the lock variable is passed to another method that locks it, the issue is still reported. Locks created without assigning to a local variable are not tracked. The requirement to call `Lock()` inside a try-catch block is checked by the separate `lock-out-of-try` check.

## Noncompliant Code Example

```bsl
Procedure WriteDocuments()

    DataLock = New DataLock;
    LockItem = DataLock.Add("Document.Invoice");
    LockItem.Mode = DataLockMode.Exclusive;

EndProcedure
```

## Compliant Solution

```bsl
Procedure WriteDocuments()

    DataLock = New DataLock;
    LockItem = DataLock.Add("Document.Invoice");
    LockItem.Mode = DataLockMode.Exclusive;
    DataLock.Lock();

EndProcedure
```

## See

- [Exceptions interception in code (1.3)](https://its.1c.ru/db/v8std#content:499:hdoc:3.6)
- Source: issue 1C-Company/v8-code-style#757
