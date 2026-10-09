# Language postfix in the name of a binary data or HTML template

A binary data template or an HTML template that is subject to translation must
be marked with a name postfix: an underscore followed by the code of the
configuration default language, for example **_ru**. When interface languages
are added, a copy of such template is created per language instead of
translating the template content. In code, the template is received with the
postfix of the current language.

Checked are binary data templates and HTML templates (common templates and
templates of applied objects). Tabular, text and other template types are
translated without preparatory actions and are not checked.

Unlike the source АПК algorithm, which checks only HTML templates containing
images, this check reports all HTML templates without the language postfix.

## Wrong

The print form template of an OpenOffice Writer document without the postfix:

```xml
<templates uuid="...">
  <name>ПФ_ODT_СчетНаОплату</name>
  <templateType>BinaryData</templateType>
</templates>
```

## Right

```xml
<templates uuid="...">
  <name>ПФ_ODT_СчетНаОплату_ru</name>
  <templateType>BinaryData</templateType>
</templates>
```

## See also

- [Standard 766. Template localization requirements](https://its.1c.ru/db/v8std#content:766:hdoc)
- Port of the АПК rule `АПК_00505`.
- If the configuration default language is not set, templates are not checked.
- Checks are not applied to objects adopted in extension configurations.
