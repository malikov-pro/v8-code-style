# Legacy chart properties from before 8.3.12

Lexical residual of pinned LS DeprecatedAttributes8312: legacy properties accessed through roots named Chart, GanttChart, PivotChart or ChartPlotArea (and Russian equivalents). Defaults: enabled, TRIVIAL/CODE_STYLE, no parameters. Applies at configuration compatibility 8.3.12 or later.

This deliberately follows the pinned spelling-based contract, not inferred receiver types. A Structure variable named Chart may be reported; a chart alias with a different name is not. Removed system enum spellings are left to native EDT validation on the current runtime. The pre-8.3.12 runtime/compatibility combination and full engine parity are not certified. No quick fix or secondary locations.
