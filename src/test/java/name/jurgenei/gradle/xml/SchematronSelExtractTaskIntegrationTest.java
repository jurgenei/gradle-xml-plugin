/* (C)2026 */
package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration tests for {@link SchematronSelExtractTask}.
 */
public class SchematronSelExtractTaskIntegrationTest {

    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void extractsGroupedSelFilesFromAnnotatedSchematron() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-task'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xml'
              groupOutput 'architecture', 'sel/architecture.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.' sel:context='ancestor::c:Section[1]/c:Title'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
              <sch:pattern id='architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' sel:type='relationship-candidate' sel:group='architecture' sel:copy='.'>Connector evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Metadata>
                <DocumentId>sample</DocumentId>
              </Metadata>
              <Body>
                <Section>
                  <Title>Scope</Title>
                  <Paragraph>Hello</Paragraph>
                </Section>
                <Connector source='a' target='b'/>
              </Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File knowledge =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        File architecture =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/architecture.xml");

        assertTrue(knowledge.exists());
        assertTrue(architecture.exists());
        assertTrue(read(knowledge).contains("group=\"knowledge\""));
        assertTrue(read(knowledge).contains("sel:Observation"));
        assertTrue(read(knowledge).contains("sel:Evidence"));
        assertTrue(
                read(knowledge).contains("<Paragraph") || read(knowledge).contains("<c:Paragraph"));
        assertTrue(read(knowledge).contains("Hello"));
        assertTrue(read(knowledge).contains("sel:Context"));
        assertTrue(read(knowledge).contains("<Title") || read(knowledge).contains("<c:Title"));
        assertTrue(read(knowledge).contains("Scope"));
        assertTrue(read(architecture).contains("type=\"relationship-candidate\""));
        assertTrue(
                read(architecture).contains("<Connector")
                        || read(architecture).contains("<c:Connector"));
        assertTrue(read(architecture).contains("source=\"a\""));
        assertTrue(read(architecture).contains("target=\"b\""));
    }

    @Test
    public void supportsPrecompiledSelStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-precompiled'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileSel', name.jurgenei.gradle.xml.SchematronSelCompileTask) {
              schema 'src/main/schematron/sel.sch'
              output 'build/generated/sel/sel.xsl'
              groupOutput 'knowledge', 'sel/knowledge.xml'
            }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              dependsOn tasks.named('compileSel')
              schema 'src/main/schematron/sel.sch'
              style 'build/generated/sel/sel.xsl'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Metadata><DocumentId>sample</DocumentId></Metadata>
              <Body><Paragraph>Hello</Paragraph></Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File knowledge =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("sel:Observation"));
    }

    @Test
    public void resolvesXirViaDocFunctionInPrecompiledExtractionStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-doc-xir'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              style 'src/main/xslt/extract.xsl'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'default', 'sel/default.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>
            """);
        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Metadata><DocumentId>sample</DocumentId></Metadata>
            </Document>
            """);
        write("src/main/xir/lookup.xir", "(lookup (value \"doc-xir-ok\"))");
        String lookupUri =
                new File(testProjectDir.getRoot(), "src/main/xir/lookup.xir").toURI().toString();
        write("src/main/xslt/extract.xsl", extractionStyleWithLookupDoc(lookupUri));

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File output = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/default.xml");
        assertTrue(output.exists());
        assertTrue(read(output).contains("doc-xir-ok"));
    }

    @Test
    public void supportsCanonicalJsonInput() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-json'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/json/canonical.json'
              jsonMode 'canonical'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/json/canonical.json",
                """
            {
              "type": "element",
              "name": "Document",
              "attributes": {},
              "children": [
                {
                  "type": "element",
                  "name": "Paragraph",
                  "attributes": {},
                  "children": [
                    { "type": "text", "value": "Hello from json" }
                  ]
                }
              ]
            }
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File knowledge =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("sel:Observation"));
    }

    @Test
    public void supportsXirInputAndXirGroupOutputs() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-xir-to-xir'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xir/canonical.xir'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xir'
              xirFormat 'beautified'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>
                    Paragraph evidence
                  </sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xir/canonical.xir",
                """
            (c:Document {xmlns:c "http://jurgenei.name/canonical"}
              (c:Body
                (c:Paragraph "Hello from xir")))
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File knowledge =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xir");
        assertTrue(knowledge.exists());
        String content = read(knowledge);
        assertTrue(content.contains("sel:Observation"));
        assertTrue(content.contains("Hello from xir"));
        assertTrue(!content.contains("<sel:Observation"));
        assertTrue(content.contains("xmlns:sel \"http://jurgenei.name/sel\""));
        assertTrue(content.contains("xmlns:c \"http://jurgenei.name/canonical\""));
        assertTrue(content.contains("path \"/c:Document[1]/c:Body[1]/c:Paragraph[1]\""));
        assertTrue(!content.contains("Q{http://jurgenei.name/canonical}"));
        assertTrue(!content.contains("(c:Paragraph\n          {xmlns:c"));
    }

    @Test
    public void supportsSourceFilesetOverload() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-fileset'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source('src/main/xml') {
                include '**/*.xml'
              }
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body><Paragraph>Hello fileset</Paragraph></Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File knowledge =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("Hello fileset"));
    }

    @Test
    public void reportsMissingInputFiles() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-missing-input'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
            }
            """);
        write(
                "src/main/schematron/sel.sch",
                "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");

        BuildResult result =
                newGradleRunner()
                        .withProjectDir(testProjectDir.getRoot())
                        .withArguments("extractSel")
                        .withPluginClasspath()
                        .buildAndFail();

        assertTrue(result.getOutput().contains("No input files configured"));
    }

    @Test
    public void continuesWhenFailOnErrorFalseWithNativeJsonMode() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-native-json'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/json/input.json'
              jsonMode 'native'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              failOnError.set(false)
            }
            """);
        write(
                "src/main/schematron/sel.sch",
                "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");
        write("src/main/json/input.json", "{\"message\":\"native-json\"}");

        BuildResult result =
                newGradleRunner()
                        .withProjectDir(testProjectDir.getRoot())
                        .withArguments("extractSel")
                        .withPluginClasspath()
                        .build();

        assertTrue(result.getOutput().contains("SEL extraction failed but failOnError=false"));
    }

    @Test
    public void failsOnUnsupportedJsonMode() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-invalid-json-mode'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/json/input.json'
              jsonMode 'invalid'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
            }
            """);
        write(
                "src/main/schematron/sel.sch",
                "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");
        write("src/main/json/input.json", "{\"message\":\"invalid-json-mode\"}");

        BuildResult result =
                newGradleRunner()
                        .withProjectDir(testProjectDir.getRoot())
                        .withArguments("extractSel")
                        .withPluginClasspath()
                        .buildAndFail();

        assertTrue(result.getOutput().contains(":extractSel"));
    }

    @Test
    public void extractsOnlyRulesFromSelectedPhaseInOnTheFlyMode() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-phase-explicit'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              phase 'architecture-phase'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'architecture', 'sel/architecture.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:phase id='knowledge-phase'>
                <sch:active pattern='p-knowledge'/>
              </sch:phase>
              <sch:phase id='architecture-phase'>
                <sch:active pattern='p-architecture'/>
              </sch:phase>

              <sch:pattern id='p-knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>

              <sch:pattern id='p-architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' sel:type='relationship-candidate' sel:group='architecture' sel:copy='.'>Connector evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body>
                <Paragraph>Hello</Paragraph>
                <Connector source='a' target='b'/>
              </Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File architecture =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/architecture.xml");
        assertTrue(architecture.exists());
        String content = read(architecture);
        assertTrue(content.contains("type=\"relationship-candidate\""));
        assertTrue(!content.contains("type=\"paragraph\""));
    }

    @Test
    public void failsWhenExplicitPhaseIsUnknown() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-phase-unknown'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              phase 'missing-phase'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:phase id='known-phase'>
                <sch:active pattern='p-knowledge'/>
              </sch:phase>
              <sch:pattern id='p-knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);
        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body><Paragraph>Hello</Paragraph></Body>
            </Document>
            """);

        BuildResult result =
                newGradleRunner()
                        .withProjectDir(testProjectDir.getRoot())
                        .withArguments("extractSel")
                        .withPluginClasspath()
                        .buildAndFail();

        assertTrue(result.getOutput().contains(":extractSel"));
    }

    @Test
    public void supportsReusableSelPresetsAcrossMultipleRules() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-presets-extract'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'quality', 'sel/quality.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sel:presets>
                <sel:preset id='id-required' type='missing-id' group='quality' copy='.'/>
              </sel:presets>
              <sch:pattern id='identifiers'>
                <sch:rule context='person'>
                  <sch:assert test='@id' sel:preset='id-required'>Person must have id.</sch:assert>
                </sch:rule>
                <sch:rule context='employee'>
                  <sch:assert test='@id' sel:preset='id-required'>Employee must have id.</sch:assert>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <root>
              <person>Jane</person>
              <employee>John</employee>
            </root>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        File quality =
                new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/quality.xml");
        assertTrue(quality.exists());
        String content = read(quality);
        assertTrue(content.contains("group=\"quality\""));
        assertTrue(content.contains("type=\"missing-id\""));
        assertTrue(content.contains("ruleContext=\"person\""));
        assertTrue(content.contains("ruleContext=\"employee\""));
        assertTrue(content.contains("<person"));
        assertTrue(content.contains("<employee"));
    }

    @Test
    public void supportsDefaultNamespaceOutputWhenPrefixOverrideIsEmpty() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-default-namespace-output'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              outputNamespaceUri 'urn:sel:default'
              outputNamespacePrefix ''
              groupOutput 'knowledge', 'sel/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body><Paragraph>Hello default namespace</Paragraph></Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        String content =
                read(
                        new File(
                                testProjectDir.getRoot(),
                                "build/out/sel/canonical/sel/knowledge.xml"));
        assertTrue(content.contains("<Observations xmlns=\"urn:sel:default\""));
        assertTrue(content.contains("<Observation"));
        assertTrue(!content.contains("sel:Observation"));
    }

    @Test
    public void evaluatesPresetTemplateContentInMatchedRuleContext() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-preset-template-context'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              groupOutput 'knowledge', 'sel/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/schematron/sel.sch",
                """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sel:presets>
                <sel:preset id='paragraph-template' type='paragraph' group='knowledge' copy='.'>
                  <sel:template>
                    <sel:Meta code="{@code}">
                      <sel:Summary>{concat(local-name(), ':', normalize-space(.))}</sel:Summary>
                      <sel:Section>{ancestor::c:Section[1]/c:Title}</sel:Section>
                    </sel:Meta>
                  </sel:template>
                </sel:preset>
              </sel:presets>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:preset='paragraph-template'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write(
                "src/main/xml/canonical.xml",
                """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body>
                <Section>
                  <Title>Interfaces</Title>
                  <Paragraph code='p-1'>Source payload</Paragraph>
                </Section>
              </Body>
            </Document>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractSel")
                .withPluginClasspath()
                .build();

        String content =
                read(
                        new File(
                                testProjectDir.getRoot(),
                                "build/out/sel/canonical/sel/knowledge.xml"));
        assertTrue(content.contains("<sel:Meta code=\"p-1\">"));
        assertTrue(content.contains("<sel:Summary>Paragraph:Source payload</sel:Summary>"));
        assertTrue(content.contains("<sel:Section>Interfaces</sel:Section>"));
    }

    private static String extractionStyleWithLookupDoc(String lookupUri) {
        return """
            <xsl:stylesheet version='3.0'
                xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                xmlns:sel='http://jurgenei.name/sel'>
              <xsl:param name='source-document'/>
              <xsl:param name='output-default' as='xs:string' xmlns:xs='http://www.w3.org/2001/XMLSchema'/>

              <xsl:template match='/'>
                <xsl:result-document href='{$output-default}' method='xml' indent='yes'>
                  <sel:Observations>
                    <sel:Lookup><xsl:value-of select="doc('%s')/lookup/value"/></sel:Lookup>
                  </sel:Observations>
                </xsl:result-document>
              </xsl:template>
            </xsl:stylesheet>
            """
                .formatted(lookupUri);
    }

    private void write(String relativePath, String content) throws IOException {
        File file = new File(testProjectDir.getRoot(), relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private GradleRunner newGradleRunner() {
        return TestKitCoverageSupport.newGradleRunner(testProjectDir.getRoot());
    }

    private String read(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
