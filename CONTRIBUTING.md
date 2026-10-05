# Contributing

Thanks for contributing to this project.

## Development setup

Requirements:

- Java 21+
- Gradle wrapper (`./gradlew`)

Clone and run tests:

```bash
git clone https://github.com/jurgenei/GradleXmlTransform.git
cd GradleXmlTransform
./gradlew test
```

## Project structure

- `src/main/java` - plugin/task implementation
- `src/test/java` - Gradle TestKit integration tests
- `samples/` - minimal runnable sample projects

## Coding guidelines

- Keep task APIs orthogonal across XML features.
- Prefer Gradle-native DSL ergonomics (`source`, `fileTree`, properties).
- Add Javadoc for public classes and APIs.
- Keep files ASCII unless an existing file already requires Unicode.
- Use Java text blocks for multiline literals when readability improves.

## Testing expectations

Run before opening a PR:

```bash
./gradlew test
./gradlew build
./gradlew -p samples/transformation/xslt runSelfTest
./gradlew -p samples/transformation/xquery runSelfTest
./gradlew -p samples/validation/schematron runSelfTest
./gradlew -p samples/validation/xsd runSelfTest
./gradlew -p samples/transformation/sel runSelfTest
./gradlew -p samples/schematron-bootstrap-ooxml runSelfTest
```

When changing task behavior, add or update integration tests in `src/test/java`.
When changing sample behavior for XSLT/XQuery/Schematron modules, update corresponding `.xspec`
self-tests first, then adjust TestKit coverage as needed.

## Commit and pull request guidance

- Keep commits focused and descriptive.
- Include rationale in PR description, not just what changed.
- Mention behavior changes and migration notes if relevant.
- Add sample updates when introducing new user-facing DSL.

## Reporting issues

Please include:

- Plugin version
- Gradle version
- Java runtime version
- Minimal reproducible build script and input files
- Full stacktrace (`--stacktrace`) when available

## Release notes

Use semantic versioning:

- Patch: bug fixes
- Minor: backward-compatible features
- Major: breaking changes
