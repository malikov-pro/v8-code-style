# Extra comma in a call parameter list

Enabled by default, CODE_STYLE / MAJOR. A trailing comma before the closing
parenthesis of a call is noise and can be read as an extra omitted argument.

Empty parameters in the **middle** of the list are a legal way to skip optional
arguments (`Method(Param1, , Param2)`) and are not reported. Only the last
significant token before `)` is checked; calls with local syntax-recovery errors
are skipped.

The marker covers the comma character. Member and global calls are treated
alike; the parameter list of `New` expressions is outside the v1 scope.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/ExtraCommas/)
