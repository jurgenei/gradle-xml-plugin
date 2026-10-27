# Workplan: XIR serialization for `xsl:result-document` (`method="myns:xir"`)

## Goal

Enable XSLT secondary result documents to serialize as XIR when stylesheet uses QName method, e.g.:

```xml
<xsl:result-document href="output.xir" method="xir:meth"/>
```

where `xir:meth` is bound to plugin-supported XIR method namespace.

Must work with Saxon HE and EE for same supported Saxon version line used by `gradle-xml-plugin`.

---

## Scope

In scope:

1. `XsltTask` runtime wiring for custom output method.
2. XIR serializer integration for both primary + secondary (`result-document`) outputs.
3. Integration tests for file-extension and QName-method routing.
4. README updates with usage and namespace contract.

Out of scope:

1. Changing XIR syntax/semantics.
2. New non-XIR custom methods.
3. Non-Saxon processors.

---

## Design decisions

1. **Primary trigger**: QName output method (`method="prefix:xir"`), not bare `method="xir"`.
2. **Namespace contract**: fixed URI (define constant in plugin), local-part `xir`.
3. **Fallback rule**: if method not XIR QName, keep Saxon default behavior.
4. **Secondary trigger (optional compatibility)**: treat `.xir` extension as XIR even when method omitted, only if safe with Saxon routing APIs.

---

## Implementation phases

## Phase 1 — Confirm Saxon extension hook points

1. Pin exact Saxon APIs available in current plugin dependency line.
2. Choose stable hook for custom serializer method:
   - preferred: `SerializerFactory`/`Receiver` mapping for QName method.
   - fallback: result-document interception via output URI resolver.
3. Verify chosen hook available in both HE and EE artifacts.

Deliverable:
- short internal note in code comments near integration class documenting chosen API and version assumption.

## Phase 2 — Add XIR output method registry

1. Create new integration class (e.g. `SaxonXirOutputMethods`) in `name.jurgenei.gradle.xml.saxon`.
2. Define constants:
   - `XIR_OUTPUT_METHOD_NAMESPACE_URI`
   - `XIR_OUTPUT_METHOD_LOCAL_NAME = "xir"`
3. Register custom method with Saxon configuration on `Processor` creation path used by `XsltTask`.
4. Bridge Saxon receiver events to `XirSerializer` (respect current `xirFormat` setting).

Deliverable:
- runtime can accept stylesheet `method="prefix:xir"` without Saxon “method must be …” failure.

## Phase 3 — Wire into `XsltTask` execution paths

1. Ensure processor/compiler setup in `XsltTask` applies both:
   - existing input/resource resolvers (`SaxonXirResolvers`)
   - new output-method registry.
2. Keep current behavior for non-XIR outputs unchanged.
3. Confirm explicit task-level `.xir` principal output behavior still passes.

Deliverable:
- no regression for existing xml/json/text output logic.

## Phase 4 — Add integration tests

Add tests to `XsltTaskIntegrationTest`:

1. **Secondary result with QName method**
   - stylesheet writes principal XML + secondary `output.xir` via `xsl:result-document method="xir:xir"`.
   - assert generated `.xir` content is valid expected shape.
2. **Secondary result with non-XIR method**
   - ensure plugin does not hijack normal XML/JSON methods.
3. **Namespace misuse test**
   - unknown QName method remains Saxon error (expected fail), proving strict routing.
4. **HE/EE compatibility smoke**
   - if EE not in CI matrix, at least keep test processor config free of EE-only classes.

Deliverable:
- deterministic coverage of routing behavior and backward compatibility.

## Phase 5 — Documentation

Update `README.md`:

1. Explain `xsl:result-document` + QName requirement.
2. Provide namespace URI and complete example stylesheet snippet.
3. Clarify `method="xir"` invalid in Saxon; must be prefixed QName.
4. State HE+EE support intent and Saxon version caveat.

Deliverable:
- user-facing guidance for exact syntax and expected behavior.

---

## Acceptance criteria

1. `xsl:result-document method="xir:xir"` produces XIR file content, not XML, in plugin integration test.
2. Existing tests for XML/XIR principal output continue passing.
3. No behavior change for `xml/json/text` methods.
4. README contains working example and namespace declaration.

---

## Risks and mitigations

1. **Saxon API drift across versions**
   - isolate Saxon-specific code in one integration class.
   - minimize reflective hacks unless unavoidable.
2. **Result-document stream lifecycle**
   - ensure writer flush/close handled by Saxon contract.
3. **Mixed output in one transform**
   - keep per-document serializer selection based on method QName at dispatch time.
4. **Format mismatch**
   - propagate existing `xirFormat` option consistently for secondary docs.

---

## Suggested execution order

1. Implement hook skeleton + compile.
2. Add failing integration test for `result-document` QName method.
3. Implement serializer routing until test passes.
4. Run targeted integration test set.
5. Update README.
