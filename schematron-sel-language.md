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

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;Document xmlns="http://jurgenei.name/canonical"&gt;
  &lt;Body&gt;
    &lt;Section&gt;
      &lt;Title&gt;Interfaces&lt;/Title&gt;
      &lt;Paragraph&gt;Hello from source&lt;/Paragraph&gt;
    &lt;/Section&gt;
  &lt;/Body&gt;
&lt;/Document&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(c:Document {xmlns:c "http://jurgenei.name/canonical"}
  (c:Body
    (c:Section
      (c:Title "Interfaces")
      (c:Paragraph "Hello from source"))))</code></pre></td>
  </tr>
</table>

### SEL template

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sch:rule context="c:Paragraph"
          xmlns:sch="http://purl.oclc.org/dsdl/schematron"
          xmlns:c="http://jurgenei.name/canonical"
          xmlns:sel="http://jurgenei.name/sel"&gt;
  &lt;sch:report test="normalize-space(.)"
              sel:type="paragraph"
              sel:group="knowledge"
              sel:copy="."
              sel:context="ancestor::c:Section[1]/c:Title"&gt;
    Paragraph evidence
  &lt;/sch:report&gt;
&lt;/sch:rule&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sch:rule
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
    "Paragraph evidence"))</code></pre></td>
  </tr>
</table>

### Output

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sel:Observations xmlns:sel="http://jurgenei.name/sel" group="knowledge"&gt;
  &lt;sel:Observation type="paragraph" group="knowledge" source="report" ruleContext="c:Paragraph"&gt;
    &lt;sel:Evidence&gt;
      &lt;c:Paragraph xmlns:c="http://jurgenei.name/canonical"&gt;Hello from source&lt;/c:Paragraph&gt;
    &lt;/sel:Evidence&gt;
    &lt;sel:Context&gt;
      &lt;c:Title xmlns:c="http://jurgenei.name/canonical"&gt;Interfaces&lt;/c:Title&gt;
    &lt;/sel:Context&gt;
    &lt;sel:Source document="canonical.xml" path="/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"/&gt;
  &lt;/sel:Observation&gt;
&lt;/sel:Observations&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "knowledge"}
  (sel:Observation
    {type "paragraph" group "knowledge" source "report" ruleContext "c:Paragraph"}
    (sel:Evidence
      (c:Paragraph {xmlns:c "http://jurgenei.name/canonical"} "Hello from source"))
    (sel:Context
      (c:Title {xmlns:c "http://jurgenei.name/canonical"} "Interfaces"))
    (sel:Source
      {document "canonical.xml"
       path "/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"})))</code></pre></td>
  </tr>
</table>

## Example 2: reusable preset chain

### Input

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;root&gt;
  &lt;person&gt;Jane&lt;/person&gt;
  &lt;employee&gt;John&lt;/employee&gt;
&lt;/root&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(root
  (person "Jane")
  (employee "John"))</code></pre></td>
  </tr>
</table>

### SEL template

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:sel="http://jurgenei.name/sel"&gt;
  &lt;sel:presets&gt;
    &lt;sel:preset id="id-required" type="missing-id" group="quality" copy="."/&gt;
  &lt;/sel:presets&gt;

  &lt;sch:pattern id="identifiers"&gt;
    &lt;sch:rule context="person"&gt;
      &lt;sch:assert test="@id" sel:preset="id-required"&gt;Person must have id.&lt;/sch:assert&gt;
    &lt;/sch:rule&gt;
    &lt;sch:rule context="employee"&gt;
      &lt;sch:assert test="@id" sel:preset="id-required"&gt;Employee must have id.&lt;/sch:assert&gt;
    &lt;/sch:rule&gt;
  &lt;/sch:pattern&gt;
&lt;/sch:schema&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sch:schema
  {xmlns:sch "http://purl.oclc.org/dsdl/schematron"
   xmlns:sel "http://jurgenei.name/sel"}
  (sel:presets
    (sel:preset {id "id-required" type "missing-id" group "quality" copy "."}))
  (sch:pattern
    {id "identifiers"}
    (sch:rule {context "person"}
      (sch:assert {test "@id" sel:preset "id-required"} "Person must have id."))
    (sch:rule {context "employee"}
      (sch:assert {test "@id" sel:preset "id-required"} "Employee must have id."))))</code></pre></td>
  </tr>
</table>

### Output

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sel:Observations xmlns:sel="http://jurgenei.name/sel" group="quality"&gt;
  &lt;sel:Observation type="missing-id" group="quality" source="assert" ruleContext="person"&gt;...&lt;/sel:Observation&gt;
  &lt;sel:Observation type="missing-id" group="quality" source="assert" ruleContext="employee"&gt;...&lt;/sel:Observation&gt;
&lt;/sel:Observations&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "quality"}
  (sel:Observation {type "missing-id" group "quality" source "assert" ruleContext "person"} "...")
  (sel:Observation {type "missing-id" group "quality" source "assert" ruleContext "employee"} "..."))</code></pre></td>
  </tr>
</table>

## Example 3: preset template content with current context

`sel:preset` can carry reusable template fragment. Attribute and text value templates evaluate in
current matched rule context.

### Input

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;Document xmlns="http://jurgenei.name/canonical"&gt;
  &lt;Body&gt;
    &lt;Section&gt;
      &lt;Title&gt;Interfaces&lt;/Title&gt;
      &lt;Paragraph code="p-1"&gt;Source payload&lt;/Paragraph&gt;
    &lt;/Section&gt;
  &lt;/Body&gt;
&lt;/Document&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(c:Document {xmlns:c "http://jurgenei.name/canonical"}
  (c:Body
    (c:Section
      (c:Title "Interfaces")
      (c:Paragraph {code "p-1"} "Source payload"))))</code></pre></td>
  </tr>
</table>

### SEL template

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:c="http://jurgenei.name/canonical"
            xmlns:sel="http://jurgenei.name/sel"&gt;
  &lt;sel:presets&gt;
    &lt;sel:preset id="paragraph-template" type="paragraph" group="knowledge" copy="."&gt;
      &lt;sel:template&gt;
        &lt;sel:Meta code="{@code}"&gt;
          &lt;sel:Summary&gt;{concat(local-name(), ':', normalize-space(.))}&lt;/sel:Summary&gt;
          &lt;sel:Section&gt;{ancestor::c:Section[1]/c:Title}&lt;/sel:Section&gt;
        &lt;/sel:Meta&gt;
      &lt;/sel:template&gt;
    &lt;/sel:preset&gt;
  &lt;/sel:presets&gt;

  &lt;sch:pattern id="knowledge"&gt;
    &lt;sch:rule context="c:Paragraph"&gt;
      &lt;sch:report test="normalize-space(.)" sel:preset="paragraph-template"&gt;
        Paragraph evidence
      &lt;/sch:report&gt;
    &lt;/sch:rule&gt;
  &lt;/sch:pattern&gt;
&lt;/sch:schema&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sch:schema
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
        "Paragraph evidence"))))</code></pre></td>
  </tr>
</table>

### Output

<table style="width:100%; table-layout:fixed; border-collapse:separate; border-spacing:12px 0;">
  <colgroup>
    <col style="width:50%;">
    <col style="width:50%;">
  </colgroup>
  <tr>
    <th>XML</th>
    <th>XIR</th>
  </tr>
  <tr>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-xml">&lt;sel:Observations xmlns:sel="http://jurgenei.name/sel" group="knowledge"&gt;
  &lt;sel:Observation type="paragraph" group="knowledge" source="report" ruleContext="c:Paragraph"&gt;
    &lt;sel:Evidence&gt;
      &lt;c:Paragraph xmlns:c="http://jurgenei.name/canonical" code="p-1"&gt;Source payload&lt;/c:Paragraph&gt;
    &lt;/sel:Evidence&gt;
    &lt;sel:Meta code="p-1"&gt;
      &lt;sel:Summary&gt;Paragraph:Source payload&lt;/sel:Summary&gt;
      &lt;sel:Section&gt;Interfaces&lt;/sel:Section&gt;
    &lt;/sel:Meta&gt;
    &lt;sel:Source document="canonical.xml" path="/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"/&gt;
  &lt;/sel:Observation&gt;
&lt;/sel:Observations&gt;</code></pre></td>
    <td style="padding:0; vertical-align:top;"><pre><code class="language-lisp">(sel:Observations {xmlns:sel "http://jurgenei.name/sel" group "knowledge"}
  (sel:Observation
    {type "paragraph" group "knowledge" source "report" ruleContext "c:Paragraph"}
    (sel:Evidence
      (c:Paragraph {xmlns:c "http://jurgenei.name/canonical" code "p-1"} "Source payload"))
    (sel:Meta {code "p-1"}
      (sel:Summary "Paragraph:Source payload")
      (sel:Section "Interfaces"))
    (sel:Source
      {document "canonical.xml"
       path "/c:Document[1]/c:Body[1]/c:Section[1]/c:Paragraph[1]"})))</code></pre></td>
  </tr>
</table>

## Compiler/runtime boundaries

SEL authoring model stays declarative:

- no `xsl:template`, `xsl:mode`, `xsl:result-document` in Schematron rules.
- output construction lives in generated extraction stylesheet.
- grouped output fan-out uses `xsl:result-document`.

## Summary

SEL language sits on Schematron + XPath + generated XSLT.
It selects evidence, preserves provenance, and feeds downstream terminology/knowledge/graph pipelines
without turning rule layer into full transformation language.
