# Compiler Discussion Wrap-up

## Core Conclusions

- SEL should remain a derivation language, not an XSLT replacement.
- Complex logic belongs in imported function libraries implemented in XSLT.
- SchXslt should act as the host compiler.
- A generated SchXslt+SEL transpiler is preferable to maintaining a long-lived fork.
- XIR is most valuable as a structural intermediate representation, not merely as alternative XML syntax.
- SEL constructs should compile into a small set of concrete XSLT mechanisms.

## SEL Design Principles

### Keep SEL Small

Candidate core constructs:

- sel:uses
- sel:derive
- sel:link
- sel:preset
- sel:call
- sel:when

New capabilities should preferably be added through libraries rather than new language syntax.

### Avoid Becoming XSLT

SEL should describe:

- what is derived
- what is linked
- what is emitted

SEL should avoid becoming responsible for:

- iteration
- traversal algorithms
- low-level control flow
- execution mechanics

Those concerns belong in XSLT functions and runtime libraries.

## Module System

Preferred form:

```xml
<sel:uses module="oracle-lineage" as="ora"/>
```

Usage:

```xml
ora:resolve-source-columns(.)
ora:resolve-table(.)
```

Benefits:

- namespace isolation
- no function collisions
- compile-time validation
- implementation hiding

## Compiler Architecture

Desired build flow:

```text
SchXslt2
    +
SEL extension model
    ↓
Generated SchXslt2+SEL transpiler
    ↓
Compile SEL-enhanced Schematron
    ↓
XSLT 3
```

The combined transpiler should be generated during the build rather than maintained as a fork.

## XIR Positioning

XIR should not be justified merely as shorter XML syntax.

More valuable role:

```text
Source Models
      ↓
     XIR
      ↓
     SEL
      ↓
Derived Models
```

XIR serves as a structural representation allowing analysis, derivation, visualization, documentation and transformation using a common model.

## Communication Insight

The strongest value proposition is not XML or XSLT itself.

The real advantages are:

- explicit structure
- declarative intent
- self-describing models
- explainable derivations
- predictable transformations

These properties benefit both humans and AI systems because intent becomes part of the representation rather than being hidden in execution flow.

## Final Observation

Being right and getting it right are different.

A technically correct architecture still needs abstractions, naming and presentation that help people understand and adopt it. SEL and XIR can be viewed as communication abstractions as much as technical abstractions.
