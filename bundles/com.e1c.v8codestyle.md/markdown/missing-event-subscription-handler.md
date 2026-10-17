# Event subscription handler requires a server module

Residual of pinned LS MissingEventSubscriptionHandler: the assigned common module must have Server enabled. Native EDT already reports empty or malformed assignments, missing modules/methods and non-exported methods; these are not duplicated. Parameter counts and procedure/function kind are outside this LS contract.

Enabled by default, CRITICAL/ERROR, no parameters or quick fix. Anchored to the subscription's handler property rather than the LS session-module workaround. Extensions and cross-project configurations are not certified. Full LS equivalence is not certified.

The qualified module reference is resolved in the current BM transaction (RU/EN type prefix, case-insensitive FQN); malformed or missing module references are skipped. The method's existence/export status remains a separate native invariant, so a non-server module can still be reported when its named method is missing, as in LS. Provider flag changes and CommonModule.mdo synchronization recheck matching subscriptions without editing their metadata. The collector scans subscriptions in the current project; large-configuration performance and provider rename/create/delete invalidation are not certified. No secondary locations are supplied.
