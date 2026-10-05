<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:sel="http://jurgenei.name/sel">
  <sch:pattern id="shacl-relations">
    <sch:rule context="*[local-name()='ApplicationComponent']">
      <sch:report test="*[local-name()='hasDataSet']" sel:type="hasDataSet" sel:group="relations" sel:copy="." sel:context="*[local-name()='hasDataSet']">SHACL relation ApplicationComponent -> DataSet</sch:report>
    </sch:rule>
    <sch:rule context="*[local-name()='DataSet']">
      <sch:report test="*[local-name()='hasTable']" sel:type="hasTable" sel:group="relations" sel:copy="." sel:context="*[local-name()='hasTable']">SHACL relation DataSet -> DatabaseTable</sch:report>
    </sch:rule>
    <sch:rule context="*[local-name()='DataSet']">
      <sch:report test="*[local-name()='hasDomain']" sel:type="hasDomain" sel:group="relations" sel:copy="." sel:context="*[local-name()='hasDomain']">SHACL relation DataSet -> DataDomain</sch:report>
    </sch:rule>
    <sch:rule context="*[local-name()='DataSet']">
      <sch:report test="*[local-name()='ownedBy']" sel:type="ownedBy" sel:group="relations" sel:copy="." sel:context="*[local-name()='ownedBy']">SHACL relation DataSet -> DataOwner</sch:report>
    </sch:rule>
    <sch:rule context="*[local-name()='DatabaseTable']">
      <sch:report test="*[local-name()='hasColumn']" sel:type="hasColumn" sel:group="relations" sel:copy="." sel:context="*[local-name()='hasColumn']">SHACL relation DatabaseTable -> DatabaseColumn</sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>
