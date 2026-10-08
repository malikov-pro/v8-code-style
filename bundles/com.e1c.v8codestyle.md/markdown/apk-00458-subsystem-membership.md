# The object is not included in any subsystem

Every object of the configuration should be included in at least one
subsystem of the configuration subsystem tree (including nested subsystems),
so it has a place in the configuration sections. An object that is not
included in any subsystem is hard to find in the application and usually
means an unfinished configuration: the object is either not used and should
be deleted, or the subsystem composition is incomplete.

As in the source АПК algorithm, objects adopted in extension configurations
are not checked: they may legitimately belong to no subsystem of the
extension.

Service objects that cannot be included in a subsystem composition are out
of the scope of this check: subsystems, languages, event subscriptions,
common pictures, command groups, common attributes, defined types, styles,
functional options, session parameters, settings storages, XDTO packages,
roles, scheduled jobs, document numerators, integration services,
web socket clients, etc.

## Noncompliant Code Example

The catalog is not included in the composition of any subsystem:

```xml
<mdclass:Catalog uuid="...">
  <name>LonelyCatalog</name>
  ...
</mdclass:Catalog>
```

## Compliant Solution

The catalog is included in the subsystem composition:

```xml
<mdclass:Subsystem uuid="...">
  <name>Ci</name>
  <includeInCommandInterface>true</includeInCommandInterface>
  <content>Catalog.InSubsystemCatalog</content>
  ...
</mdclass:Subsystem>
```

## See

- Ported from the APK check `АПК_00458`.
- The check does not report objects adopted in extensions.
