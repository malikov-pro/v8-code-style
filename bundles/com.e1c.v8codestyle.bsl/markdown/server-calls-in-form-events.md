# Server call in a form event

Enabled by default, ERROR / CRITICAL. Calling a local `&НаСервере` method from
`ПриАктивизацииСтроки`/`НачалоВыбора` (`OnActivateRow`/`OnStartChoice`) adds a
server round trip on every row activation or choice start.

`&НаСервереБезКонтекста`/`&AtServerNoContext` calls are allowed by the pinned LS
contract and are not reported. Only local module methods are resolved.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/ServerCallsInFormEvents/)
