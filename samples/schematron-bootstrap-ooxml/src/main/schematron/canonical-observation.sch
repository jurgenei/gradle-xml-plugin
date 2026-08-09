<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:c="http://jurgenei.name/canonical">
  <sch:title>Bootstrap observation Schematron</sch:title>
  <sch:ns prefix="c" uri="http://jurgenei.name/canonical"/>
  <sch:p>generated-from: jar:file:/Users/cs79en/.gradle/caches/9.5.1/transforms/88de0ea317e9da9d918d0b28dae3cd41/transformed/original/gradle-ooxml-plugin-0.1.0-SNAPSHOT.jar!/schema/canonical.xsd</sch:p>
  <sch:p>generated-at: 2026-08-09T07:06:48.747575Z</sch:p>

  <sch:pattern id="obs-Document">
    <sch:title>Document observation</sch:title>
    <sch:rule context="Document">
      <sch:assert test="true()">Bootstrap rule for Document (always passing until customized)</sch:assert>
      <sch:p>required-children: Body, Metadata</sch:p>
    </sch:rule>
  </sch:pattern>
</sch:schema>
