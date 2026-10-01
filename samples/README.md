# Samples

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

Minimal runnable sample projects for the XML Gradle plugin.

Each sample resolves the local plugin implementation through:

```groovy
pluginManagement {
    includeBuild("../..")
}
```

## Available samples

- `xslt-basic` - transform one XML with XSLT
- `s-xslt-xir-identity` - identity transform with XIR input, XIR stylesheet, and XIR output
- `xquery-basic` - transform one XML with XQuery
- `s-xquery-xir-identity` - identity transform with XIR input and output, preserving namespace/comment/PI nodes
- `s-xsd` - validate XIR data against XIR XSD schema
- `s-schematron` - validate XIR data against XIR Schematron schema
- `validation-basic` - validate XML with XSD and Schematron (SVRL/JUnit), including a persisted compiled Schematron stylesheet (`style`) and transpiler params
- `sel-multi-canonical` - compile and extract grouped SEL payloads from multiple canonical XML inputs
- `schematron-bootstrap-ooxml` - bootstrap Schematron from `gradle-ooxml-plugin` canonical schema URL, then validate canonical XML

## Run samples

From repository root:

```bash
./gradlew -p samples/xslt-basic runXslt
./gradlew -p samples/s-xslt-xir-identity runXslt
./gradlew -p samples/xquery-basic runXQuery
./gradlew -p samples/s-xquery-xir-identity runXQuery
./gradlew -p samples/s-xsd runSXsd
./gradlew -p samples/s-schematron runSSchematron
./gradlew -p samples/validation-basic runXsd runSchematron
./gradlew -p samples/sel-multi-canonical compileSel extractSel
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```

## Smoke-test samples

Each sample provides a tiny `verifySample` task that runs the sample task(s)
and asserts expected output files exist.

```bash
./gradlew -p samples/xslt-basic verifySample
./gradlew -p samples/s-xslt-xir-identity verifySample
./gradlew -p samples/xquery-basic verifySample
./gradlew -p samples/s-xquery-xir-identity verifySample
./gradlew -p samples/s-xsd verifySample
./gradlew -p samples/s-schematron verifySample
./gradlew -p samples/validation-basic verifySample
./gradlew -p samples/sel-multi-canonical verifySample
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
./gradlew -p samples/sel-multi-canonical verifySample
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```
