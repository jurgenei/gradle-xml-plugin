# transformation/sel

Comprehensive Schematron SEL sample using multiple canonical input files.

## What this sample demonstrates

- Compilation of SEL annotations into a reusable extraction stylesheet.
- Extraction from multiple canonical input files in one run.
- Grouped SEL outputs for `knowledge`, `architecture`, and `terminology`.
- Emission of SEL payloads with evidence, context, and source metadata.
- Reusable `sel:preset` chains in `sel-preset.sch` for shared rule metadata.
- XPath value-template style selectors in `sel:copy` / `sel:context` (including attribute-first fallback).
- XIR source -> XIR target SEL extraction (`extractSelXir`).
- Readable canonical source paths in SEL metadata (`/c:Document/...`).
- Namespace declarations hoisted to top-level SEL map for cleaner XIR output.

## Input files

- `src/main/xml/canonical-order.xml`
- `src/main/xml/canonical-integration.xml`
- `src/main/xml/canonical-glossary.xml`
- `src/main/xir/canonical-order.xir`

## Run

```bash
./gradlew -p samples/transformation/sel verifySample
./gradlew -p samples/transformation/sel runSelfTest
./gradlew -p samples/transformation/sel extractSelXir
./gradlew -p samples/transformation/sel extractSelPreset
```

Phase-specific extraction example (terminology profile only):

```bash
./gradlew -p samples/transformation/sel extractTerminologySel
```

## Output layout

`build/out/sel/<input-stem>/sel/*.xml`
`build/out/sel-xir/<input-stem>/sel/*.xir`

Examples:

- `build/out/sel/canonical-order/sel/knowledge.xml`
- `build/out/sel/canonical-integration/sel/architecture.xml`
- `build/out/sel/canonical-glossary/sel/terminology.xml`
- `build/out/sel-preset/canonical-order/sel/knowledge.xml`
- `build/out/sel-preset/canonical-integration/sel/architecture.xml`
- `build/out/sel-preset/canonical-glossary/sel/terminology.xml`
- `build/out/sel-xir/canonical-order/sel/knowledge.xir`
