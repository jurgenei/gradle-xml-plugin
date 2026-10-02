<?xml version='1.0'?>
<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:param name='prefix'/>
  <xsl:template match='/'>
    <result><xsl:value-of select='$prefix'/><xsl:value-of select='/root/value'/></result>
  </xsl:template>
</xsl:stylesheet>

