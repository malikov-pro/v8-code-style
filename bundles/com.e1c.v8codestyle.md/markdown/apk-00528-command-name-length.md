# The name of a command is longer than 38 characters

To avoid scroll bars at the standard screen resolution, the name (synonym)
of a command should not exceed 38 characters, and it is better to keep it
within 30.

As in the source АПК algorithm:

- only commands (of configuration objects) and common commands are checked,
  other objects displayed in the command interface are not checked;
- if the synonym is empty, the command name is checked;
- commands whose group is a form command group are not checked;
- only commands available in the command interface are checked: the command
  (for a command of an object — the owner object) is included in a
  subsystem, where the subsystem itself and all its parent subsystems up to
  the root have the **Include in command interface** flag set.

The message contains the section of the command — the synonym of the root
subsystem.

## Noncompliant Code Example

The command with the synonym that is longer than 38 characters:

```xml
<commands uuid="...">
  <name>LongCommand</name>
  <synonym>
    <key>en</key>
    <value>A very long command synonym that is longer than thirty eight characters</value>
  </synonym>
  <group>NavigationPanelOrdinary</group>
  ...
</commands>
```

## Compliant Solution

The synonym is within 30—38 characters:

```xml
<commands uuid="...">
  <name>LongCommand</name>
  <synonym>
    <key>en</key>
    <value>Post documents</value>
  </synonym>
  <group>NavigationPanelOrdinary</group>
  ...
</commands>
```

## See

- Ported from the APK check `АПК_00528`.
- The check does not report objects adopted in extensions.
- The maximum length is a check parameter (38 characters by default).
