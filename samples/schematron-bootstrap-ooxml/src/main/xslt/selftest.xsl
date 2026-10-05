<?xml version='1.0'?>
<xsl:stylesheet version='3.0'
  xmlns:svrl='http://purl.oclc.org/dsdl/svrl'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test='doc-available("../../../src/main/schematron/canonical-sel.sch")'
      error-code='XTMM9501'>Missing canonical-sel.sch after bootstrap.</xsl:assert>
    <xsl:assert test='doc-available("../../../src/main/schematron/canonical-local.sch")'
      error-code='XTMM9502'>Missing canonical-local.sch after local bootstrap.</xsl:assert>
    <xsl:assert test='doc-available("../../../build/reports/schematron/canonical.svrl.xml")'
      error-code='XTMM9503'>Missing canonical Schematron SVRL report.</xsl:assert>
    <xsl:assert test='exists(doc("../../../build/reports/schematron/canonical.svrl.xml")/svrl:schematron-output)'
      error-code='XTMM9504'>Invalid canonical Schematron SVRL shape.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
