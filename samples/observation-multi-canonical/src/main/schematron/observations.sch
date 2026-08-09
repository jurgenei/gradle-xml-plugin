<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
            xmlns:c='http://jurgenei.name/canonical'
            xmlns:obs='http://jurgenei.name/observation'>
  <sch:pattern id='knowledge'>
    <sch:rule context='c:Paragraph'>
      <sch:report
          test='normalize-space(.)'
          obs:emit='true'
          obs:type='paragraph'
          obs:group='knowledge'
          obs:copy='.'
          obs:context='ancestor::c:Section[1]/c:Title'>
        Paragraph evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='architecture'>
    <sch:rule context='c:Connector'>
      <sch:report
          test='@source and @target'
          obs:emit='true'
          obs:type='relationship-candidate'
          obs:group='architecture'
          obs:copy='.'
          obs:context='ancestor::c:Diagram[1]/c:Title'>
        Connector evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='terminology'>
    <sch:rule context='c:Term'>
      <sch:report
          test='normalize-space(@name)'
          obs:emit='true'
          obs:type='term'
          obs:group='terminology'
          obs:copy='.'
          obs:context='ancestor::c:Glossary[1]/c:Title'>
        Term evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>

