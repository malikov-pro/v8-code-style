# Explicit color value in form items

Use style items to change the appearance of form controls instead of setting concrete color values directly in the items. This is required so that similar controls look the same in all forms where they appear (standard 667).

The check reports each color property (text color and back color, including their title, footer, multiple values and hidden state variants) that has a concrete color value (RGB) set in a form item or its ext info:

- item title text/back color, button text/back color, field footer text/back color, table text/back color, decoration text color;
- ext info text/back colors of input, label, check box, image, radio buttons, text document and formatted document fields;
- group back colors (usual, page, popup), the hidden state title back color and the column group title back color;
- text/back colors of table additions (search string, view status, search control).

A style item color reference (`style:...`), a web color (`web:...`), a palette color and "auto" are not reported. Border colors, picture colors and the view status button color are not checked. Base forms of extensions (borrowed from the main configuration without own changes) are not checked.

## Noncompliant Code Example

Form items with concrete color values in the form XML:

```xml
<items xsi:type="form:Button">
  <name>BadButton</name>
  ...
  <textColor>#FF0000</textColor>
</items>
<items xsi:type="form:FormField">
  <name>BadField</name>
  ...
  <extInfo xsi:type="form:InputFieldExtInfo">
    <backColor>#00FF00</backColor>
  </extInfo>
</items>
```

## Compliant Solution

Reference a style item color instead:

```xml
<items xsi:type="form:Button">
  <name>GoodButton</name>
  ...
  <textColor>style:ButtonTextColor</textColor>
</items>
```

## See

- [Standard 667. Style items](https://its.1c.ru/db/v8std#content:667:hdoc)
- Ported from the upstream issue [1C-Company/v8-code-style#765](https://github.com/1C-Company/v8-code-style/issues/765).
