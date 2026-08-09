# Schematron-Based Observation Extraction

## Purpose

Use Schematron as a declarative language for corpus observation extraction.

Instead of building separate:

- Observation DSL
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

Observation Extraction asks:

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
Observation Schematron
    ↓
Observation XML
    ↓
Terminology Mining
    ↓
LLM
    ↓
Knowledge XML
    ↓
Neo4j
```

Observations represent evidence.

Observations do not represent knowledge.

---

# Observation Philosophy

Observation extraction performs:

```text
Selection
Aggregation
Context Preservation
```

Observation extraction does not perform:

```text
Semantic Classification
Knowledge Extraction
Ontology Mapping
```

Good Observation:

```text
This paragraph is interesting.
```

Bad Observation:

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

Observations are derived artifacts.

```text
Canonical XML
     ↓
Observation Rules
     ↓
Observation XML
```

The corpus remains:

```text
Canonical XML
```

---

# Observation Namespace

```xml
xmlns:obs="http://jurgenei.name/observation"
```

Used as extension attributes in Schematron.

---

# Observation Annotations

## obs:emit

Enable observation generation.

```xml
obs:emit="true"
```

---

## obs:type

Logical observation type.

```xml
obs:type="paragraph"
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

## obs:group

Logical observation destination.

```xml
obs:group="knowledge"
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

## obs:copy

XPath expression describing what evidence should be copied.

Copy current node:

```xml
obs:copy="."
```

Copy containing section:

```xml
obs:copy="ancestor::c:Section[1]"
```

Copy table row:

```xml
obs:copy="ancestor::c:Row[1]"
```

---

## obs:context

XPath expression selecting contextual information.

Example:

```xml
obs:context="ancestor::c:Section[1]/c:Title"
```

Allows:

```text
Evidence
+
Document Context
```

to be emitted together.

---

# Example Rule

```xml
<sch:rule context="c:Paragraph">

    <sch:report
        test="normalize-space(.)"
        obs:emit="true"
        obs:type="paragraph"
        obs:group="knowledge"
        obs:copy="."
        obs:context="ancestor::c:Section[1]/c:Title">

        Knowledge paragraph

    </sch:report>

</sch:rule>
```

---

# Example Observation

```xml
<obs:Observation
    type="paragraph">

    <obs:Evidence>

        <c:Paragraph>
            SAP sends customer data to Vortex.
        </c:Paragraph>

    </obs:Evidence>

    <obs:Context>

        <c:Title>
            Interfaces
        </c:Title>

    </obs:Context>

</obs:Observation>
```

---

# Observation Output Groups

Rules target logical groups.

Example:

```xml
obs:group="knowledge"
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
    → observations/knowledge.xml

terminology
    → observations/terminology.xml

architecture
    → observations/architecture.xml
```

Rule authors never reference physical filenames.

---

# Observation Profiles

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
terminology observations
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
knowledge observations
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
architecture observations
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
        obs:emit="true"
        obs:type="relationship-candidate"
        obs:group="architecture"
        obs:copy=".">

        Connector candidate

    </sch:report>

</sch:rule>
```

---

## Example Diagram Observation

```xml
<obs:Observation
    type="relationship-candidate">

    <obs:Evidence>

        <c:Connector
            source="sap"
            target="vortex"/>

    </obs:Evidence>

</obs:Observation>
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
Observation Meta Transform
    ↓
Observation Stylesheet
    ↓
Observation XML
```

Result:

```text
One Rule Base

    ↓

Validation

and

Observation Extraction
```

---

# Output Strategy

Observation extraction should support:

```text
SVRL generation

Observation generation

Grouped observation output

Multiple output documents
```

using:

```xslt
xsl:result-document
```

generated by the observation compiler rather than authored directly in rules.

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

Output construction belongs to the generated observation stylesheet.

---

# Automatic Rule Generation

Initial observation profiles can be generated automatically from the canonical vocabulary.

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

# Observation Model

Keep observation XML intentionally small.

```xml
<obs:Observation>

    <obs:Type/>

    <obs:Evidence/>

    <obs:Context/>

    <obs:Source/>

</obs:Observation>
```

Everything else belongs in:

```text
Canonical XML
```

or

```text
Knowledge XML
```

not Observation XML.

---

# Provenance

Every observation should preserve provenance.

Example:

```xml
<obs:Source
    document="FD-123.docx"
    version="4.2"
    path="/1/3/7"/>
```

The source document must always remain traceable.

---

# Future Processing

Observation XML becomes input for:

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
No Observation DSL

No Fragment DSL

No Corpus DSL

Reuse XPath

Reuse Schematron

Reuse SchXslt2

Reuse XSLT

One Rule Base

One Validation Model

One Observation Model
```

Schematron becomes a declarative evidence-selection language for corpus-to-graph workflows while remaining understandable to anyone already familiar with XPath and Schematron.