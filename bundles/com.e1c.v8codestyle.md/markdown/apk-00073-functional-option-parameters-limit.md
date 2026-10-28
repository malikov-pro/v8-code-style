# Functional option parameter count

Configurations should have no more than **10 functional option parameters**.
Exactly 10 is allowed; more than 10 produces one issue on the configuration.
All declared parameters count, including unused ones. Consolidate parameters
with the same meaning, rather than raising the limit.

This is the count invariant of APK `АПК_00073`; its source has no formal error
message, so this check supplies one. No quick fix: consolidation changes
application behavior. Extension configurations are excluded because their
local count is not the complete configuration count.

## See

- [Standard 470](https://its.1c.ru/db/v8std#content:470:hdoc)
