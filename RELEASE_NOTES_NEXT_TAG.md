# Release Notes (Next Tag)

Date: 2026-05-29
Scope: XML transform/validation output mapping and incremental transformation performance

## Highlights

- Fixed output path mapping for file tree sources to be relative to the file tree root.
- Added timestamp-based skipping for XML transformations to avoid unnecessary work.
- Added lifecycle logging for both skip and process paths.
- Aligned validation report paths (SVRL/JUnit) with the same relative output mapping behavior.

## Changes

### Output Path Mapping

- `source(fileTree('src/main/xml') { ... })` now writes outputs under `outputDir` relative to the file tree root.
- Example: `src/main/xml/foo/bar.xml` now maps to `out/xslt/foo/bar.xml`.
- The old, less natural mapping that included `src/main/xml/...` under `outputDir` is removed.

### Incremental Transformation by Timestamp

- Transform tasks now skip processing when the target file is newer than all relevant inputs.
- Logged at lifecycle level as:
  - `<input-file> + SKIP`
  - `<input-file> + PROCESSED -> <output-file>`
- Dependency timestamp checks include:
  - XSLT: source XML + stylesheet
  - XQuery: source XML + query file

### Validation Reporting Layout

- Validation output path computation now follows file tree relative paths as well.
- Affects both SVRL output files and optional JUnit reports.

## Verification

- Integration test suite for XML plugin passed after changes:
  - `./gradlew test --tests "name.jurgenei.gradle.xml.*IntegrationTest"`

