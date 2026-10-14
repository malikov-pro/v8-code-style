# Nested constructors in structure declaration

Reports a Structure/FixedStructure constructor with more than one argument when a direct argument starts with a constructor having at least one non-empty argument. Empty constructors, wrapped calls and single-argument structure-copy constructors are excluded. Methods and module-level code are checked. Highlights the outer constructor, MINOR/WARNING, enabled by default; no parameters or quick fix. LS's related-information list of nested constructors is not provided.

## See

- [BSL LS NestedConstructorsInStructureDeclaration](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedConstructorsInStructureDeclaration/)
