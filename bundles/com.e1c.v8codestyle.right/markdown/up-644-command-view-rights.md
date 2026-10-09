# No role has view right for the command

Checks that at least one role grants the `View` right for the owner object of a command that is available in the command interface.

Rights for a command derive from the rights of its owner object: a role that gives access to the object shall also give the view right for its commands. If a command is visible in the command interface but no role grants the `View` right for the owner object, users can never see or open the command — the command is dead configuration content.

The check inspects:

- commands of configuration objects only; a common command has no owner object whose rights govern access to it, so it is not checked;
- commands available in the command interface: the owner object is included in a subsystem, where the subsystem itself and all its parent subsystems up to the root have the *Include in command interface* flag set;
- the `View` right is counted for a role when it is explicitly granted for the owner object or, when the owner object has no custom rights in the role, the default right value of the role applies to it.

Adopted objects of extensions are not checked.

## Non-compliant

A catalog `Goods` is included in a subsystem with the *Include in command interface* flag set and has the command `OpenGoods`, but neither role grants the `View` right for the `Goods` catalog:

- role `Storekeeper` — no rights for `Goods`;
- role `Manager` — the `View` right for `Goods` is denied.

The command `OpenGoods` is reported.

## Compliant

At least one role grants the `View` right for the `Goods` catalog.

## See also

- [Standard 689. Setting up roles and access rights](https://its.1c.ru/db/v8std#content:689:hdoc:9.1)
- Ported from the upstream issue [1C-Company/v8-code-style#644](https://github.com/1C-Company/v8-code-style/issues/644).
