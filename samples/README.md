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
- `xslt-sexpr-identity` - identity transform with S-expression input, S-expression stylesheet, and S-expression output
- `xquery-basic` - transform one XML with XQuery
- `xquery-sexpr-identity` - identity transform with S-expression input and output, preserving namespace/comment/PI nodes
- `s-xsd` - validate S-expression data against S-expression XSD schema
- `s-schematron` - validate S-expression data against S-expression Schematron schema
- `validation-basic` - validate XML with XSD and Schematron (SVRL/JUnit), including a persisted compiled Schematron stylesheet (`style`) and transpiler params
- `observation-multi-canonical` - compile and extract grouped observations from multiple canonical XML inputs
- `schematron-bootstrap-ooxml` - bootstrap Schematron from `gradle-ooxml-plugin` canonical schema URL, then validate canonical XML

## Run samples

From repository root:

```bash
./gradlew -p samples/xslt-basic runXslt
./gradlew -p samples/xslt-sexpr-identity runXslt
./gradlew -p samples/xquery-basic runXQuery
./gradlew -p samples/xquery-sexpr-identity runXQuery
./gradlew -p samples/s-xsd runSXsd
./gradlew -p samples/s-schematron runSSchematron
./gradlew -p samples/validation-basic runXsd runSchematron
./gradlew -p samples/observation-multi-canonical compileObservation extractObs
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```

## Smoke-test samples

Each sample provides a tiny `verifySample` task that runs the sample task(s)
and asserts expected output files exist.

```bash
./gradlew -p samples/xslt-basic verifySample
./gradlew -p samples/xslt-sexpr-identity verifySample
./gradlew -p samples/xquery-basic verifySample
./gradlew -p samples/xquery-sexpr-identity verifySample
./gradlew -p samples/s-xsd verifySample
./gradlew -p samples/s-schematron verifySample
./gradlew -p samples/validation-basic verifySample
./gradlew -p samples/observation-multi-canonical verifySample
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
./gradlew -p samples/observation-multi-canonical verifySample
./gradlew -p samples/schematron-bootstrap-ooxml verifySample
```
