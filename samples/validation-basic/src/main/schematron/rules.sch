<schema xmlns='http://purl.oclc.org/dsdl/schematron'>
  <pattern id='value-is-ok'>
    <rule context='/root'>
      <assert test='value = "OK"'>Value must be OK</assert>
    </rule>
  </pattern>
</schema>

