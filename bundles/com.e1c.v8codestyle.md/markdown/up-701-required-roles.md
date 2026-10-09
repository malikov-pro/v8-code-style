# Required role is missing

The configuration must define the three mandatory roles of the standard 488
(section 2):

- `ПолныеПрава` (rus. «Полные права», FullAccess) — application
  administration;
- `АдминистраторСистемы` (rus. «Администратор системы», SystemAdministrator)
  — system administration of the infobase;
- `ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок`
  (InteractiveOpenExternalReportsAndDataProcessors) — interactive opening of
  external reports and data processors.

A role is found by its name; both the Russian and the English standard names
are accepted. Each missing role is reported on the configuration. Extension
configurations are not checked: by the same standard (section 6) an extension
must not borrow the mandatory roles and supplies its own roles instead.

Note: the roles must also be included in the configuration property
"Default roles" (section 2.4 of the standard) — that requirement is out of
the scope of this check.

## Noncompliant Code Example

A configuration whose roles contain, for example, only
`ПолныеПрава` — the roles `АдминистраторСистемы` and
`ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок` are missing.

## Compliant Solution

The configuration contains all three mandatory roles with the standard names.

## See also

- [Standard 488. Standard roles](https://its.1c.ru/db/v8std#content:488:hdoc)
- Upstream issue [1C-Company/v8-code-style#701](https://github.com/1C-Company/v8-code-style/issues/701),
  АПК error 1046.
