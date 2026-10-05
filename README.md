# Gradle XML Validate and Transform Plugin

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

Plugin gives Gradle-native tasks for XML transformations and validations with Saxon-backed execution.
It supports XML, XIR, and canonical JSON routing, with one orthogonal DSL across task types.

## Introduction and Overview

Use this plugin when project needs repeatable XML processing in build pipeline:

- Transform XML with XSLT or XQuery
- Validate XML with XSD or Schematron
- Bootstrap Schematron from XSD
- Compile and run SEL/SHACL extraction flows
- Compile and run XSpec test suites

Main task types:

- `name.jurgenei.gradle.xml.XsltTask`
- `name.jurgenei.gradle.xml.XQueryTask`
- `name.jurgenei.gradle.xml.SchematronTask`
- `name.jurgenei.gradle.xml.XsdTask`
- `name.jurgenei.gradle.xml.SchematronBootstrapTask`
- `name.jurgenei.gradle.xml.SchematronSelCompileTask`
- `name.jurgenei.gradle.xml.SchematronSelExtractTask`
- `name.jurgenei.gradle.xml.ShaclSelCompileTask`
- `name.jurgenei.gradle.xml.ShaclSelExtractTask`
- `name.jurgenei.gradle.xml.XSpecCompileTask`
- `name.jurgenei.gradle.xml.XSpecTask`

Plugin ID: `name.jurgenei.gradle.xml`

```kotlin
plugins {
    id("name.jurgenei.gradle.xml")
}
```

## Features

- Saxon HE execution for XSLT and XQuery
- Validation outputs normalized to SVRL (+ optional JUnit for validation/XSpec)
- File-tree mode and explicit single-file mode
- Parallel workers with virtual threads
- Parameter passing via `param(name, value)`
- XIR input/output support
- Canonical JSON input/output support
- Orthogonal output naming:
  - `outputFormat` (`compact`, `beautified`)
  - `outputMode` (`auto`, `native`, `canonical`)
- Validation format naming unchanged:
  - `format(...)` / `reportFormat` (`SVRL`, `JUNIT`, `SVRL_AND_JUNIT`)

## XIR Support

XIR is alternate syntax for XML/XDM structures, not new data model.
Think: same information as XML, different representation optimized for compactness and readability in diffs and generated artifacts.

### Why XIR

- Less punctuation noise vs XML
- Easier nested-structure scanning in code review
- Roundtrip-friendly with existing XML tooling path
- Works with same transformation and validation pipeline

### XIR syntax relation to XML and XDM

| XIR token | Meaning | XML/XDM equivalent |
|---|---|---|
| `(...)` | node expression | element/document node structure |
| `{...}` | associative payload | attributes + namespace bindings, or XDM map value |
| `[...]` | sequence payload | XDM sequence / array value |
| `.` | document node head | XML document root wrapper |
| `?` | PI node head | processing instruction |
| `!` | comment node head | XML comment |

Disambiguation:

- `{...}` denotes XDM map value.
- `[...]` denotes XDM sequence/array value.
- `(map ...)` and `(array ...)` are XML elements unless mapped from XPath Functions XML namespace bridge.
- If first child of element is map value, explicit empty attribute block can disambiguate:
  - `(element {} { key value })`

<details>
<summary>XML and XIR side-by-side example</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<book id="b1" xmlns:m="urn:math">
  <title>XML</title>
</book>
```

```lisp
(. { version "1.0" encoding "UTF-8" }
  (book { id "b1" xmlns:m "urn:math" }
    (title "XML")))
```
</details>

Runtime note:

- XIR parser/serializer ships inside plugin artifact.
- No extra runtime dependency needed.

## Transformations

Transformations use `XsltTask` and `XQueryTask`.

### Ways to work with files

1. File-tree mode: `source(...)` + `outputDir`
2. Explicit mode: `input(...)` + `output(...)`

### Common transformation options

- `outputExtension`
- `outputMethod` (`xml`, `json`, `text`)
- `outputFormat` (`compact`, `beautified`) for XIR/canonical JSON style
- `outputMode` (`auto`, `native`, `canonical`) for JSON routing
- `workers`
- `failOnError`
- `param(name, value)`

### XSLT transformation

<details>
<summary>Input/output example in XML and XIR</summary>

```xml
<book id="b1"><title>XML</title></book>
```

```lisp
(book {id "b1"} (title "XML"))
```
</details>

<details>
<summary>Groovy Task Def (XSLT)</summary>

```groovy
tasks.register('xmlToXir', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('out/xslt'))
  outputExtension.set('.xir')
  outputFormat.set('beautified')
  outputMode('auto')
  workers.set(4)
  param 'tenant', 'acme'
}
```
</details>

<details>
<summary>Kotlin Task Def (XSLT)</summary>

```kotlin
tasks.register<name.jurgenei.gradle.xml.XsltTask>("xmlToXir") {
    style("src/main/xslt/identity.xsl")
    source(fileTree("src/main/xml") { include("**/*.xml") })
    outputDir.set(layout.buildDirectory.dir("out/xslt"))
    outputExtension.set(".xir")
    outputFormat.set("beautified")
    outputMode("auto")
    workers.set(4)
    param("tenant", "acme")
}
```
</details>

### XQuery transformation

<details>
<summary>Input/output example in XML and XIR</summary>

```xml
<book id="b1"><title>XML</title></book>
```

```lisp
(book {id "b1"} (title "XML"))
```
</details>

<details>
<summary>Groovy Task Def (XQuery)</summary>

```groovy
tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
  query 'src/main/xquery/main.xq'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/xquery'))
  outputExtension.set('.json')
  outputMode('canonical')
  outputFormat.set('beautified')
}
```
</details>

<details>
<summary>Kotlin Task Def (XQuery)</summary>

```kotlin
tasks.register<name.jurgenei.gradle.xml.XQueryTask>("runXQuery") {
    query("src/main/xquery/main.xq")
    source("src/main/xml/input.xml")
    outputDir.set(layout.buildDirectory.dir("out/xquery"))
    outputExtension.set(".json")
    outputMode("canonical")
    outputFormat.set("beautified")
}
```
</details>

### Canonical JSON mode details

- `outputMode('auto')`: parse JSON canonically on input; output tries canonical-first with native fallback.
- `outputMode('native')`: no canonical input parser; output still canonical-first with native fallback.
- `outputMode('canonical')`: canonical input and canonical output only.

Legacy aliases still work:

- `jsonMode` / `mode`
- `xirFormat` / `xformat`

### SEL and SHACL transformation pipelines

- `SchematronSelCompileTask`: compile `sel:*` metadata to extraction stylesheet
- `SchematronSelExtractTask`: run extraction and emit grouped outputs
- `ShaclSelCompileTask` + `ShaclSelExtractTask`: same compile/extract shape for SHACL-driven flows

## Validations

Validation tasks use shared contract:

- `outputExtension` (default `.svrl.xml`)
- `workers` (default `1`)
- `failOnError` (default `true`)
- `format(...)` / `reportFormat` values:
  - `SVRL`
  - `JUNIT`
  - `SVRL_AND_JUNIT`

### XSD validation (`XsdTask`)

Engine options:

- `AUTO` (default; chooses best available engine)
- `SAXON`
- `JAXP`

<details>
<summary>XML and XIR validation inputs</summary>

```xml
<root><wrong>bad</wrong></root>
```

```lisp
(root (wrong "bad"))
```
</details>

<details>
<summary>Groovy Task Def (XSD validation)</summary>

```groovy
tasks.register('validateXsd', name.jurgenei.gradle.xml.XsdTask) {
  schema 'src/main/xsd/schema.xsd'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/xsd'))
  format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
  engine.set(name.jurgenei.gradle.xml.validation.XsdEngine.AUTO)
  failOnError.set(false)
}
```
</details>

<details>
<summary>Kotlin Task Def (XSD validation)</summary>

```kotlin
tasks.register<name.jurgenei.gradle.xml.XsdTask>("validateXsd") {
    schema("src/main/xsd/schema.xsd")
    source(fileTree("src/main/xml") { include("**/*.xml") })
    outputDir.set(layout.buildDirectory.dir("reports/xsd"))
    format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
    engine.set(name.jurgenei.gradle.xml.validation.XsdEngine.AUTO)
    failOnError.set(false)
}
```
</details>

### Schematron validation (`SchematronTask`)

Schematron options include:

- `style(...)` for persistent compiled stylesheet cache
- optional `transpilerStylesheet(...)`
- SchXslt parameter set: `phase`, `severityThreshold`, `streamable`, `compactReport`, and others

<details>
<summary>Groovy Task Def (Schematron validation)</summary>

```groovy
tasks.register('validateSchematron', name.jurgenei.gradle.xml.SchematronTask) {
  schema 'src/main/schematron/rules.sch'
  style 'build/generated/schematron/rules.compiled.xsl'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/schematron'))
  format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
  phase.set('#ALL')
  severityThreshold.set('warning')
  workers.set(4)
  failOnError.set(false)
}
```
</details>

<details>
<summary>Kotlin Task Def (Schematron validation)</summary>

```kotlin
tasks.register<name.jurgenei.gradle.xml.SchematronTask>("validateSchematron") {
    schema("src/main/schematron/rules.sch")
    style("build/generated/schematron/rules.compiled.xsl")
    source(fileTree("src/main/xml") { include("**/*.xml") })
    outputDir.set(layout.buildDirectory.dir("reports/schematron"))
    format(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
    phase.set("#ALL")
    severityThreshold.set("warning")
    workers.set(4)
    failOnError.set(false)
}
```
</details>

### Schematron bootstrap from XSD

`SchematronBootstrapTask` can generate initial Schematron from XSD and does not overwrite existing target file.

## XSpec testing

### Current native scope

- `XSpecCompileTask`: compile `.xspec` to executable runner
- `XSpecTask`: run XSpec and emit XML + JUnit reports
- Current built-in scope: XSLT XSpec

### Scenarios

1. **XSLT scenario**: direct `.xspec` compile/run flow.
2. **XQuery scenario**: validate XQuery outputs through integration tasks, then assert results in test layer (JUnit/TestKit).
3. **XSD scenario**: run `XsdTask`, assert SVRL/JUnit outputs in pipeline.
4. **Schematron scenario**: run `SchematronTask`, assert SVRL/JUnit outputs, optionally combine with XSpec around downstream XSLT assets.

<details>
<summary>Groovy Task Def (XSpec compile + run)</summary>

```groovy
tasks.register('compileXSpec', name.jurgenei.gradle.xml.XSpecCompileTask) {
  source(fileTree('src/main/xspec') { include '**/*.xspec' })
  outputDir.set(layout.buildDirectory.dir('generated/xspec'))
  outputExtension.set('.xspec.xsl')
}

tasks.register('runXSpec', name.jurgenei.gradle.xml.XSpecTask) {
  dependsOn tasks.named('compileXSpec')
  source(fileTree('src/main/xspec') { include '**/*.xspec' })
  outputDir.set(layout.buildDirectory.dir('reports/xspec'))
  junitOutputDir.set(layout.buildDirectory.dir('reports/xspec/junit'))
  failOnError.set(true)
}
```
</details>

<details>
<summary>Kotlin Task Def (XSpec compile + run)</summary>

```kotlin
tasks.register<name.jurgenei.gradle.xml.XSpecCompileTask>("compileXSpec") {
    source(fileTree("src/main/xspec") { include("**/*.xspec") })
    outputDir.set(layout.buildDirectory.dir("generated/xspec"))
    outputExtension.set(".xspec.xsl")
}

tasks.register<name.jurgenei.gradle.xml.XSpecTask>("runXSpec") {
    dependsOn(tasks.named("compileXSpec"))
    source(fileTree("src/main/xspec") { include("**/*.xspec") })
    outputDir.set(layout.buildDirectory.dir("reports/xspec"))
    junitOutputDir.set(layout.buildDirectory.dir("reports/xspec/junit"))
    failOnError.set(true)
}
```
</details>

## Code Style

- Java 21+
- Public API/classes documented with Javadoc
- Multiline literals use text blocks where practical
- Build scripts follow standard Gradle Groovy/Kotlin DSL idioms

## Build, Test, Security

```bash
./gradlew build
./gradlew test
./gradlew coverage
./gradlew allSecurityChecks
```

Coverage outputs:

- `build/reports/jacoco/test/jacocoTestReport.xml`
- `build/reports/jacoco/test/html/index.html`

## Architecture (quick view)

```text
AbstractXmlTransformTask
  ├── XsltTask
  ├── XQueryTask
  ├── XSpecCompileTask
  └── XSpecTask

AbstractXmlValidationTask
  ├── SchematronTask
  └── XsdTask
```

## Samples

See runnable examples under `samples/`:

- `samples/transformation/xslt`
- `samples/transformation/xquery`
- `samples/transformation/sel`
- `samples/transformation/shacl-sel`
- `samples/validation/xsd`
- `samples/validation/schematron`
- `samples/schematron-bootstrap-ooxml`

## Contributing

Workflow and expectations: `CONTRIBUTING.md`

## License

[MIT](LICENSE)
