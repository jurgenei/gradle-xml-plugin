# sel-multi-canonical

Comprehensive Schematron SEL sample using multiple canonical input files.

## What this sample demonstrates

- Compilation of SEL annotations into a reusable extraction stylesheet.
- Extraction from multiple canonical input files in one run.
- Grouped SEL outputs for `knowledge`, `architecture`, and `terminology`.
- Emission of SEL payloads with evidence, context, and source metadata.

## Input files

- `src/main/xml/canonical-order.xml`
- `src/main/xml/canonical-integration.xml`
- `src/main/xml/canonical-glossary.xml`

## Run

```bash
./gradlew -p samples/sel-multi-canonical verifySample
```

Phase-specific extraction example (terminology profile only):

```bash
./gradlew -p samples/sel-multi-canonical extractTerminologySel
```

## Output layout

`build/out/sel/<input-stem>/sel/*.xml`

Examples:

- `build/out/sel/canonical-order/sel/knowledge.xml`
- `build/out/sel/canonical-integration/sel/architecture.xml`
- `build/out/sel/canonical-glossary/sel/terminology.xml`
