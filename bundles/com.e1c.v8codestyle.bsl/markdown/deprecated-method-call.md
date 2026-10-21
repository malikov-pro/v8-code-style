# Deprecated platform property

Residual scope of pinned LS DeprecatedMethodCall: resolved platform properties carrying deprecatedSince metadata. Defaults: enabled, MINOR/CODE_STYLE, no parameters. The configuration compatibility mode controls deprecation; the EDT project runtime version controls which platform model supplies metadata. Missing metadata is not evidence that a member is current.

Native EDT global method and constructed-type deprecation are not duplicated. Existing user-method obsolete checks remain separate. Removed names, deleted-prefix configuration properties and full LS behavior are not covered by this increment. No automatic replacement or related locations; full engine parity is not certified.
