# Disabling safe mode

Checks global SetSafeMode/SetSafeModeDisabled calls (including Russian names) in methods and module-level code. Only an explicit safe flag is exempt: True for SetSafeMode, False for SetSafeModeDisabled. Unknown flags, missing or extra arguments are reported. Object methods are not checked. MAJOR/SECURITY, enabled by default, no parameters or quick fix. This is distinct from checking whether server-side code is executed in safe mode.

## See

- [BSL LS DisableSafeMode](https://1c-syntax.github.io/bsl-language-server/diagnostics/DisableSafeMode/)
