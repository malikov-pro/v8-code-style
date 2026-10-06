# Access to the scheduled jobs manager in code

In solutions designed for the SaaS model (1cFresh), there must be no code that manages scheduled jobs directly. The check reports the beginning of an access chain to the scheduled jobs manager - "ScheduledJobs" ("РегламентныеЗадания").

To manage scheduled jobs, use the BSP program interface from the **ScheduledJobsServer** (РегламентныеЗаданияСервер) module: `FindJobs`, `AddJob`, `ChangeJob`, `DeleteJob`.

References like "Metadata.ScheduledJobs" and references to the ScheduledJobsServer module are not reported. Common modules whose name starts with "РегламентныеЗадания" (ScheduledJobs) are not checked.

The check is **disabled by default**: the APK precondition "the solution contains the StandardSubsystems (BSP) subsystem" cannot be reproduced statically, so enable it for BSP-based solutions.

## Noncompliant Code Example

```bsl
// Find the job by name.
Filter = New Structure();
Filter.Insert("Metadata", "PriceCheck");
Jobs = РегламентныеЗадания.GetScheduledJobs(Filter);
```

## Compliant Solution

```bsl
// Find the job by name.
Filter = New Structure();
Filter.Insert("Metadata", "PriceCheck");
Jobs = РегламентныеЗаданияСервер.FindJobs(Filter);
```

## See

- [Standard 760. Scheduled jobs - SaaS restrictions](https://its.1c.ru/db/v8std#content:760:hdoc)
- Ported from the APK check `АПК_00184`.
