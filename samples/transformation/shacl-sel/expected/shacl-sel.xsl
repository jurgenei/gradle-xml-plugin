<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:sx="urn:name.jurgenei.gradle.xml:sel-support"
    xmlns:c="http://jurgenei.name/canonical"
    xmlns:sel="http://jurgenei.name/sel"
    exclude-result-prefixes="xs c sx" expand-text="yes">

  <xsl:output method="xml" indent="yes"/>
  <xsl:mode on-no-match="shallow-skip"/>
  <xsl:param name="source-document" as="xs:string" select="''"/>

  <xsl:function name="sx:canonical-path" as="xs:string">
    <xsl:param name="n" as="node()"/>
    <xsl:sequence select="replace(path($n), 'Q\{http://jurgenei.name/canonical\}', 'c:')"/>
  </xsl:function>

  <xsl:mode name="m-rule-0" on-no-match="shallow-skip"/>
  <xsl:mode name="m-rule-1" on-no-match="shallow-skip"/>
  <xsl:mode name="m-rule-2" on-no-match="shallow-skip"/>
  <xsl:mode name="m-rule-3" on-no-match="shallow-skip"/>
  <xsl:mode name="m-rule-4" on-no-match="shallow-skip"/>

  <xsl:param name="output-relations" as="xs:string" select="'sel/relations.xml'"/>

  <xsl:template match="/">
    <!-- Emit grouped SEL payloads into independent output files. -->
    <xsl:result-document href="{$output-relations}">
      <sel:Observations group="relations" xmlns:c="http://jurgenei.name/canonical">
        <xsl:apply-templates select="/" mode="m-rule-0"/>
        <xsl:apply-templates select="/" mode="m-rule-1"/>
        <xsl:apply-templates select="/" mode="m-rule-2"/>
        <xsl:apply-templates select="/" mode="m-rule-3"/>
        <xsl:apply-templates select="/" mode="m-rule-4"/>
      </sel:Observations>
    </xsl:result-document>
  </xsl:template>

  <xsl:template match="*[local-name()=&apos;ApplicationComponent&apos;]" mode="m-rule-0">
    <xsl:if test="(*[local-name()=&apos;hasDataSet&apos;])">
      <sel:Observation type="hasDataSet" group="relations" source="report" ruleContext="*[local-name()=&apos;ApplicationComponent&apos;]">
        <sel:Evidence>
          <xsl:copy-of select="." copy-namespaces="no"/>
        </sel:Evidence>
        <sel:Context>
          <xsl:copy-of select="*[local-name()=&apos;hasDataSet&apos;]" copy-namespaces="no"/>
        </sel:Context>
        <sel:Source document="{$source-document}" path="{sx:canonical-path(.)}"/>
      </sel:Observation>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[local-name()=&apos;DataSet&apos;]" mode="m-rule-1">
    <xsl:if test="(*[local-name()=&apos;hasTable&apos;])">
      <sel:Observation type="hasTable" group="relations" source="report" ruleContext="*[local-name()=&apos;DataSet&apos;]">
        <sel:Evidence>
          <xsl:copy-of select="." copy-namespaces="no"/>
        </sel:Evidence>
        <sel:Context>
          <xsl:copy-of select="*[local-name()=&apos;hasTable&apos;]" copy-namespaces="no"/>
        </sel:Context>
        <sel:Source document="{$source-document}" path="{sx:canonical-path(.)}"/>
      </sel:Observation>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[local-name()=&apos;DataSet&apos;]" mode="m-rule-2">
    <xsl:if test="(*[local-name()=&apos;hasDomain&apos;])">
      <sel:Observation type="hasDomain" group="relations" source="report" ruleContext="*[local-name()=&apos;DataSet&apos;]">
        <sel:Evidence>
          <xsl:copy-of select="." copy-namespaces="no"/>
        </sel:Evidence>
        <sel:Context>
          <xsl:copy-of select="*[local-name()=&apos;hasDomain&apos;]" copy-namespaces="no"/>
        </sel:Context>
        <sel:Source document="{$source-document}" path="{sx:canonical-path(.)}"/>
      </sel:Observation>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[local-name()=&apos;DataSet&apos;]" mode="m-rule-3">
    <xsl:if test="(*[local-name()=&apos;ownedBy&apos;])">
      <sel:Observation type="ownedBy" group="relations" source="report" ruleContext="*[local-name()=&apos;DataSet&apos;]">
        <sel:Evidence>
          <xsl:copy-of select="." copy-namespaces="no"/>
        </sel:Evidence>
        <sel:Context>
          <xsl:copy-of select="*[local-name()=&apos;ownedBy&apos;]" copy-namespaces="no"/>
        </sel:Context>
        <sel:Source document="{$source-document}" path="{sx:canonical-path(.)}"/>
      </sel:Observation>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[local-name()=&apos;DatabaseTable&apos;]" mode="m-rule-4">
    <xsl:if test="(*[local-name()=&apos;hasColumn&apos;])">
      <sel:Observation type="hasColumn" group="relations" source="report" ruleContext="*[local-name()=&apos;DatabaseTable&apos;]">
        <sel:Evidence>
          <xsl:copy-of select="." copy-namespaces="no"/>
        </sel:Evidence>
        <sel:Context>
          <xsl:copy-of select="*[local-name()=&apos;hasColumn&apos;]" copy-namespaces="no"/>
        </sel:Context>
        <sel:Source document="{$source-document}" path="{sx:canonical-path(.)}"/>
      </sel:Observation>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
