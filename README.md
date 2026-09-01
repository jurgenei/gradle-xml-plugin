# Gradle XML Transform Plugin

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

[![Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/name.jurgenei.gradle.xml?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/name.jurgenei.gradle.xml)
[![Build and Test](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml)
[![Coverage CI](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml)
[![Coverage](https://codecov.io/gh/jurgenei/gradle-xml-plugin/branch/main/graph/badge.svg)](https://codecov.io/gh/jurgenei/gradle-xml-plugin)
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

The plugin contributes four task types:

- `name.jurgenei.gradle.xml.XsltTask` — XSLT 3.0 transformations
- `name.jurgenei.gradle.xml.XQueryTask` — XQuery transformations
- `name.jurgenei.gradle.xml.SchematronTask` — Schematron to SVRL validation
- `name.jurgenei.gradle.xml.XsdTask` — XSD validation normalized to SVRL
- `name.jurgenei.gradle.xml.SchematronBootstrapTask` — bootstrap Schematron from XSD
- `name.jurgenei.gradle.xml.SchematronObservationCompileTask` — compile `obs:*` annotated Schematron into grouped observation stylesheet skeleton
- `name.jurgenei.gradle.xml.SchematronExtractTask` — execute runtime observation extraction and emit grouped observation XML

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
- **S-expression I/O** — `.sexpr` input and output routing for XSLT/XQuery tasks
- **Canonical JSON I/O** — optional `.json` input/output routing with reversible element mapping

## S-expression Support (MVP)

`XsltTask` and `XQueryTask` support `.sexpr` files in file-tree mode and explicit mode.

S-expression runtime ships inside `gradle-xml-plugin` artifact.

- Internal package: `name.jurgenei.gradle.xml.sexpr`
- No separate `name.jurgenei.xml:xml-sexpr` dependency required

- Input `.sexpr` is parsed as SAX source.
- XSLT stylesheet may also be `.sexpr` (for `XsltTask.style(...)`).
- Output `.sexpr` is serialized from Saxon result tree.
- `sexprFormat` controls output style: `compact` (default) or `beautified`.

S-expression format details:

- Attributes: `[id "b1" version "1.0"]`
- Namespaces:
  - default: `[ns "http://www.w3.org/1998/Math/MathML"]`
  - prefixed: `[ns "m" "http://www.w3.org/1998/Math/MathML"]`
- Comments: `(# "text")`
- Processing instructions: `(?xml-stylesheet type="text/xsl" href="style.xsl")`

`sexprFormat` is also reused for canonical JSON output formatting.

Format conventions:

```lisp
; compact
(book [id "b1"] (title "XML"))

; beautified
(book
  [id "b1"]
  (title "XML"))
```

### Syntax Migration (Hard Cut)

Old syntax removed. New bracket syntax required.

| XML concept | Old (removed) | New (required) |
|---|---|---|
| Attribute | `(@id "b1")` | `[id "b1"]` |
| Multiple attributes | `(@id "b1") (@version "1.0")` | `[id "b1" version "1.0"]` |
| Default namespace | n/a | `[ns "http://www.w3.org/1998/Math/MathML"]` |
| Prefixed namespace | n/a | `[ns "m" "http://www.w3.org/1998/Math/MathML"]` |
| Comment | n/a | `(# "this is a comment")` |
| Processing instruction | n/a | `(?xml-stylesheet type="text/xsl" href="style.xsl")` |

Processing-instruction values must be quoted.

### XSLT Example

```groovy
tasks.register('xmlToSexpr', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/xslt'))
  outputExtension.set('.sexpr')
}

tasks.register('sexprToXml', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  input 'build/out/xslt/input.sexpr'
  output 'build/out/xml/result.xml'
}
```

## Canonical JSON Support

`XsltTask` and `XQueryTask` support optional canonical JSON parsing/serialization.

- Canonical JSON maps XML element trees to JSON objects with `type`, `name`, `attributes`, `children`.
- Canonical JSON mode is reversible for XML -> JSON -> XML roundtrips.
- `sexprFormat` controls canonical JSON output style too: `compact` or `beautified`.

Set JSON routing mode with `jsonMode`:

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
  jsonMode.set('canonical')
  sexprFormat.set('beautified')
}

tasks.register('jsonCanonicalToXml', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  input 'build/out/json/input.json'
  output 'build/out/xml/result.xml'
  jsonMode.set('canonical')
}
```

### Beautified Output Switch

Kotlin DSL:

```kotlin
tasks.register<name.jurgenei.gradle.xml.XsltTask>("xmlToSexpr") {
    style("src/main/xslt/identity.xsl")
    source("src/main/xml/input.xml")
    outputDir.set(layout.buildDirectory.dir("out/xslt"))
    outputExtension.set(".sexpr")
    sexprFormat.set("beautified")
}
```

Groovy DSL:

```groovy
tasks.register('xmlToSexpr', name.jurgenei.gradle.xml.XsltTask) {
  style 'src/main/xslt/identity.xsl'
  source 'src/main/xml/input.xml'
  outputDir.set(layout.buildDirectory.dir('out/xslt'))
  outputExtension.set('.sexpr')
  sexprFormat.set('beautified')
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
- `reportFormat = SVRL`
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
  reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
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
  reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
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

Use `SchematronBootstrapTask` to create an initial observation Schematron from an XSD.
The generated file is comprehensive (captures required children/attributes as observations)
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
  output 'src/main/schematron/canonical-observation.sch'
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
  schema.set(layout.projectDirectory.file('src/main/schematron/canonical-observation.sch'))
  source 'src/main/xml/canonical.xml'
  outputDir.set(layout.buildDirectory.dir('reports/schematron'))
}
```

## Observation Compiler Skeleton (Phase 2)

`SchematronObservationCompileTask` compiles `obs:*` rule metadata into an extraction stylesheet skeleton
with grouped `xsl:result-document` outputs.

```groovy
tasks.register('compileObservation', name.jurgenei.gradle.xml.SchematronObservationCompileTask) {
  schema 'src/main/schematron/observations.sch'
  output 'build/generated/observation/observations.xsl'
  groupOutput 'knowledge', 'observations/knowledge.xml'
  groupOutput 'terminology', 'observations/terminology.xml'
  groupOutput 'architecture', 'observations/architecture.xml'
}
```

## Observation Runtime Extraction (Phase 3)

`SchematronExtractTask` executes observation extraction against canonical XML and emits grouped outputs.
It can either:

- compile extraction style on the fly from `schema`, or
- consume a precompiled style via `style`.

```groovy
tasks.register('extractObservations', name.jurgenei.gradle.xml.SchematronExtractTask) {
  schema 'src/main/schematron/observations.sch'
  // Optional if precompiled by SchematronObservationCompileTask:
  // style 'build/generated/observation/observations.xsl'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/observations'))
  groupOutput 'knowledge', 'observations/knowledge.xml'
  groupOutput 'terminology', 'observations/terminology.xml'
  groupOutput 'architecture', 'observations/architecture.xml'
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

- `samples/xslt-basic`
- `samples/xslt-sexpr-identity`
- `samples/xquery-basic`
- `samples/xquery-sexpr-identity`
- `samples/s-xsd`
- `samples/s-schematron`
- `samples/validation-basic`

See `samples/README.md` for run commands.

### Testing

JUnit 4 with Gradle TestKit for functional integration testing:

```bash
./gradlew test --tests '*XsltTaskIntegrationTest'
./gradlew test --tests '*XQueryTaskIntegrationTest'
./gradlew test --tests '*SchematronTaskIntegrationTest'
./gradlew test --tests '*XsdTaskIntegrationTest'
./gradlew test --tests '*SchematronBootstrapTaskIntegrationTest'
./gradlew test --tests '*SchematronObservationCompileTaskIntegrationTest'
./gradlew test --tests '*SchematronExtractTaskIntegrationTest'
```

### Code Style

- Java 21+ source
- Javadoc on all public APIs and classes
- Text blocks for multiline strings (Java 15+)

## Contributing

Contribution workflow and coding expectations are documented in `CONTRIBUTING.md`.

## License

[MIT](LICENSE)
