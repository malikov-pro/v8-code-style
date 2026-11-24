# Variable name conflicts with a common module

Reports a simple assignment whose left identifier matches a common module name
without case sensitivity, using the availability rules of APK `АПК_01218`
(diagnostic `acc_1435`, source APK 1.2.9.124).

## Context rules

- Common-module consumer: Server+ServerCall checks all common modules; Server
  without ServerCall checks server modules; otherwise checks managed-client modules.
- Object, manager, record-set, session, ordinary-application and value-manager
  consumers check server modules.
- Other consumers use the method's compiler directive: AtClient/AtClientAtServer*
  checks managed-client or ServerCall modules; AtServer* checks server modules.
  A method without a compiler directive has no match in this branch. Module body
  assignments use the server context. In this branch only, a same-name formal
  parameter with a default value excludes that name's assignments in the method.

## Scope and differences

Enabled by default, ERROR / CRITICAL, no parameters or quick fix. Base
configuration only; extension projects are excluded. Other EDT checks can report
the same conflict independently; no checks are automatically disabled.

The check uses syntax assignments, not APK's line-start text search. Strings,
comments, declarations, parameter defaults, member/index writes and reads do not
trigger it. Inline assignments are included. It does not reproduce APK's
cross-module table-cache order dependency. This is not full textual APK or LS
CommonModuleAssign equivalence. The marker targets the left identifier.

## See

- [Standard 467](https://its.1c.ru/db/v8std#content:467:hdoc)
