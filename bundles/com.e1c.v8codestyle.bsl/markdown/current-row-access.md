# Dynamic list current row access

Use CurrentData to access row fields instead of dereferencing CurrentRow on a form table bound to a dynamic list. Standalone CurrentRow, ordinary table sections, value tables/trees and unknown receivers must not be reported. The receiver must be resolved, not guessed from a control name.

Enabled by default, MAJOR/PERFORMANCE, no parameters. Related locations and a quick fix are not supplied. Full LS equivalence is not certified.

Reports the CurrentRow token when followed by a property, method call or index access. Binding is proven through the resolved form control, its current data path and a single DynamicList attribute type; stale table extInfo alone is insufficient. Changes to the table binding, existing path segments and attribute type revalidate the form module without editing BSL. Aliased controls, composite types, extensions and external forms are not certified; unresolved receivers are skipped. This is a partial implementation, not a complete form-type inference engine.
