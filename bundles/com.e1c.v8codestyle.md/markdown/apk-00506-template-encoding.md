# Encoding other than utf-8 in a text or HTML template

The content of a text document template and of an HTML template must use the
utf-8 encoding. A template whose content declares an encoding with the
`encoding=` or `charset=` attribute (case-insensitive) with a value other than
utf-8 is reported.

Checked are text document templates and HTML templates (common templates and
templates of applied objects). Templates whose content is empty or not
accessible from the project model are not checked.

## Wrong

A text template declaring windows-1251:

```xml
<?xml version="1.0" encoding="windows-1251"?>
```

An HTML template with a meta tag declaring windows-1251:

```html
<meta charset="windows-1251">
```

## Right

```xml
<?xml version="1.0" encoding="utf-8"?>
```

```html
<meta charset="utf-8">
```

## See also

- [Standard 766. Template localization requirements](https://its.1c.ru/db/v8std#content:766:hdoc)
- Port of the АПК rule `АПК_00506`.
- Checks are not applied to objects adopted in extension configurations.
