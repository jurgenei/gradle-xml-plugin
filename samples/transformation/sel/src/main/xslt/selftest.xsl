<?xml version='1.0'?>
<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
  <xsl:template match='/'>
    <xsl:assert test="doc-available('../../../build/generated/sel/sel.xsl')"
      error-code='XTMM9401'>Missing compiled SEL stylesheet.</xsl:assert>
    <xsl:assert test="doc-available('../../../build/generated/sel/sel-preset.xsl')"
      error-code='XTMM9402'>Missing compiled SEL preset stylesheet.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel/canonical-order/sel/knowledge.xml'), 'type=&quot;paragraph&quot;')
      and contains(unparsed-text('../../../build/out/sel/canonical-order/sel/knowledge.xml'), 'Order Summary')"
      error-code='XTMM9403'>Missing expected knowledge SEL XML output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel/canonical-integration/sel/architecture.xml'), 'type=&quot;relationship-candidate&quot;')
      and contains(unparsed-text('../../../build/out/sel/canonical-integration/sel/architecture.xml'), 'source=&quot;sap&quot;')
      and contains(unparsed-text('../../../build/out/sel/canonical-integration/sel/architecture.xml'), 'target=&quot;crm&quot;')"
      error-code='XTMM9404'>Missing expected architecture SEL XML output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel/canonical-glossary/sel/terminology.xml'), 'type=&quot;term&quot;')
      and contains(unparsed-text('../../../build/out/sel/canonical-glossary/sel/terminology.xml'), 'Master Data')"
      error-code='XTMM9405'>Missing expected terminology SEL XML output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel-xir/canonical-order/sel/knowledge.xir'), 'sel:Observation')
      and contains(unparsed-text('../../../build/out/sel-xir/canonical-order/sel/knowledge.xir'), 'Order Summary')
      and contains(unparsed-text('../../../build/out/sel-xir/canonical-order/sel/knowledge.xir'), 'xmlns:sel &quot;http://jurgenei.name/sel&quot;')"
      error-code='XTMM9406'>Missing expected SEL XIR output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel-preset/canonical-order/sel/knowledge.xml'), 'group=&quot;knowledge&quot;')
      and contains(unparsed-text('../../../build/out/sel-preset/canonical-order/sel/knowledge.xml'), 'SAP sends order status updates to CRM.')"
      error-code='XTMM9407'>Missing expected preset knowledge SEL output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel-preset/canonical-integration/sel/architecture.xml'), 'group=&quot;architecture&quot;')
      and contains(unparsed-text('../../../build/out/sel-preset/canonical-integration/sel/architecture.xml'), 'Core Integration Landscape')"
      error-code='XTMM9408'>Missing expected preset architecture SEL output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel-preset/canonical-glossary/sel/terminology.xml'), 'group=&quot;terminology&quot;')
      and contains(unparsed-text('../../../build/out/sel-preset/canonical-glossary/sel/terminology.xml'), 'Business Terms')"
      error-code='XTMM9409'>Missing expected preset terminology SEL output.</xsl:assert>
    <xsl:assert test="contains(unparsed-text('../../../build/out/sel-terminology/canonical-glossary/sel/terminology.xml'), 'Master Data')"
      error-code='XTMM9410'>Missing expected phase-specific terminology extraction output.</xsl:assert>
    <ok>true</ok>
  </xsl:template>
</xsl:stylesheet>
