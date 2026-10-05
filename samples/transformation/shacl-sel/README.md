# transformation/shacl-sel

SHACL-Sel sample with Collibra-style case data.

## Scope

Sample-only fixtures (not shipped in plugin artifact):

- ApplicationComponent
- DatabaseTable
- DatabaseColumn
- DataDomain
- DataSet
- DataOwner

## What this sample demonstrates

- SHACL Collibra schema as test data in `src/main/shacl/collibra-model.shacl.xml`
- SHACL -> Schematron SEL bridge compilation (`compileShaclSel`)
- Grouped SEL extraction runtime (`extractShaclSel`)
- Committed transpiler snapshots in `expected/shacl-sel.sch` and `expected/shacl-sel.xsl`
- Self-test execution via XSpec (`runSelfTest`)

## Run

```bash
./gradlew -p samples/transformation/shacl-sel compileShaclSel
./gradlew -p samples/transformation/shacl-sel extractShaclSel
./gradlew -p samples/transformation/shacl-sel syncExpectedShaclSel
./gradlew -p samples/transformation/shacl-sel verifyExpectedShaclSel
./gradlew -p samples/transformation/shacl-sel runSelfTest
./gradlew -p samples/transformation/shacl-sel verifySample
```

## Output layout

- `build/generated/sel/shacl-sel.sch`
- `build/generated/sel/shacl-sel.xsl`
- `build/out/shacl-sel/<input-stem>/sel/relations.xml`
- `expected/shacl-sel.sch`
- `expected/shacl-sel.xsl`
