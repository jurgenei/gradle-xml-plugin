<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:c="http://jurgenei.name/canonical">
  <sch:title>Bootstrap observation Schematron</sch:title>
  <sch:ns prefix="c" uri="http://jurgenei.name/canonical"/>
  <sch:p>generated-from: file:/Users/cs79en/Developer/GitHub/gradle/gradle-xml-plugin/samples/schematron-bootstrap-ooxml/src/main/xsd/canonical.local.xsd</sch:p>
  <sch:p>generated-at: 2026-08-09T07:06:48.816643Z</sch:p>

  <sch:pattern id="obs-Document">
    <sch:title>Document observation</sch:title>
    <sch:rule context="Document">
      <sch:assert test="true()">Bootstrap rule for Document (always passing until customized)</sch:assert>
      <sch:p>required-children: Body, Metadata</sch:p>
    </sch:rule>
  </sch:pattern>
</sch:schema>
