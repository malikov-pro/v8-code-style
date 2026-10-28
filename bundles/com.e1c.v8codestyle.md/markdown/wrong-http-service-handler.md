# HTTP service handler contract

Checks an assigned handler in the owning service module: the method must exist and declare exactly one formal parameter. Empty assignment is native EDT validation and is not duplicated. Procedure/function kind, Export and return type are not constrained by pinned LS. Enabled by default, CRITICAL/ERROR, no parameters, quick fix or related locations. Extensions, external owners and full LS differential parity are not certified.

An optional formal still counts: one optional parameter is accepted, zero or two are not. Names are compared case-insensitively only within the actual owning module; methods of other services/common modules cannot satisfy the assignment. Missing/unresolved source modules and ambiguous method declarations are skipped. Whitespace-only assignments are treated as empty conservatively, without claiming literal LS equivalence for malformed metadata.

Each invalid assignment has its own marker on the nested HTTP Method.handler feature rather than the LS module-name/subroutine range. Source-only edits revalidate the owner through ModuleContextDefIndex.contextDef, including non-exported handlers. BM and file-only metadata assignment cycles are tested separately. Preprocessor/recovery cases, module file removal/recreation and extension ownership are not certified. Live count/message checks do not certify GUI card navigation.
