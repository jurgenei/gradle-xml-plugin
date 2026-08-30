# xml-sexpr

Standalone S-expression XML bridge library used by `gradle-xml-plugin`.

## Package

`name.jurgenei.xml.sexpr`

## Format

- Attributes: `[id "b1" version "1.0"]`
- Namespaces:
  - default: `[ns "http://www.w3.org/1998/Math/MathML"]`
  - prefixed: `[ns "m" "http://www.w3.org/1998/Math/MathML"]`
- Comments: `(# "text")`
- Processing instructions: `(?xml-stylesheet type="text/xsl" href="style.xsl")`

Processing-instruction values must be quoted.

