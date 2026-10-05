# Gradle XML Transform Plugin

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

[![Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/name.jurgenei.gradle.xml?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/name.jurgenei.gradle.xml)
[![Build and Test](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml)
[![JUnit Report](https://img.shields.io/badge/JUnit-Report-blue?logo=githubactions)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml)
[![Coverage CI](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml)
[![CodeQL](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/codeql.yml)
[![Dependency Check](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/dependency-check.yml)
[![SpotBugs Security](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/spotbugs-security.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/spotbugs-security.yml)
[![Checkstyle](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/checkstyle.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/checkstyle.yml)
[![Dependabot](https://img.shields.io/badge/dependabot-enabled-025E8C?logo=dependabot)](https://github.com/jurgenei/gradle-xml-plugin/security/dependabot)
[![Coverage](https://codecov.io/gh/jurgenei/gradle-xml-plugin/graph/badge.svg?branch=main)](https://codecov.io/gh/jurgenei/gradle-xml-plugin)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21+-green.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/gradle-9.5+-blue.svg)](https://gradle.org/)

A Gradle plugin providing **Saxon**-backed XSLT/XQuery transforms and SVRL-based XML validation tasks with an orthogonal, Gradle-style DSL.

## Overview

Define and execute XPath/XSLT/XQuery transformations and XML validations as Gradle tasks with:

- File-tree input matching (include/exclude patterns)
- Explicit single-file mode via Ant-like `input(...)` / `output(...)`
- Output file generation with configurable extension mapping
- External parameter passing to transforms
- Optional parallel processing using **virtual threads**
- SVRL and optional JUnit XML reporting for validation

The plugin contributes task types:

- `name.jurgenei.gradle.xml.XsltTask` — XSLT 3.0 transformations
- `name.jurgenei.gradle.xml.XQueryTask` — XQuery transformations
- `name.jurgenei.gradle.xml.SchematronTask` — Schematron to SVRL validation
- `name.jurgenei.gradle.xml.XsdTask` — XSD validation normalized to SVRL
- `name.jurgenei.gradle.xml.SchematronBootstrapTask` — bootstrap Schematron from XSD
- `name.jurgenei.gradle.xml.SchematronSelCompileTask` — compile `sel:*` annotated Schematron into grouped SEL stylesheet skeleton
- `name.jurgenei.gradle.xml.SchematronSelExtractTask` — execute runtime SEL extraction and emit grouped SEL XML
- `name.jurgenei.gradle.xml.ShaclSelCompileTask` — compile SHACL relation shapes into grouped SEL stylesheet skeleton (+ generated Schematron bridge)
- `name.jurgenei.gradle.xml.ShaclSelExtractTask` — execute runtime SEL extraction from SHACL-compiled stylesheet
- `name.jurgenei.gradle.xml.XSpecCompileTask` — compile XSLT XSpec (`.xspec`) into executable runner stylesheet
- `name.jurgenei.gradle.xml.XSpecTask` — run XSpec (from `.xspec` or precompiled runner) and emit XML + JUnit reports

Both share a near-orthogonal API for unified Gradle-style configuration.

## Features

- **Saxon HE** XSLT 3.0 and XQuery execution
- **Schematron validation** via SchXslt2 transpiler (`name.dmaus.schxslt:schxslt2`)
- **XSD validation** with AUTO engine resolution (Saxon PE/EE when available, JAXP fallback on HE)
- **Orthogonal task API** — both task types inherit the same base configuration
- **File-tree DSL** — Ant-like include/exclude filtering via Gradle's native `fileTree`
- **Single-file DSL** — explicit one-to-one transforms via `input(...)` and `output(...)`
- **Flexible output mapping** — custom extension and output directory per task
- **Parameter passing** — externalize stylesheet/query variables
- **Virtual-thread parallelism** — optional worker pool for concurrent file processing (default: serial)
- **Comprehensive testing** — JUnit 4 integration tests with mirrored XSLT/XQuery scenarios
- **Security automation** — CodeQL, OWASP Dependency-Check, SpotBugs + FindSecBugs, Dependabot
- **XIR I/O** — `.xir` input and output routing for XSLT/XQuery tasks
- **Canonical JSON I/O** — optional `.json` input/output routing with reversible element mapping
- **Native XSpec support (XSLT v1)** — two-phase compile/run with XML + JUnit reporting

## XIR Support

XIR support provides a compact, human- and AI-friendly representation of XML and XDM-based technologies. Rather than introducing new semantics, it offers an alternative serialization syntax for established standards such as XML, XDM, XPath, XSLT, and XML Schema.

By reducing serialization overhead while preserving structure, typing, and validation capabilities, XIR makes it easier to work with existing XML assets in modern development and AI workflows. All processing continues to rely on the same mature standards and implementations that have evolved within the XML ecosystem for more than two decades.

`XsltTask` and `XQueryTask` support `.xir` files in file-tree mode and explicit mode.

S-expression runtime ships inside `gradle-xml-plugin` artifact.

- Internal package: `name.jurgenei.gradle.xml.xir`
- No separate `name.jurgenei.xml:xml-xir` dependency required
- S-expression parser/serializer runtime is Saxon-agnostic (`java.xml` SAX/JAXP APIs)

- Input `.xir` is parsed as SAX source.
- XSLT stylesheet may also be `.xir` (for `XsltTask.style(...)`).
- Output `.xir` is serialized from XML result events through SAX/JAXP pipeline.
- Saxon URI dereferencing routes `.xir` resources through the same SAX parser path for `doc()` and `collection()` calls.
- `outputFormat` controls output style: `compact` (default) or `beautified`.
  (`xirFormat` and `xformat` remain supported as legacy aliases.)

XIR format details:

- `()` = nodes
- `{}` = associative structures
- `[]` = sequences
- `.` = document node head
- `?` = processing instruction head
- `!` = comment node head

Canonical examples:

- Element node: `(book (title "XML"))`
- Element associative block (attributes + namespaces): `(book { id "b1" xmlns:m "urn:math" } (m:title "XML"))`
- Document with XML declaration map: `(. { version "1.0" encoding "UTF-8" } (book))`
- Map node: `(xdm:map { name "John" age 42 })`
- Array node: `(xdm:array [ "A" "B" "C" ])`
- Typed atomics: `(xs:boolean true)`, `(xs:date "2026-09-06")`
- Comment: `(! "text")`
- Processing instruction: `(?xml-stylesheet { href "main.xsl" type "text/xsl" })`

Disambiguation:

- `(map ...)` and `(array ...)` are XML elements named `map`/`array`.
- XDM map/array nodes use explicit heads: `xdm:map` and `xdm:array`.

Serializer compatibility modes:

- Canonical mode (default): canonical token classes and forms shown above
- Legacy mode: retained for compatibility output only

Internal bridge note:

- SAX cannot represent XDM map/array/typed-atomic/xml-declaration directly.
- Runtime uses internal `xdm:*` helper elements in URI `urn:name.jurgenei.gradle.xml:xdm` as lossless bridge between parser and serializer.

`outputFormat` is also reused for canonical JSON output formatting.

Format conventions:

```lisp
; compact
(book {id "b1"} (title "XML"))

; beautified
(book
  {id "b1"}
  (title "XML"))
```

### XSLT Example

```groovy
tasks.register('xmlToXir', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/xslt'))
  outputExtension.set('.xir')
}

tasks.register('xirToXml', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  input 'build/out/xslt/input.xir'
  output 'build/out/xml/result.xml'
}
```

## Canonical JSON Support

`XsltTask` and `XQueryTask` support optional canonical JSON parsing/serialization.

- Canonical JSON maps XML element trees to JSON objects with `type`, `name`, `attributes`, `children`.
- Canonical JSON mode is reversible for XML -> JSON -> XML roundtrips.
- `outputFormat` controls canonical JSON output style too: `compact` or `beautified`.

Set JSON routing mode with `outputMode` (`jsonMode` and `mode` remain supported as legacy aliases):

- `auto` (default): canonical parser for `.json` input; for `.json` output, try canonical hierarchical JSON first and fall back to native Saxon JSON when canonical serialization is not applicable (for example map/array results)
- `native`: no canonical JSON parser for input; for `.json` output, same canonical-first behavior with native fallback
- `canonical`: canonical parser + canonical serializer for `.json` input/output (no fallback)

### XSLT Canonical JSON Example

```groovy
tasks.register('xmlToJsonCanonical', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/json'))
  outputExtension.set('.json')
  outputMode('canonical')
  outputFormat.set('beautified')
}

tasks.register('jsonCanonicalToXml', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  input 'build/out/json/input.json'
  output 'build/out/xml/result.xml'
  outputMode('canonical')
}
```

### Beautified Output Switch

Kotlin DSL:

```kotlin
tasks.register<name.jurgenei.gradle.xml.XsltTask>("xmlToXir") {
    style("src/main/xslt/identity.xsl")
    source("src/main/xml/input.xml")
    outputDir.set(layout.buildDirectory.dir("out/xslt"))
    outputExtension.set(".xir")
    outputFormat.set("beautified")
}
```

Groovy DSL:

```groovy
tasks.register('xmlToXir', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/xslt'))
  outputExtension.set('.xir')
  outputFormat.set('beautified')
}
```

## Input/Output Modes

`XsltTask` and `XQueryTask` support two equivalent execution modes:

- **File-tree mode**: set `source(...)` and `outputDir`
- **Explicit single-file mode**: set `input(...)` and `output(...)`

Notes:

- In explicit mode, `input(...)` and `output(...)` must be set together.
- In file-tree mode, `outputDir` is required.
- Both modes support `param(...)`; file-tree mode additionally supports `workers` and extension-based mapping.

## Validation API Contract

Validation tasks share a common contract (`ValidationTaskSpec`) and defaults:

- `outputExtension = '.svrl.xml'`
- `workers = 1`
- `format = SVRL` (`reportFormat` remains supported as legacy alias)
- `failOnError = true`
- `junitOutputDir = build/reports/xml-validation/junit`

`ReportFormat` values:

- `SVRL`
- `JUNIT`
- `SVRL_AND_JUNIT`

`XsdTask` supports `XsdEngine` values:

- `AUTO` (default; prefers Saxon schema-aware, otherwise JAXP)
- `SAXON`
- `JAXP`

## Plugin ID and Coordinates

- Supported plugin ID: `name.jurgenei.gradle.xml`
- Maven artifact for legacy `buildscript` usage: `name.jurgenei.gradle:gradle-xml-transform:<version>`
- Obsolete/legacy IDs from earlier docs are no longer supported.

## Installation

Add to `build.gradle.kts`:

```kotlin
plugins {
    id("name.jurgenei.gradle.xml")
}
```

Or `build.gradle`:

```groovy
plugins {
    id 'name.jurgenei.gradle.xml'
}
```

Legacy `buildscript` usage:

```kotlin
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("name.jurgenei.gradle:gradle-xml-transform:0.1.1")
    }
}

apply(plugin = "name.jurgenei.gradle.xml")
```

## Example (Kotlin DSL)

```kotlin
plugins {
    id("name.jurgenei.gradle.xml")
}

tasks.register<name.jurgenei.gradle.xml.XsltTask>("transformDocs") {
    style("src/main/xslt/main.xsl")
    source(fileTree("src/main/xml") {
        include("**/*.xml")
        exclude("**/legacy/**")
    })
    outputDir.set(layout.buildDirectory.dir("generated/xslt"))
    outputExtension.set(".html")
    workers.set(4)
    param("env", "dev")
}

tasks.register<name.jurgenei.gradle.xml.XQueryTask>("queryDocs") {
    query("src/main/xquery/main.xq")
    source("src/main/xml/single.xml")
    outputDir.set(layout.buildDirectory.dir("generated/xquery"))
    outputExtension.set(".xml")
    workers.set(1)
    param("tenant", "acme")
}

tasks.register<name.jurgenei.gradle.xml.XsltTask>("transformOne") {
    style("src/main/xslt/main.xsl")
    input("src/main/xml/a.xml")
    output("build/custom/b.xml")
}

tasks.register<name.jurgenei.gradle.xml.XQueryTask>("queryOne") {
    query("src/main/xquery/main.xq")
    input("src/main/xml/a.xml")
    output("build/custom/b.xml")
}
```

## Example (Groovy DSL)

```groovy
plugins {
  id 'name.jurgenei.gradle.xml'
}

tasks.register('transformDocs', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/main.xsl'
  source(fileTree('src/main/xml') {
    include '**/*.xml'
    exclude '**/legacy/**'
  })
  outputDir.set(layout.buildDirectory.dir('generated/xslt'))
  outputExtension.set('.html')
  workers.set(4)
  param 'env', 'dev'
}

tasks.register('queryDocs', name.jurgenei.gradle.xml.XQueryTask) {
  query 'src/main/xquery/main.xq'
  source 'src/main/xml/single.xml'
  outputDir.set(layout.buildDirectory.dir('generated/xquery'))
  outputExtension.set('.xml')
  workers.set(1)
  param 'tenant', 'acme'
}

tasks.register('transformOne', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/main.xsl'
  input 'src/main/xml/a.xml'
  output 'build/custom/b.xml'
}

tasks.register('queryOne', name.jurgenei.gradle.xml.XQueryTask) {
  query 'src/main/xquery/main.xq'
  input 'src/main/xml/a.xml'
  output 'build/custom/b.xml'
}
```

## Validation Examples (Groovy DSL)

```groovy
tasks.register('validateSchematron', name.jurgenei.gradle.xml.SchematronTask) {
  schema 'src/main/schematron/rules.sch'
  // Optional persistent compiled stylesheet cache.
  style 'build/generated/schematron/rules.compiled.xsl'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/schematron'))
  format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
  // Optional SchXslt transpiler parameters.
  phase.set('#ALL')
  severityThreshold.set('warning')
  workers.set(4)
  failOnError.set(false)
}

tasks.register('validateXsd', name.jurgenei.gradle.xml.XsdTask) {
  schema 'src/main/xsd/schema.xsd'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/xsd'))
  format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
  engine.set(name.jurgenei.gradle.xml.validation.XsdEngine.AUTO)
}
```

Schematron-specific options:

- `style(...)`/`style.set(...)` (optional): persistent location for compiled Schematron XSLT.
  - When unset, a temp compiled stylesheet is used per validation run.
  - When set, recompilation is skipped if the compiled stylesheet is newer than inputs and transpiler parameters are unchanged.
- `transpilerStylesheet(...)` (optional): override bundled SchXslt transpiler.
- Optional SchXslt transpiler parameter properties (only passed when explicitly set):
  - `debug`, `phase`, `expandText`, `streamable`, `locationFunction`, `failEarly`
  - `terminateValidationOnError`, `reportActivePattern`, `reportFiredRule`, `reportSuppressedRule`
  - `reportSkippedAssertion`, `compactReport`, `severityThreshold`, `defaultSeverity`, `defaultFrom`
  - `checkAssembledSchema`, `handleDynamicErrors`

## Schematron Bootstrap From XSD

Use `SchematronBootstrapTask` to create an initial SEL Schematron from an XSD.
The generated file is comprehensive (captures required children/attributes as SEL profiles)
but intentionally passing (bootstrap-safe) until you tighten rules manually.

Safety behavior:

- If output `.sch` already exists, bootstrap does **not** overwrite it.
- The task logs a lifecycle warning and exits.

Cross-plugin workflow (OOXML + XML plugins):

```groovy
plugins {
  id 'name.jurgenei.gradle.ooxml'
  id 'name.jurgenei.gradle.xml'
}

tasks.register('bootstrapCanonicalSchematron', name.jurgenei.gradle.xml.SchematronBootstrapTask) {
  def ooxmlExt = project.extensions.getByType(name.jurgenei.gradle.ooxml.OoXmlExtension)
  schemaUrl(ooxmlExt.canonicalSchemaUrl.get())
  output 'src/main/schematron/canonical-sel.sch'
}

tasks.register('copyCanonicalXsd') {
  doLast {
    def ooxmlExt = project.extensions.getByType(name.jurgenei.gradle.ooxml.OoXmlExtension)
    def target = file('src/main/xsd/canonical.local.xsd')
    if (!target.exists()) {
      target.parentFile.mkdirs()
      target.text = new URL(ooxmlExt.canonicalSchemaUrl.get()).getText('UTF-8')
    }
  }
}

tasks.register('bootstrapFromLocalXsd', name.jurgenei.gradle.xml.SchematronBootstrapTask) {
  dependsOn tasks.named('copyCanonicalXsd')
  schemaFile.set(layout.projectDirectory.file('src/main/xsd/canonical.local.xsd'))
  output 'src/main/schematron/canonical-local.sch'
}

tasks.register('validateCanonicalSchematron', name.jurgenei.gradle.xml.SchematronTask) {
  dependsOn tasks.named('bootstrapCanonicalSchematron')
  schema.set(layout.projectDirectory.file('src/main/schematron/canonical-sel.sch'))
  source 'src/main/xml/canonical.xml'
  outputDir.set(layout.buildDirectory.dir('reports/schematron'))
}
```

## SEL Compiler Skeleton (Phase 2)

`SchematronSelCompileTask` compiles `sel:*` rule metadata into an extraction stylesheet skeleton
with grouped `xsl:result-document` outputs.

Reusable SEL metadata can be defined once and referenced from many rules (SQF-style):

```xml
<sel:presets xmlns:sel="http://jurgenei.name/sel">
  <sel:preset id="id-required"
              type="missing-id"
              group="quality"
              copy="."/>
</sel:presets>

<sch:rule context="person">
  <sch:assert test="@id" sel:preset="id-required">
    Person must have id.
  </sch:assert>
</sch:rule>
```

Presets can also provide reusable output template fragments. Template text/attributes support value templates
evaluated in the current matched rule context:

```xml
<sel:presets xmlns:sel="http://jurgenei.name/sel"
             xmlns:c="http://jurgenei.name/canonical">
  <sel:preset id="paragraph-template" type="paragraph" group="knowledge" copy=".">
    <sel:template>
      <sel:Meta code="{@code}">
        <sel:Summary>{concat(local-name(), ':', normalize-space(.))}</sel:Summary>
        <sel:Section>{ancestor::c:Section[1]/c:Title}</sel:Section>
      </sel:Meta>
    </sel:template>
  </sel:preset>
</sel:presets>
```

Preset merge order is deterministic: defaults -> referenced preset(s) in declared order -> inline `sel:*` attributes on the assert/report node (inline wins).

```groovy
tasks.register('compileSel', name.jurgenei.gradle.xml.SchematronSelCompileTask) {
  schema 'src/main/schematron/sel.sch'
  output 'build/generated/sel/sel.xsl'
  phase '#DEFAULT' // '#DEFAULT' | '#ALL' | explicit phase id
  outputNamespaceUri 'http://jurgenei.name/sel'   // optional task-level override
  outputNamespacePrefix 'sel'                     // optional; '' => default namespace output
  groupOutput 'knowledge', 'sel/knowledge.xml'
  groupOutput 'terminology', 'sel/terminology.xml'
  groupOutput 'architecture', 'sel/architecture.xml'
}
```

Schema-level defaults can be declared once and are used unless task-level override is set:

```xml
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:sel="http://jurgenei.name/sel"
            sel:outputNamespaceUri="http://jurgenei.name/sel"
            sel:outputNamespacePrefix="sel">
```

## SEL Runtime Extraction (Phase 3)

`SchematronSelExtractTask` executes SEL extraction against canonical XML, XIR (`.xir`), or canonical JSON (`.json`) inputs and emits grouped outputs.
It can either:

- compile extraction style on the fly from `schema`, or
- consume a precompiled style via `style`.

```groovy
tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
  schema 'src/main/schematron/sel.sch'
  // Optional if precompiled by SchematronSelCompileTask:
  // style 'build/generated/sel/sel.xsl'
  phase 'knowledge-phase' // used when style is compiled on-the-fly from schema
  outputNamespaceUri 'http://jurgenei.name/sel'  // optional task-level override
  outputNamespacePrefix 'sel'                    // optional; '' => default namespace output
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/sel'))
  groupOutput 'knowledge', 'sel/knowledge.xml'
  groupOutput 'terminology', 'sel/terminology.xml'
  groupOutput 'architecture', 'sel/architecture.xml'
  // XIR targets are supported by using .xir output paths:
  // groupOutput 'knowledge', 'sel/knowledge.xir'
  // outputFormat.set('beautified')
  outputMode('auto')
  failOnError.set(true)
}
```

Phase behavior for SEL compile/extract tasks:

- `#DEFAULT` (default): uses `sch:schema/@defaultPhase`; if absent, all patterns are active.
- `#ALL`: all SEL-annotated rules are compiled.
- explicit phase id: only rules whose owning pattern is activated via `<sch:phase><sch:active pattern='...'/></sch:phase>`.

## SHACL-Sel Compile + Extract

`ShaclSelCompileTask` and `ShaclSelExtractTask` follow same compile/extract shape as Schematron SEL tasks.

Current SHACL compile input is RDF/XML and targets relation-oriented SEL extraction.

```groovy
tasks.register('compileShaclSel', name.jurgenei.gradle.xml.ShaclSelCompileTask) {
  schema 'src/main/shacl/collibra-model.shacl.xml'
  output 'build/generated/sel/shacl-sel.xsl'
  outputSchematron 'build/generated/sel/shacl-sel.sch'
  groupOutput 'relations', 'sel/relations.xml'
}

tasks.register('extractShaclSel', name.jurgenei.gradle.xml.ShaclSelExtractTask) {
  dependsOn tasks.named('compileShaclSel')
  schema 'build/generated/sel/shacl-sel.sch'
  style 'build/generated/sel/shacl-sel.xsl'
  source(fileTree('src/main/xml') { include '*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/shacl-sel'))
  groupOutput 'relations', 'sel/relations.xml'
  failOnError.set(true)
}
```

Sample `samples/transformation/shacl-sel` also commits snapshot artifacts for discoverability:

- `expected/shacl-sel.sch`
- `expected/shacl-sel.xsl`

## XSpec Compile (Phase 2)

`XSpecCompileTask` transpiles XSLT XSpec files (`.xspec`) into executable runner stylesheets.

```groovy
tasks.register('compileXSpec', name.jurgenei.gradle.xml.XSpecCompileTask) {
  source(fileTree('src/main/xspec') { include '**/*.xspec' })
  outputDir.set(layout.buildDirectory.dir('generated/xspec'))
  outputExtension.set('.xspec.xsl') // default
  failOnError.set(true)
}
```

## XSpec Run (Phase 3)

`XSpecTask` executes XSpec and emits XML + JUnit reports.

v1 scope is **XSLT XSpec only**.

Runtime inputs:

- `.xspec` (auto-compiles internally, then runs), or
- precompiled runner stylesheet (`.xsl`) from `XSpecCompileTask`.

```groovy
tasks.register('runXSpec', name.jurgenei.gradle.xml.XSpecTask) {
  // Either source .xspec files (auto-compile)...
  source(fileTree('src/main/xspec') { include '**/*.xspec' })
  outputDir.set(layout.buildDirectory.dir('reports/xspec'))

  // ...or explicit precompiled runner input:
  // input 'build/generated/xspec/sample.xspec.xsl'
  // output 'build/reports/xspec/sample-report.xml'

  junitOutputDir.set(layout.buildDirectory.dir('reports/xspec/junit'))
  failOnError.set(true)
}
```

## Run tests

```bash
./gradlew test
```

## Test Coverage

Generate coverage report and enforce the current minimum line coverage baseline (>= 0%):

```bash
./gradlew coverage
```

Coverage report outputs:

- XML: `build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `build/reports/jacoco/test/html/index.html`

CI coverage workflow: `.github/workflows/coverage.yml`

To enable Codecov upload/badge, add repository secret `CODECOV_TOKEN`.

## Security Scanning

Security automation runs in GitHub Actions:

- CodeQL static analysis: `.github/workflows/codeql.yml`
- OWASP Dependency-Check: `.github/workflows/dependency-check.yml`
- SpotBugs + FindSecBugs: `.github/workflows/spotbugs-security.yml`
- Dependabot updates: `.github/dependabot.yml`

Set repository secret `NVD_API_KEY` for faster/more reliable Dependency-Check NVD lookups.

Run locally:

```bash
./gradlew dependencyCheckAnalyze --no-configuration-cache
./gradlew spotbugsMain -PspotbugsIgnoreFailures=false --no-configuration-cache
./gradlew allSecurityChecks
```

## Building

```bash
./gradlew build
```

Required Java version: **21+**

## Architecture

### Task Hierarchy

```
AbstractXmlTransformTask (shared base)
  ├── XsltTask (XSLT transformations)
  └── XQueryTask (XQuery transformations)

AbstractXmlValidationTask (shared base)
  ├── SchematronTask (Schematron validation)
  └── XsdTask (XSD validation)

AbstractXmlTransformTask (XSpec additions)
  ├── XSpecCompileTask (compile .xspec to runner stylesheet)
  └── XSpecTask (execute XSpec runner, emit XML + JUnit)
```

### Execution Flow

1. Resolve input files from `source` / `fileset`
2. Sort files deterministically
3. Optionally parallelize using virtual-thread worker pool (if `workers > 1`)
4. For each input file:
   - Skip when output is newer than transform dependencies (source + style/query/schema)
   - Derive output file path using `outputExtension` mapping
   - Create output directories (thread-safe via `Files.createDirectories`)
   - Compile and execute transform (XSLT or XQuery)
   - Log success or collect failure

### Parallelism

- `workers = 1` (default): Sequential processing
- `workers > 1`: Fixed virtual-thread pool with concurrent file processing

Virtual threads are used to maximize throughput with minimal memory overhead for I/O-bound XML transformations.

## Development

### Samples

Runnable minimal examples are available under `samples/`:

- `samples/transformation/xslt`
- `samples/transformation/xquery`
- `samples/transformation/sel`
- `samples/transformation/shacl-sel`
- `samples/validation/xsd`
- `samples/validation/schematron`
- `samples/schematron-bootstrap-ooxml`

See `samples/README.md` for run commands.

### Testing

JUnit 4 with Gradle TestKit for functional integration testing:

```bash
./gradlew test --tests '*XsltTaskIntegrationTest'
./gradlew test --tests '*XQueryTaskIntegrationTest'
./gradlew test --tests '*SchematronTaskIntegrationTest'
./gradlew test --tests '*XsdTaskIntegrationTest'
./gradlew test --tests '*SchematronBootstrapTaskIntegrationTest'
./gradlew test --tests '*SchematronSelCompileTaskIntegrationTest'
./gradlew test --tests '*SchematronSelExtractTaskIntegrationTest'
./gradlew test --tests '*ShaclSelCompileTaskIntegrationTest'
./gradlew test --tests '*ShaclSelExtractTaskIntegrationTest'
./gradlew test --tests '*XSpecCompileTaskIntegrationTest'
./gradlew test --tests '*XSpecTaskIntegrationTest'
```

Sample module self-tests (XSpec-driven):

```bash
./gradlew -p samples/transformation/xslt runSelfTest
./gradlew -p samples/transformation/xquery runSelfTest
./gradlew -p samples/validation/schematron runSelfTest
./gradlew -p samples/validation/xsd runSelfTest
./gradlew -p samples/transformation/sel runSelfTest
./gradlew -p samples/transformation/shacl-sel runSelfTest
./gradlew -p samples/schematron-bootstrap-ooxml runSelfTest
```

### Code Style

- Java 21+ source
- Javadoc on all public APIs and classes
- Text blocks for multiline strings (Java 15+)

## Contributing

Contribution workflow and coding expectations are documented in `CONTRIBUTING.md`.

## License

[MIT](LICENSE)
