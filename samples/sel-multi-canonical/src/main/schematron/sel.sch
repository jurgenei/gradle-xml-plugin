<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
            xmlns:c='http://jurgenei.name/canonical'
            xmlns:sel='http://jurgenei.name/sel'>
  <sch:pattern id='knowledge'>
    <sch:rule context='c:Paragraph'>
      <sch:report
          test='normalize-space(.)'
          sel:emit='true'
          sel:type='paragraph'
          sel:group='knowledge'
          sel:copy='.'
          sel:context='ancestor::c:Section[1]/c:Title'>
        Paragraph evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='architecture'>
    <sch:rule context='c:Connector'>
      <sch:report
          test='@source and @target'
          sel:emit='true'
          sel:type='relationship-candidate'
          sel:group='architecture'
          sel:copy='.'
          sel:context='ancestor::c:Diagram[1]/c:Title'>
        Connector evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='terminology'>
    <sch:rule context='c:Term'>
      <sch:report
          test='normalize-space(@name)'
          sel:emit='true'
          sel:type='term'
          sel:group='terminology'
          sel:copy='.'
          sel:context='ancestor::c:Glossary[1]/c:Title'>
        Term evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>
