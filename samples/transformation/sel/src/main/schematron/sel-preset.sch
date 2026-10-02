<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
            xmlns:c='http://jurgenei.name/canonical'
            xmlns:sel='http://jurgenei.name/sel'
            defaultPhase='preset-phase'>
  <sel:presets>
    <sel:preset id='dynamic-evidence' copy='if (@name) then @name else if (@source) then @source else .'/>
    <sel:preset id='dynamic-context'
                context='(ancestor::c:Section[1]/c:Title, ancestor::c:Diagram[1]/c:Title, ancestor::c:Glossary[1]/c:Title)[1]'/>
    <sel:preset id='as-knowledge' type='paragraph' group='knowledge'/>
    <sel:preset id='as-architecture' type='relationship-candidate' group='architecture'/>
    <sel:preset id='as-terminology' type='term' group='terminology'/>
  </sel:presets>

  <sch:phase id='preset-phase'>
    <sch:active pattern='preset-rules'/>
  </sch:phase>

  <sch:pattern id='preset-rules'>
    <sch:rule context='c:Paragraph'>
      <sch:report
          test='normalize-space(.)'
          sel:preset='dynamic-evidence dynamic-context as-knowledge'>
        Paragraph evidence via reusable presets
      </sch:report>
    </sch:rule>

    <sch:rule context='c:Connector'>
      <sch:report
          test='@source and @target'
          sel:preset='dynamic-evidence dynamic-context as-architecture'
          sel:context='if (@protocol) then ../c:Title else (ancestor::c:Diagram[1]/c:Title)[1]'>
        Connector evidence via reusable presets
      </sch:report>
    </sch:rule>

    <sch:rule context='c:Term'>
      <sch:report
          test='normalize-space(@name)'
          sel:preset='dynamic-evidence dynamic-context as-terminology'>
        Term evidence via reusable presets
      </sch:report>
    </sch:rule>
  </sch:pattern>
</sch:schema>
