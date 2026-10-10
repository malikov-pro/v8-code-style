# Redundant form group tooltip

A nonempty group tooltip should provide information beyond the group's title
or name. One issue is reported per group if any tooltip language matches the
name or the title in that same language.

APK normalization uppercases text, removes its specified ASCII punctuation
(plus №), spaces, TAB and LF, then removes the Russian word ГРУППА. It does
not remove arbitrary Unicode whitespace/punctuation or the English word GROUP.
Empty normalized values are compared as in APK. Platform group kinds and
children of auto-filled command bars/context menus or command-source groups
are excluded. No quick fix: removing text may require the author's decision.

Ported from APK `АПК_00136` (error 447). Its registry links standards 478
and 765; the implemented invariant concerns redundant group tooltips,
not missing translations of every form item.

## See

- [Standard 478](https://its.1c.ru/db/v8std#content:478:hdoc)
- [Standard 765](https://its.1c.ru/db/v8std#content:765:hdoc)
