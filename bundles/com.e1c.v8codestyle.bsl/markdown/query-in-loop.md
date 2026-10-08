# Query in loop

It is recommended that you merge queries that address related data into a single query.

## Noncompliant Code Example

```bsl
// BanksToProcessing - contains an array of banks

InidividualQuery = New Query;
InidividualQuery.Text =
"SELECT
|	BankAccounts.Ref AS Account
|FROM
|	Catalog.BankAccounts AS BankAccounts
|WHERE
|	BankAccounts.Bank = &Bank");

For Each Bank From BanksToProcess Do
	InidividualQuery .SetParameter("Bank", Bank);
	AccountsSelection = InidividualQuery .Execute().Select();
	While AccountsSelection.Next() Do
		ProcessBankAccounts(AccountsSelection.Account);
	EndDo;
EndDo;
```

## Compliant Solution

```bsl
// BanksToProcess - contains an array of banks

MergedQuery = New Query;
MergedQuery.Text =
"SELECT
|	BankAccounts.Ref AS Account
|FROM
|	Catalog.BankAccounts AS BankAccounts
|WHERE
|	BankAccounts.Bank B(&BanksToProcess)";

MergedQuery.SetParameter("BanksToProcess", BanksToProcess);

AccountsSelection = MergedQuery.Execute().Select();
While AccountsSelection.Next() Do
	ProcessBankAccounts(AccountsSelection.Account);
EndDo;
```

## See

https://support.1ci.com/hc/en-us/articles/360011001620-Multiple-execution-of-the-similar-queries

https://support.1ci.com/hc/en-us/articles/360011001540-Rounding-arithmetic-results-in-queries

> **09.10.2026:** the check is disabled by default — its core invariant (query created and executed in a loop) is superseded by the wider `create-query-in-cycle` check (query/report builders, nested loops, While predicate added). The cross-method scenario "loop calls a method that executes a query" remains in this check — enable it manually in the check settings if needed.
