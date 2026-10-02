# Schematron-Based SEL Extraction

## Purpose

Use Schematron as a declarative language for corpus SEL extraction.

Instead of building separate:

- SEL DSL
- Fragment DSL
- Corpus DSL

reuse:

- Schematron
- XPath
- SchXslt2
- XSLT

The core insight is:

```text
Validation asks:

    Which nodes are problematic?

SEL Extraction asks:

    Which nodes are interesting?
```

Both are node-selection problems.

---

# Position in the Architecture

```text
OOXML
    ↓
Canonical XML
    ↓
SEL Schematron
    ↓
SEL XML
    ↓
Terminology Mining
    ↓
LLM
    ↓
Knowledge XML
    ↓
Neo4j
```

SELs represent evidence.

SELs do not represent knowledge.

---

# SEL Philosophy

SEL extraction performs:

```text
Selection
Aggregation
Context Preservation
```

SEL extraction does not perform:

```text
Semantic Classification
Knowledge Extraction
Ontology Mapping
```

Good SEL:

```text
This paragraph is interesting.
```

Bad SEL:

```text
This paragraph describes an application.
```

The first is evidence.

The second is interpretation.

---

# Canonical XML Is The Corpus

The canonical repository itself is the corpus.

```text
corpus/

    canonical/

        FD-001.xml
        FD-002.xml
        FD-003.xml
```

SELs are derived artifacts.

```text
Canonical XML
     ↓
SEL Rules
     ↓
SEL XML
```

The corpus remains:

```text
Canonical XML
```

---

# SEL Namespace

```xml
xmlns:obs="http://jurgenei.name/sel"
```

Used as extension attributes in Schematron.

---

# SEL Annotations

## sel:type

Logical sel type.

```xml
sel:type="paragraph"
```

Examples:

```text
paragraph
cell
title
label
connector
acronym
relationship-candidate
```

---

## sel:group

Logical sel destination.

```xml
sel:group="knowledge"
```

Examples:

```text
knowledge
terminology
architecture
```

Groups are logical categories.

Groups are not filenames.

---

## sel:copy

XPath expression describing what evidence should be copied.

Copy current node:

```xml
sel:copy="."
```

Copy containing section:

```xml
sel:copy="ancestor::c:Section[1]"
```

Copy table row:

```xml
sel:copy="ancestor::c:Row[1]"
```

---

## sel:context

XPath expression selecting contextual information.

Example:

```xml
sel:context="ancestor::c:Section[1]/c:Title"
```

Allows:

```text
Evidence
+
Document Context
```

to be emitted together.

---

## sel:presets + sel:preset (reusable metadata)

Define reusable SEL metadata once at schema level:

```xml
<sel:presets xmlns:sel="http://jurgenei.name/sel">
  <sel:preset id="id-required"
              type="missing-id"
              group="quality"
              copy="."
              context="ancestor::Section[1]/Title"/>
</sel:presets>
```

Reference from any `sch:assert` / `sch:report`:

```xml
<sch:assert test="@id" sel:preset="id-required">
  Missing id
</sch:assert>
```

Multiple presets are supported with space-separated ids:

```xml
sel:preset="base id-required"
```

Merge order:

```text
defaults -> referenced preset(s) in declared order -> inline sel:* attributes
```

So inline values always override preset values.

Unknown preset ids and duplicate preset definitions fail fast.

---

# Example Rule

```xml
<sch:rule context="c:Paragraph">

    <sch:report
        test="normalize-space(.)"
        sel:type="paragraph"
        sel:group="knowledge"
        sel:copy="."
        sel:context="ancestor::c:Section[1]/c:Title">

        Knowledge paragraph

    </sch:report>

</sch:rule>
```

---

# Example SEL

```xml
<sel:SEL
    type="paragraph">

    <sel:Evidence>

        <c:Paragraph>
            SAP sends customer data to Vortex.
        </c:Paragraph>

    </sel:Evidence>

    <sel:Context>

        <c:Title>
            Interfaces
        </c:Title>

    </sel:Context>

</sel:SEL>
```

---

# SEL Output Groups

Rules target logical groups.

Example:

```xml
sel:group="knowledge"
```

Compiler/configuration resolves:

```text
knowledge
   ↓
knowledge.xml
```

Example mapping:

```text
knowledge
    → sels/knowledge.xml

terminology
    → sels/terminology.xml

architecture
    → sels/architecture.xml
```

Rule authors never reference physical filenames.

---

# SEL Profiles

Use Schematron phases.

---

## Terminology Profile

Focus:

```text
Titles
Table Headers
Diagram Labels
Acronyms
Glossary Entries
```

Output:

```text
terminology sels
```

---

## Knowledge Profile

Focus:

```text
Paragraphs
Cells
Table Rows
References
```

Output:

```text
knowledge sels
```

---

## Architecture Profile

Focus:

```text
Shapes
Labels
Connectors
Architecture Tables
```

Output:

```text
architecture sels
```

---

# Diagram Support

Diagrams are first-class evidence.

Many enterprise relationships are expressed visually before they are expressed textually.

---

## Example Diagram Rule

```xml
<sch:rule context="c:Connector">

    <sch:report
        test="@source and @target"
        sel:type="relationship-candidate"
        sel:group="architecture"
        sel:copy=".">

        Connector candidate

    </sch:report>

</sch:rule>
```

---

## Example Diagram SEL

```xml
<sel:SEL
    type="relationship-candidate">

    <sel:Evidence>

        <c:Connector
            source="sap"
            target="vortex"/>

    </sel:Evidence>

</sel:SEL>
```

---

# SchXslt2 Extension Strategy

Current flow:

```text
Schematron
    ↓
SchXslt2
    ↓
Validation Stylesheet
    ↓
SVRL
```

Proposed flow:

```text
Schematron
    ↓
SchXslt2
    ↓
Validation Stylesheet
    ↓
SEL Meta Transform
    ↓
SEL Stylesheet
    ↓
SEL XML
```

Result:

```text
One Rule Base

    ↓

Validation

and

SEL Extraction
```

---

# Output Strategy

SEL extraction should support:

```text
SVRL generation

SEL generation

Grouped sel output

Multiple output documents
```

using:

```xslt
xsl:result-document
```

generated by the sel compiler rather than authored directly in rules.

---

# Allowed XSLT Features

Allowed:

```text
XPath

XSLT functions

XSLT variables

XSLT accumulators
```

Examples:

```xml
<xsl:function/>

<xsl:variable/>

<xsl:accumulator/>
```

These can support rule evaluation.

---

# Avoid Inside Rules

Avoid:

```text
xsl:template

xsl:mode

xsl:result-document

xsl:element

xsl:copy

xsl:choose
```

inside Schematron rules.

Rules should remain declarative.

Output construction belongs to the generated sel stylesheet.

---

# Automatic Rule Generation

Initial sel profiles can be generated automatically from the canonical vocabulary.

Example canonical elements:

```xml
<c:Title/>
<c:Paragraph/>
<c:Cell/>
<c:Label/>
<c:Connector/>
```

Bootstrap rules:

```xml
<sch:rule context="c:Title"/>

<sch:rule context="c:Paragraph"/>

<sch:rule context="c:Cell"/>

<sch:rule context="c:Label"/>

<sch:rule context="c:Connector"/>
```

These provide the initial corpus extraction profile.

Human effort focuses only on high-value refinement.

---

# SEL Model

Keep SEL XML intentionally small.

```xml
<sel:SEL>

    <sel:Type/>

    <sel:Evidence/>

    <sel:Context/>

    <sel:Source/>

</sel:SEL>
```

Everything else belongs in:

```text
Canonical XML
```

or

```text
Knowledge XML
```

not SEL XML.

---

# Provenance

Every sel should preserve provenance.

Example:

```xml
<sel:Source
    document="FD-123.docx"
    version="4.2"
    path="/1/3/7"/>
```

The source document must always remain traceable.

---

# Future Processing

SEL XML becomes input for:

```text
Terminology Mining

Acronym Discovery

Entity Candidate Detection

Relationship Candidate Detection

LLM Enrichment

Knowledge Graph Creation
```

---

# Key Benefits

```text
No SEL DSL

No Fragment DSL

No Corpus DSL

Reuse XPath

Reuse Schematron

Reuse SchXslt2

Reuse XSLT

One Rule Base

One Validation Model

One SEL Model
```

Schematron becomes a declarative evidence-selection language for corpus-to-graph workflows while remaining understandable to anyone already familiar with XPath and Schematron.