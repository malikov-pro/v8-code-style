# Unsafe code search

Checks FindByCode / НайтиПоКоду on catalog and chart managers against metadata: disabled CheckUnique or code series other than the whole object make the search unsafe. This is distinct from using-find-element-by-string: the argument need not be a literal. Unknown receivers must not be guessed by their spelling.

Enabled by default, MAJOR/CODE_STYLE, no parameters or quick fix. Only direct Catalogs / ChartsOfAccounts / ChartsOfCharacteristicTypes roots (RU/EN), as in the pinned LS MdoRefBuilder; manager aliases are excluded. Metadata ownership is proven by resolved types, not inferred from a name. Metadata-only changes update indexed consumers in the same project without changing their source. Cross-project scope and union receivers are not certified. No related locations. Full pinned LS equivalence is not certified.
