# Samples

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

Minimal runnable sample projects for XML Gradle plugin, organized by **project type**.

Each sample resolves local plugin implementation through `pluginManagement.includeBuild(...)` in sample `settings.gradle`.

## Available samples

### Validation

- `validation/xsd` - XSD validation scenarios as tasks:
  - `runSXsd` (XIR input + XIR schema)
  - `runXsd` (XML inputs + XML XSD schema)
- `validation/schematron` - Schematron validation scenarios as tasks:
  - `runSSchematron` (XIR schema/input with transpiler stylesheet)
  - `runSchematron` (XML schema/input with persisted compiled stylesheet)

### Transformation

- `transformation/xslt` - XSLT scenarios as tasks:
  - `runXslt` (XML input/output)
  - `runXsltXir` (XIR input/output)
  - `runXsltIdentityXir` (XIR identity scenario with XIR stylesheet)
- `transformation/xquery` - XQuery scenarios as tasks:
  - `runXQuery` (XML input/output)
  - `runXQueryXir` (XIR input/output)
  - `runXQueryIdentity` (XIR identity scenario preserving namespace/comment/PI)
- `transformation/sel` - compile and extract grouped SEL payloads from multiple canonical XML inputs, including `sel-preset.sch` preset-reuse showcase.

### Special scenario

- `schematron-bootstrap-ooxml` - bootstrap Schematron from `gradle-ooxml-plugin` canonical schema URL, then validate canonical XML.

## Run samples

From repository root:

```bash
./gradlew -p samples/transformation/xslt runXslt runXsltXir runXsltIdentityXir
./gradlew -p samples/transformation/xquery runXQuery runXQueryXir runXQueryIdentity
./gradlew -p samples/validation/xsd runSXsd runXsd
./gradlew -p samples/validation/schematron runSSchematron runSchematron
./gradlew -p samples/transformation/sel compileSel extractSel extractSelPreset
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```

Run module self-tests:

```bash
./gradlew -p samples/transformation/xslt runSelfTest
./gradlew -p samples/transformation/xquery runSelfTest
./gradlew -p samples/validation/schematron runSelfTest
./gradlew -p samples/validation/xsd runSelfTest
./gradlew -p samples/transformation/sel runSelfTest
./gradlew -p samples/schematron-bootstrap-ooxml runSelfTest
```

## Smoke-test samples

Each sample provides a `verifySample` task. XSLT, XQuery, Schematron, XSD, SEL, and
Schematron bootstrap samples route `verifySample` through `runSelfTest` (XSpec).

```bash
./gradlew -p samples/transformation/xslt verifySample
./gradlew -p samples/transformation/xquery verifySample
./gradlew -p samples/validation/xsd verifySample
./gradlew -p samples/validation/schematron verifySample
./gradlew -p samples/transformation/sel verifySample
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```
