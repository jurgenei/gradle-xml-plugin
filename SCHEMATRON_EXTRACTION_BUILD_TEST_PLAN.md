# Schematron Extraction Build and Test Plan

## Purpose

This plan operationalizes `schematron-based-extraction.md` into implementable build phases, task breakdown, and verification gates.

It targets:

- `gradle-xml-plugin` as the extraction engine owner
- `gradle-ooxml-plugin` as the canonical XML producer and integration consumer

## Scope and Ownership

- `gradle-xml-plugin` owns:
  - observation rule annotations (`obs:*`)
  - bootstrap and compile pipeline for extraction
  - observation XML emission and grouping
- `gradle-ooxml-plugin` owns:
  - canonical model/schema generation (`canonical.xsd`)
  - integration sample/tests that consume XML plugin capabilities

Rule: `gradle-xml-plugin` has no dependency on other `gradle-*` plugins.

## Architecture Targets

Build toward this runtime chain:

1. Canonical XML input
2. Observation Schematron (`.sch`)
3. SchXslt transpilation
4. Observation meta-transform
5. Generated observation stylesheet
6. Observation XML outputs (grouped)

Keep validation and extraction from one rule base.

## Delivery Phases

## Phase 0 - Baseline and Contracts

### Build Tasks

- Stabilize `obs` annotation contract in docs:
  - `obs:emit`, `obs:type`, `obs:group`, `obs:copy`, `obs:context`
- Define extraction output contract:
  - root model (`obs:Observation`)
  - required provenance fields
- Freeze namespace constants:
  - canonical namespace
  - observation namespace

### Exit Criteria

- Contract doc approved
- Canonical and observation namespace constants referenced in code and docs

### Tests

- Documentation consistency check in review
- Existing plugin tests pass unchanged

## Phase 1 - Bootstrap Rule Generation

### Build Tasks

- Extend/finish `SchematronBootstrapTask` behavior:
  - Generate comprehensive bootstrap rules from XSD
  - Rules pass by default (observation-safe)
  - Never overwrite existing `.sch`; lifecycle warning only
- Ensure dual source support:
  - `schemaFile`
  - `schemaUrl`
- Keep deterministic rule order and reproducible output

### Exit Criteria

- Bootstrap from local XSD works
- Bootstrap from URL works
- Existing `.sch` is preserved

### Tests

- Unit:
  - XSD parsing edge cases
  - rule ordering determinism
- Integration (TestKit):
  - bootstrap from `schemaUrl`
  - bootstrap from `schemaFile`
  - no-overwrite warning behavior
- Regression:
  - existing `SchematronTask` tests stay green

## Phase 2 - Observation Compiler Layer

### Build Tasks

- Add compiler stage converting annotation-bearing Schematron to extraction stylesheet:
  - collect `obs:*` metadata from `sch:report` / `sch:assert`
  - generate extraction templates
  - support grouped outputs (`knowledge`, `terminology`, `architecture`)
- Keep validator output path available (SVRL) alongside extraction path

### Exit Criteria

- One rule base can produce both:
  - SVRL validation output
  - observation XML outputs

### Tests

- Unit:
  - metadata extraction from Schematron AST/DOM
  - XPath expression plumbing for `obs:copy` and `obs:context`
- Integration:
  - compile + run extraction stylesheet
  - grouped output file generation
- Golden-file tests:
  - deterministic observation XML for fixed inputs

## Phase 3 - Observation Runtime Task

### Build Tasks

- Introduce dedicated extraction task (recommended name: `SchematronExtractTask`):
  - source XML file-tree support
  - schema/transpiler configuration parity with `SchematronTask`
  - grouped outputs directory mapping
  - fail policy (`failOnError`) and reporting options
- Support profiles via Schematron phases:
  - terminology
  - knowledge
  - architecture

### Exit Criteria

- Task executes end-to-end extraction on canonical corpus
- Phase/profile switching verified

### Tests

- Integration (TestKit):
  - single-file extraction
  - file-tree extraction
  - profile/phase selection
  - grouped output routing
- Configuration cache compatibility
- Parallel worker behavior where applicable

## Phase 4 - Cross-Plugin Integration (Reversed Dependency)

### Build Tasks

- Keep integration sample owned by `gradle-ooxml-plugin`:
  - bootstrap from `ooxml.canonicalSchemaUrl`
  - local copy + curated XSD path
  - validation/extraction execution via XML plugin
- Keep `gradle-xml-plugin` independent and self-contained

### Exit Criteria

- OOXML functional tests validate XML plugin interop
- XML plugin has no compile/test/sample dependency on OOXML plugin

### Tests

- `gradle-ooxml-plugin` functional test with included build for XML plugin
- sample `verifySample` pass in OOXML module

## Test Matrix

## Functional Coverage

- Minimal canonical document
- Dense tables and cells
- Diagram-heavy content (shape/connector)
- Reference/link-heavy content
- Multi-file corpus runs

## Quality Gates

- Deterministic output (stable ordering)
- Provenance completeness
- No-overwrite safety for bootstrap files
- Backward compatibility for existing `SchematronTask` and `XsdTask`

## CI Gates

Run on each PR:

1. `gradle-xml-plugin`:
   - targeted extraction/bootstrap tests
   - full `test`
2. `gradle-ooxml-plugin`:
   - interop functional tests
   - sample verification

## Suggested Commands

```bash
cd "/Users/cs79en/Developer/GitHub/gradle/gradle-xml-plugin"
./gradlew test --tests "*Schematron*" --no-daemon
```

```bash
cd "/Users/cs79en/Developer/GitHub/gradle/gradle-ooxml-plugin"
./gradlew test --tests "*OoXmlPluginFunctionalTest*" --no-daemon
```

```bash
cd "/Users/cs79en/Developer/GitHub/gradle/gradle-ooxml-plugin"
./gradlew -p samples/schematron-bootstrap-xml verifySample --no-daemon
```

## Non-Functional Checks

- Performance:
  - measure extraction throughput by corpus size
  - watch memory under grouped output fan-out
- Security:
  - XML parser hardening (DOCTYPE disabled)
  - controlled URL loading in bootstrap
- Maintainability:
  - Javadoc + README parity for new tasks/properties

## Risks and Mitigations

- Rule complexity creep
  - Mitigation: keep declarative-only authoring policy for Schematron rules
- Divergence between schema and rule base
  - Mitigation: bootstrap regenerate + human review workflow
- Output instability across runs
  - Mitigation: deterministic traversal/sorting and golden tests

## Definition of Done

Done when all are true:

- Bootstrap, compile, and extraction pipeline implemented per phases 1-3
- Cross-plugin integration verified from OOXML side only
- XML plugin remains dependency-isolated
- CI matrix green with deterministic output assertions
- Docs updated with task usage, profile strategy, and safety semantics

