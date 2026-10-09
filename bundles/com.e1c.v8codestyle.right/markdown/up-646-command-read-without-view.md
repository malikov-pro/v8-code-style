# Role with access to the object has no view right for the command

Checks that a role granting the `Read` or `View` right for the owner object of a command also grants the `View` right for the command itself (standard 689, clause 9.1).

The right to view a command shall be granted by the same role that grants the right to read or view the object: a user holding such a role sees the object but never sees or opens the command, so the command is unavailable in the interface of the role — a dead command for the roles that actually work with the data.

The check inspects:

- commands of configuration objects only; a common command has no owner object whose rights govern access to it, so it is not checked;
- commands available in the command interface: the owner object is included in a subsystem, where the subsystem itself and all its parent subsystems up to the root have the *Include in command interface* flag set;
- a right is counted for a role when it is explicitly granted for the object or, when the object has no custom rights in the role, the default right value of the role applies to it.

Adopted objects of extensions are not checked. Unlike the related check *No role has view right for the command* (which reports a command that no role can see at all), this check reports a command hidden for each concrete role that has access to the data.

## Noncompliant

A catalog `Goods` is included in a subsystem with the *Include in command interface* flag set and has the command `OpenGoods`. The role `Storekeeper` grants the `View` right for the `Goods` catalog but grants no right for the `OpenGoods` command.

The command `OpenGoods` is reported for the role `Storekeeper`.

## Compliant

The role that grants the `Read` or `View` right for the `Goods` catalog also grants the `View` right for the `OpenGoods` command.

## See also

- [Standard 689. Setting up roles and access rights](https://its.1c.ru/db/v8std#content:689:hdoc:9.1)
- Ported from the upstream issue [1C-Company/v8-code-style#646](https://github.com/1C-Company/v8-code-style/issues/646).
