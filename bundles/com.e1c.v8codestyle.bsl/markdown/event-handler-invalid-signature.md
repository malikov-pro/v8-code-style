# Invalid event handler signature

Compares the declared parameter count of a bound platform handler with the event's permitted signatures. Method spelling alone does not establish a form event. Missing handler methods remain native EDT validation, not duplicate issues from this check. Defaults: enabled, MAJOR/ERROR, no parameters.

Initial scope is parameter counts of root form events returned by EDT BslEventsService.getAllModuleEvents. Every known event bound to a method must accept its total declared parameter count; default values of the handler's own parameters do not reduce that count. Any permitted ParamSet range of an event is accepted (including an unbounded maximum). An unbound method named OnOpen is not a bound handler. Names of parameters, compiler directives and procedure/function kind are not checked.

BM name/event changes and Form.form synchronization revalidate the module without editing BSL; EDT emits a derived formEvents update during file synchronization. Item and command handlers, extensions, non-form handlers, repeated bindings of the same event and variadic contracts require separate certification. No quick fix or secondary location is supplied: unlike the LS edit, this partial implementation does not rewrite parameter lists. Full LS parity is not certified.
