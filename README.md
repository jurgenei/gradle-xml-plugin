# Gradle XML Transform Plugin

[![Build and Test](https://github.com/jurgenei/GradleXmlTransform/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/jurgenei/GradleXmlTransform/actions/workflows/gradle-build.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21+-green.svg)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/gradle-9.5+-blue.svg)](https://gradle.org/)

A Gradle plugin providing **Saxon HE**-backed XSLT and XQuery transformation tasks with an orthogonal, Gradle-style DSL.

## Overview

Define and execute XPath/XSLT/XQuery transformations as Gradle tasks with:

- File-tree input matching (include/exclude patterns)
- Output file generation with configurable extension mapping
- External parameter passing to transforms
- Optional parallel processing using **virtual threads**

The plugin contributes two task types:

- `name.jurgenei.gradle.xml.XsltTask` — XSLT 3.0 transformations
- `name.jurgenei.gradle.xml.XQueryTask` — XQuery transformations

Both share a near-orthogonal API for unified Gradle-style configuration.

## Features

- **Saxon HE** XSLT 3.0 and XQuery execution
- **Orthogonal task API** — both task types inherit the same base configuration
- **File-tree DSL** — Ant-like include/exclude filtering via Gradle's native `fileTree`
- **Flexible output mapping** — custom extension and output directory per task
- **Parameter passing** — externalize stylesheet/query variables
- **Virtual-thread parallelism** — optional worker pool for concurrent file processing (default: serial)
- **Comprehensive testing** — JUnit 4 integration tests with mirrored XSLT/XQuery scenarios

## Plugin id

`name.jurgenei.gradle.xml`

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

## Run tests

```bash
gradle test
```

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

### Testing

JUnit 4 with Gradle TestKit for functional integration testing:

```bash
gradle test --tests '*XsltTaskIntegrationTest'
gradle test --tests '*XQueryTaskIntegrationTest'
```

### Code Style

- Java 21+ source
- Javadoc on all public APIs and classes
- Text blocks for multiline strings (Java 15+)

## License

[MIT](LICENSE)
