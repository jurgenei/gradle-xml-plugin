<?xml version='1.0'?>
<xsl:stylesheet version='3.0'
  xmlns:svrl='http://purl.oclc.org/dsdl/svrl'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test='doc-available("../../../build/generated/schematron/rules.compiled.xsl")'
      error-code='XTMM9201'>Missing compiled Schematron style.</xsl:assert>
    <xsl:assert test='exists(doc("../../../build/reports/schematron-xir/invalid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9202'>Expected failed-assert in XIR report.</xsl:assert>
    <xsl:assert test='exists(doc("../../../build/reports/schematron-xml/invalid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9203'>Expected failed-assert in XML invalid report.</xsl:assert>
    <xsl:assert test='empty(doc("../../../build/reports/schematron-xml/valid.svrl.xml")//svrl:failed-assert)'
      error-code='XTMM9204'>Did not expect failed-assert in XML valid report.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/reports/xml-validation/junit/invalid.junit.xml"), "&lt;failure")'
      error-code='XTMM9205'>Expected JUnit failure for invalid input.</xsl:assert>
    <xsl:assert test='contains(unparsed-text("../../../build/reports/xml-validation/junit/valid.junit.xml"), "&lt;testsuite")'
      error-code='XTMM9206'>Expected JUnit testsuite output for valid input.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
