# Gradle XML Transform Plugin

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

[![Build and Test](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/gradle-build.yml)
[![Coverage CI](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml/badge.svg)](https://github.com/jurgenei/gradle-xml-plugin/actions/workflows/coverage.yml)
[![Coverage](https://codecov.io/gh/jurgenei/gradle-xml-plugin/branch/main/graph/badge.svg)](https://codecov.io/gh/jurgenei/gradle-xml-plugin)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21+-green.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/gradle-9.5+-blue.svg)](https://gradle.org/)
[![Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/name.jurgenei.gradle.xml?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/name.jurgenei.gradle.xml)

A Gradle plugin providing **Saxon**-backed XSLT/XQuery transforms and SVRL-based XML validation tasks with an orthogonal, Gradle-style DSL.

## Overview

Define and execute XPath/XSLT/XQuery transformations and XML validations as Gradle tasks with:

- File-tree input matching (include/exclude patterns)
- Output file generation with configurable extension mapping
- External parameter passing to transforms
- Optional parallel processing using **virtual threads**
- SVRL and optional JUnit XML reporting for validation

The plugin contributes four task types:

- `name.jurgenei.gradle.xml.XsltTask` — XSLT 3.0 transformations
- `name.jurgenei.gradle.xml.XQueryTask` — XQuery transformations
- `name.jurgenei.gradle.xml.SchematronTask` — Schematron to SVRL validation
- `name.jurgenei.gradle.xml.XsdTask` — XSD validation normalized to SVRL

Both share a near-orthogonal API for unified Gradle-style configuration.

## Features

- **Saxon HE** XSLT 3.0 and XQuery execution
- **Schematron validation** via SchXslt2 transpiler (`name.dmaus.schxslt:schxslt2`)
- **XSD validation** with AUTO engine resolution (Saxon PE/EE when available, JAXP fallback on HE)
- **Orthogonal task API** — both task types inherit the same base configuration
- **File-tree DSL** — Ant-like include/exclude filtering via Gradle's native `fileTree`
- **Flexible output mapping** — custom extension and output directory per task
- **Parameter passing** — externalize stylesheet/query variables
- **Virtual-thread parallelism** — optional worker pool for concurrent file processing (default: serial)
- **Comprehensive testing** — JUnit 4 integration tests with mirrored XSLT/XQuery scenarios

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
- Plugin Portal page: https://plugins.gradle.org/plugin/name.jurgenei.gradle.xml

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
```

## Validation Examples (Groovy DSL)

```groovy
tasks.register('validateSchematron', name.jurgenei.gradle.xml.SchematronTask) {
  schema 'src/main/schematron/rules.sch'
  source(fileTree('src/main/xml') { include '**/*.xml' })
  outputDir.set(layout.buildDirectory.dir('reports/schematron'))
  reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
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

## Run tests

```bash
gradle test
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
gradle build
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
- `samples/xquery-basic`
- `samples/validation-basic`

See `samples/README.md` for run commands.

### Testing

JUnit 4 with Gradle TestKit for functional integration testing:

```bash
gradle test --tests '*XsltTaskIntegrationTest'
gradle test --tests '*XQueryTaskIntegrationTest'
gradle test --tests '*SchematronTaskIntegrationTest'
gradle test --tests '*XsdTaskIntegrationTest'
```

### Code Style

- Java 21+ source
- Javadoc on all public APIs and classes
- Text blocks for multiline strings (Java 15+)

## Contributing

Contribution workflow and coding expectations are documented in `CONTRIBUTING.md`.

## License

[MIT](LICENSE)
