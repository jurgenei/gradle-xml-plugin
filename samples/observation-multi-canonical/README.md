# observation-multi-canonical

Comprehensive Schematron observation sample using multiple canonical input files.

## What this sample demonstrates

- Compilation of observation annotations into a reusable extraction stylesheet.
- Extraction from multiple canonical input files in one run.
- Grouped observation outputs for `knowledge`, `architecture`, and `terminology`.
- Emission of observation payloads with evidence, context, and source metadata.

## Input files

- `src/main/xml/canonical-order.xml`
- `src/main/xml/canonical-integration.xml`
- `src/main/xml/canonical-glossary.xml`

## Run

```bash
./gradlew -p samples/observation-multi-canonical verifySample
```

## Output layout

`build/out/observations/<input-stem>/observations/*.xml`

Examples:

- `build/out/observations/canonical-order/observations/knowledge.xml`
- `build/out/observations/canonical-integration/observations/architecture.xml`
- `build/out/observations/canonical-glossary/observations/terminology.xml`

