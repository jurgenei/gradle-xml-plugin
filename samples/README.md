# Samples

Minimal runnable sample projects for the XML Gradle plugin.

Each sample resolves the local plugin implementation through:

```groovy
pluginManagement {
    includeBuild("../..")
}
```

## Available samples

- `xslt-basic` - transform one XML with XSLT
- `xquery-basic` - transform one XML with XQuery
- `validation-basic` - validate XML with XSD and Schematron (SVRL and JUnit reports)

## Run samples

From repository root:

```bash
./gradlew -p samples/xslt-basic runXslt
./gradlew -p samples/xquery-basic runXQuery
./gradlew -p samples/validation-basic runXsd runSchematron
```

