# ErrorCategory value unavailable for compatibility mode

Scoped residual of pinned LS UnavailableMemberCall: resolved platform ErrorCategory enum values whose introduction version is newer than the configuration compatibility mode. Defaults: enabled, MAJOR/ERROR, no parameters.

Introduction versions come from the ErrorCategory entries in pinned LS builtin-platform-types.json at 33a1400681e342ad4bd12281dcee8c7db781ee3d, not from name guessing. The EDT runtime version selects the model used for binding. Unresolved values, unknown receivers and custom Structure properties are skipped; native unresolved-name, global-call and constructor diagnostics are not duplicated.

This increment is not a general member/type version service: other enum types, methods, properties, constructors, HBK-derived versions and the LS explicit targetVersion override are outside its scope. Full LS parity is not certified. No quick fix or secondary locations.
