<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <xsl:variable name="relations"
                  select="doc('../../../../build/out/shacl-sel/collibra-case/sel/relations.xml')"/>
    <ok value="{exists($relations//*[local-name()='Observation']) and exists($relations//*[local-name()='Evidence'])}"/>
  </xsl:template>
</xsl:stylesheet>
