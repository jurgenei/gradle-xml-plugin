<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
            xmlns:c='http://jurgenei.name/canonical'
            xmlns:sel='http://jurgenei.name/sel'
            defaultPhase='knowledge-phase'>
  <sel:presets>
    <sel:preset id='copy-current' copy='.'/>
  </sel:presets>
  <sch:phase id='knowledge-phase'>
    <sch:active pattern='knowledge'/>
  </sch:phase>
  <sch:phase id='architecture-phase'>
    <sch:active pattern='architecture'/>
  </sch:phase>
  <sch:phase id='terminology-phase'>
    <sch:active pattern='terminology'/>
  </sch:phase>

  <sch:pattern id='knowledge'>
    <sch:rule context='c:Paragraph'>
      <sch:report
          test='normalize-space(.)'
          sel:preset='copy-current'
          sel:type='paragraph'
          sel:group='knowledge'
          sel:context='ancestor::c:Section[1]/c:Title'>
        Paragraph evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='architecture'>
    <sch:rule context='c:Connector'>
      <sch:report
          test='@source and @target'
          sel:preset='copy-current'
          sel:type='relationship-candidate'
          sel:group='architecture'
          sel:context='ancestor::c:Diagram[1]/c:Title'>
        Connector evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id='terminology'>
    <sch:rule context='c:Term'>
      <sch:report
          test='normalize-space(@name)'
          sel:preset='copy-current'
          sel:type='term'
          sel:group='terminology'
          sel:context='ancestor::c:Glossary[1]/c:Title'>
        Term evidence
      </sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>
