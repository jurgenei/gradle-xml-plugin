<?xml version='1.0'?>
<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test='exists(doc("../../../build/out/xquery/input.xml")/result[. = "Hello Gradle"])'
      error-code='XTMM9101'>Missing or invalid build/out/xquery/input.xml output.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/out/xquery-xir/input.xir"), "(result")
      and contains(unparsed-text("../../../build/out/xquery-xir/input.xir"), "Hello ")
      and contains(unparsed-text("../../../build/out/xquery-xir/input.xir"), "Gradle")'
      error-code='XTMM9102'>Missing or invalid build/out/xquery-xir/input.xir output.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/out/xquery-identity/identity-input.xir"), "http://www.w3.org/1998/Math/MathML")
      and contains(unparsed-text("../../../build/out/xquery-identity/identity-input.xir"), "this is a comment")
      and contains(unparsed-text("../../../build/out/xquery-identity/identity-input.xir"), "xml-stylesheet")
      and contains(unparsed-text("../../../build/out/xquery-identity/identity-input.xir"), "style.xsl")'
      error-code='XTMM9103'>Missing or invalid build/out/xquery-identity/identity-input.xir output.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
