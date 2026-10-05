# Schematron SEL Language

## Purpose

Use Schematron as declarative SEL language for evidence extraction.

Validation question:

- which nodes violate rule?

SEL question:

- which nodes are interesting evidence?

Both are node-selection problems; SEL keeps interpretation outside rule layer.

## Design principles

- Keep SEL small: selection + grouping + provenance.
- Keep traversal/control flow in generated XSLT/runtime.
- Prefer reusable presets over adding new language surface.
- Keep one rule base for validation and extraction profiles.

## Core SEL annotations

- `sel:type` logical evidence type.
- `sel:group` output group key.
- `sel:copy` evidence selection XPath.
- `sel:context` optional context selection XPath.
- `sel:preset` reusable metadata/template chain.

Merge order:

`defaults -> referenced presets in declared order -> inline sel:* attributes`

## Configurable SEL output namespace/prefix

Output namespace/prefix no longer fixed to `sel:*`.

Configuration precedence:

`task override -> schema default -> built-in default (http://jurgenei.name/sel, sel)`

Schema defaults:

```xml
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:sel="http://jurgenei.name/sel"
            sel:outputNamespaceUri="http://jurgenei.name/sel"
            sel:outputNamespacePrefix="sel">
```

Task overrides (`SchematronSelCompileTask` and `SchematronSelExtractTask`):

```groovy
outputNamespaceUri 'urn:custom:sel'
outputNamespacePrefix 'obs'   // or '' for default namespace output
```

## Example 1: basic knowledge evidence

### Input

```lisp
(c:Document {xmlns:c "http://jurgenei.name/canonical"}
  (c:Body
    (c:Section
      (c:Title "Interfaces")
      (c:Paragraph "Hello from source"))))
```

<details>
<summary>XML view</summary>

```xml
<Document xmlns="http://jurgenei.name/canonical">
  <Body>
    <Section>
      <Title>Interfaces</Title>
      <Paragraph>Hello from source</Paragraph>
    </Section>
  </Body>
</Document>
```

</details>

### SEL template

```lisp
(sch:rule
  {context "c:Paragraph"
   xmlns:sch "http://purl.oclc.org/dsdl/schematron"
   xmlns:c "http://jurgenei.name/canonical"
   xmlns:sel "http://jurgenei.name/sel"}
  (sch:report
    {test "normalize-space(.)"
     sel:type "paragraph"
     sel:group "knowledge"
     sel:copy "."
     sel:context "ancestor::c:Section[1]/c:Title"}
    "Paragraph evidence"))
```

<details>
<summary>XML view</summary>

```xml
<sch:rule context="c:Paragraph"
          xmlns:sch="http://purl.oclc.org/dsdl/schematron"
          xmlns:c="http://jurgenei.name/canonical"
          xmlns:sel="http://jurgenei.name/sel">
  <sch:report test="normalize-space(.)"
              sel:type="paragraph"
              sel:group="knowledge"
              sel:copy="."
              sel:context="ancestor::c:Section[1]/c:Title">
    Paragraph evidence
  </sch:report>
</sch:rule>
```

</details>

### Output

```lisp
(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "knowledge"}
  (sel:Observation
    {type "paragraph" group "knowledge" source "report" ruleContext "c:Paragraph"}
    (sel:Evidence
      (c:Paragraph {xmlns:c "http://jurgenei.name/canonical"} "Hello from source"))
    (sel:Context
      (c:Title {xmlns:c "http://jurgenei.name/canonical"} "Interfaces"))
    (sel:Source
      {document "canonical.xml"
       path "/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"})))
```

<details>
<summary>XML view</summary>

```xml
<sel:Observations xmlns:sel="http://jurgenei.name/sel" group="knowledge">
  <sel:Observation type="paragraph" group="knowledge" source="report" ruleContext="c:Paragraph">
    <sel:Evidence>
      <c:Paragraph xmlns:c="http://jurgenei.name/canonical">Hello from source</c:Paragraph>
    </sel:Evidence>
    <sel:Context>
      <c:Title xmlns:c="http://jurgenei.name/canonical">Interfaces</c:Title>
    </sel:Context>
    <sel:Source document="canonical.xml" path="/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"/>
  </sel:Observation>
</sel:Observations>
```

</details>

## Example 2: reusable preset chain

### Input

```lisp
(root
  (person "Jane")
  (employee "John"))
```

<details>
<summary>XML view</summary>

```xml
<root>
  <person>Jane</person>
  <employee>John</employee>
</root>
```

</details>

### SEL template

```lisp
(sch:schema
  {xmlns:sch "http://purl.oclc.org/dsdl/schematron"
   xmlns:sel "http://jurgenei.name/sel"}
  (sel:presets
    (sel:preset {id "id-required" type "missing-id" group "quality" copy "."}))
  (sch:pattern
    {id "identifiers"}
    (sch:rule {context "person"}
      (sch:assert {test "@id" sel:preset "id-required"} "Person must have id."))
    (sch:rule {context "employee"}
      (sch:assert {test "@id" sel:preset "id-required"} "Employee must have id."))))
```

<details>
<summary>XML view</summary>

```xml
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:sel="http://jurgenei.name/sel">
  <sel:presets>
    <sel:preset id="id-required" type="missing-id" group="quality" copy="."/>
  </sel:presets>

  <sch:pattern id="identifiers">
    <sch:rule context="person">
      <sch:assert test="@id" sel:preset="id-required">Person must have id.</sch:assert>
    </sch:rule>
    <sch:rule context="employee">
      <sch:assert test="@id" sel:preset="id-required">Employee must have id.</sch:assert>
    </sch:rule>
  </sch:pattern>
</sch:schema>
```

</details>

### Output

```lisp
(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "quality"}
  (sel:Observation {type "missing-id" group "quality" source "assert" ruleContext "person"} "...")
  (sel:Observation {type "missing-id" group "quality" source "assert" ruleContext "employee"} "..."))
```

<details>
<summary>XML view</summary>

```xml
<sel:Observations xmlns:sel="http://jurgenei.name/sel" group="quality">
  <sel:Observation type="missing-id" group="quality" source="assert" ruleContext="person">...</sel:Observation>
  <sel:Observation type="missing-id" group="quality" source="assert" ruleContext="employee">...</sel:Observation>
</sel:Observations>
```

</details>

## Example 3: preset template content with current context

`sel:preset` can carry reusable template fragment. Attribute and text value templates evaluate in
current matched rule context.

### Input

```lisp
(c:Document {xmlns:c "http://jurgenei.name/canonical"}
  (c:Body
    (c:Section
      (c:Title "Interfaces")
      (c:Paragraph {code "p-1"} "Source payload"))))
```

<details>
<summary>XML view</summary>

```xml
<Document xmlns="http://jurgenei.name/canonical">
  <Body>
    <Section>
      <Title>Interfaces</Title>
      <Paragraph code="p-1">Source payload</Paragraph>
    </Section>
  </Body>
</Document>
```

</details>

### SEL template

```lisp
(sch:schema
  {xmlns:sch "http://purl.oclc.org/dsdl/schematron"
   xmlns:c "http://jurgenei.name/canonical"
   xmlns:sel "http://jurgenei.name/sel"}
  (sel:presets
    (sel:preset {id "paragraph-template" type "paragraph" group "knowledge" copy "."}
      (sel:template
        (sel:Meta {code "{@code}"}
          (sel:Summary "{concat(local-name(), ':', normalize-space(.))}")
          (sel:Section "{ancestor::c:Section[1]/c:Title}")))))
  (sch:pattern {id "knowledge"}
    (sch:rule {context "c:Paragraph"}
      (sch:report {test "normalize-space(.)" sel:preset "paragraph-template"}
        "Paragraph evidence"))))
```

<details>
<summary>XML view</summary>

```xml
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:c="http://jurgenei.name/canonical"
            xmlns:sel="http://jurgenei.name/sel">
  <sel:presets>
    <sel:preset id="paragraph-template" type="paragraph" group="knowledge" copy=".">
      <sel:template>
        <sel:Meta code="{@code}">
          <sel:Summary>{concat(local-name(), ':', normalize-space(.))}</sel:Summary>
          <sel:Section>{ancestor::c:Section[1]/c:Title}</sel:Section>
        </sel:Meta>
      </sel:template>
    </sel:preset>
  </sel:presets>

  <sch:pattern id="knowledge">
    <sch:rule context="c:Paragraph">
      <sch:report test="normalize-space(.)" sel:preset="paragraph-template">
        Paragraph evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>
```

</details>

### Output

```lisp
(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "knowledge"}
  (sel:Observation
    {type "paragraph" group "knowledge" source "report" ruleContext "c:Paragraph"}
    (sel:Evidence
      (c:Paragraph {xmlns:c "http://jurgenei.name/canonical" code "p-1"} "Source payload"))
    (sel:Meta {code "p-1"}
      (sel:Summary "Paragraph:Source payload")
      (sel:Section "Interfaces"))
    (sel:Source
      {document "canonical.xml"
       path "/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"})))
```

<details>
<summary>XML view</summary>

```xml
<sel:Observations xmlns:sel="http://jurgenei.name/sel" group="knowledge">
  <sel:Observation type="paragraph" group="knowledge" source="report" ruleContext="c:Paragraph">
    <sel:Evidence>
      <c:Paragraph xmlns:c="http://jurgenei.name/canonical" code="p-1">Source payload</c:Paragraph>
    </sel:Evidence>
    <sel:Meta code="p-1">
      <sel:Summary>Paragraph:Source payload</sel:Summary>
      <sel:Section>Interfaces</sel:Section>
    </sel:Meta>
    <sel:Source document="canonical.xml" path="/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"/>
  </sel:Observation>
</sel:Observations>
```

</details>

## Compiler/runtime boundaries

SEL authoring model stays declarative:

- no `xsl:template`, `xsl:mode`, `xsl:result-document` in Schematron rules.
- output construction lives in generated extraction stylesheet.
- grouped output fan-out uses `xsl:result-document`.

## Summary

SEL language sits on Schematron + XPath + generated XSLT.
It selects evidence, preserves provenance, and feeds downstream terminology/knowledge/graph pipelines
without turning rule layer into full transformation language.
