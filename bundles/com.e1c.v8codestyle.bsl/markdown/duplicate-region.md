# Duplicate module regions

Each module-level region should be unique. The check reports one issue on the
first opening directive of each repeated group, including custom region names.
Nested regions and regions inside methods are not checked.

Standard Russian and English names (Public, Internal, Private, Variables,
Initialize and event-handler sections) are matched case-insensitively. Custom
names are compared exactly, including case, as in pinned BSL LS. Deprecated is
not a standard alias for this check; table-item suffixes are not translated.

Enabled by default, CODE_STYLE / TRIVIAL, with no parameters or quick fix.
Merging regions requires deciding where their contents belong. LS related
locations are not included in this version.

While this check is enabled, `module-structure-top-region` leaves module-level
duplicate groups to it, but still checks nested duplicates, ordering and
placement. Disabling this check restores that check's original duplicate audit
when its `checkDuplicates` parameter is enabled. No settings are rewritten.
