# Incomplete register dimension values

Review dimensions whose **Deny incomplete values** flag is not set in information,
accumulation, accounting and calculation registers. Enable the flag when empty
dimension values are not allowed by the application's business rules.

This check is **disabled by default**, with WARNING / MAJOR severity. An empty
dimension can be intentional; this is an audit, not a platform validity error.
There are no parameters or automatic fixes.

The marker belongs to the dimension's flag, including registers without BSL
modules. Resources and attributes are not checked. The four register families
match BSL LS `DenyIncompleteValues`; unlike LS, markers are located directly on
metadata rather than on an available module. Cross-project and adopted extension
objects are not certified by this port.
