<?xml version='1.0'?>
<xsl:stylesheet version='3.0'
  xmlns:svrl='http://purl.oclc.org/dsdl/svrl'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test='exists(doc("../../../build/reports/xsd-xir/invalid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9301'>Expected failed-assert in XIR XSD report.</xsl:assert>
    <xsl:assert test='exists(doc("../../../build/reports/xsd-xml/invalid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9302'>Expected failed-assert in XML invalid XSD report.</xsl:assert>
    <xsl:assert test='empty(doc("../../../build/reports/xsd-xml/valid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9303'>Did not expect failed-assert in XML valid XSD report.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/reports/xml-validation/junit/invalid.junit.xml"), "&lt;failure")'
      error-code='XTMM9304'>Expected JUnit failure for invalid XSD sample.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/reports/xml-validation/junit/valid.junit.xml"), "&lt;testsuite")'
      error-code='XTMM9305'>Expected JUnit testsuite for valid XSD sample.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
