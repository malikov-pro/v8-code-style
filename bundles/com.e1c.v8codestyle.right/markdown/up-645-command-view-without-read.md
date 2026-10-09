# Role has view right for the command without access to the object

Checks that a role granting the `View` right for a command also grants the `Read` or `View` right for the owner object of the command (standard 689, clause 9.1).

The right to view a command derives from the rights of its owner object: a user holding such a role sees the command in the interface but cannot read the object behind it, so opening the command fails or shows nothing.

The check inspects:

- commands of configuration objects only; a common command has no owner object whose rights govern access to it, so it is not checked;
- commands available in the command interface: the owner object is included in a subsystem, where the subsystem itself and all its parent subsystems up to the root have the *Include in command interface* flag set;
- a right is counted for a role when it is explicitly granted for the object or, when the object has no custom rights in the role, the default right value of the role applies to it.

Adopted objects of extensions are not checked. Unlike the related check *No role has view right for the command* (which reports a command that no role can see at all), this check reports a command visible to a role that has no access to the object data behind it.

## Noncompliant

A catalog `Goods` is included in a subsystem with the *Include in command interface* flag set and has the command `OpenGoods`. The role `Dispatcher` grants the `View` right for the `OpenGoods` command but grants no right for the `Goods` catalog.

The command `OpenGoods` is reported for the role `Dispatcher`.

## Compliant

The role that grants the `View` right for the `OpenGoods` command also grants the `Read` or `View` right for the `Goods` catalog (or grants no right for the command at all).

## See also

- [Standard 689. Setting up roles and access rights](https://its.1c.ru/db/v8std#content:689:hdoc:9.1)
- Ported from the upstream issue [1C-Company/v8-code-style#645](https://github.com/1C-Company/v8-code-style/issues/645).
