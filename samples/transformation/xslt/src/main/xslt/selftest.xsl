<?xml version='1.0'?>
<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test='exists(doc("../../../build/out/xslt/input.xml")/result[. = "Hello Gradle"])'
      error-code='XTMM9001'>Missing or invalid build/out/xslt/input.xml output.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/out/xslt-xir/input.xir"), "(result")
      and contains(unparsed-text("../../../build/out/xslt-xir/input.xir"), "Hello ")
      and contains(unparsed-text("../../../build/out/xslt-xir/input.xir"), "Gradle")'
      error-code='XTMM9002'>Missing or invalid build/out/xslt-xir/input.xir output.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/out/xslt-identity/identity-input.xir"), "{id ")
      and contains(unparsed-text("../../../build/out/xslt-identity/identity-input.xir"), "(title ")
      and contains(unparsed-text("../../../build/out/xslt-identity/identity-input.xir"), "XML")'
      error-code='XTMM9003'>Missing or invalid build/out/xslt-identity/identity-input.xir output.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
