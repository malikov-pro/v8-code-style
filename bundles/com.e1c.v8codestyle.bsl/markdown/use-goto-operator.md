# Goto operator use

Avoid using the GoTo operator in 1C:Enterprise language code as its use can result 
in complicated and ill-structured modules. It is difficult to understand the execution 
sequence and interrelation of its snippets. Instead of the GoTo operator, use other statements 
of 1C:Enterprise language.

The project's policy treats Goto as a blocking violation: this check defaults to
BLOCKER severity and reports both Goto statements and labels. Explicit project
severity settings are not overwritten. BLOCKER describes the issue severity;
it does not by itself stop validation or a build.

Future CFG-based return-path and reachability checks will skip a method or an
analyzed module-body block containing Goto rather than model label jumps or
produce unreliable linear-flow conclusions. This is a deliberate difference
from BSL LS, whose CFG handles Goto. Other checks remain active; this policy does
not imply that CFG support or its skip mechanism has already been implemented.

## Noncompliant Code Example

```bsl
If ChartOfCalculationTypes = Object.ChartOfCalculationTypes Then

  GoTo ~ChartOfCalculationTypes;

EndIf;
```

## Compliant Solution

```bsl
If ChartOfCalculationTypes = Object.ChartOfCalculationTypes Then

  ProcessChartOfCalculationTypes();

EndIf;
```

## See

- [Restrictions on using Go operator](https://kb.1ci.com/1C_Enterprise_Platform/Guides/Developer_Guides/1C_Enterprise_Development_Standards/Code_conventions/Using_1C_Enterprise_language_structures/Restrictions_on_using_Go_operator/?language=en)
