<xsl:stylesheet version='1.0'
    xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
    xmlns:axsl='http://www.w3.org/1999/XSL/TransformAlias'
    xmlns:svrl='http://purl.oclc.org/dsdl/svrl'>
  <xsl:output method='xml' indent='yes'/>
  <xsl:namespace-alias stylesheet-prefix='axsl' result-prefix='xsl'/>

  <xsl:template match='/'>
    <axsl:stylesheet version='1.0' xmlns:svrl='http://purl.oclc.org/dsdl/svrl'>
      <axsl:output method='xml' indent='yes'/>
      <axsl:template match='/'>
        <svrl:schematron-output>
          <axsl:if test="not(/root/value='OK')">
            <svrl:failed-assert test="/root/value='OK'" location='/root/value'>
              <svrl:text>Value must be OK</svrl:text>
            </svrl:failed-assert>
          </axsl:if>
        </svrl:schematron-output>
      </axsl:template>
    </axsl:stylesheet>
  </xsl:template>
</xsl:stylesheet>

