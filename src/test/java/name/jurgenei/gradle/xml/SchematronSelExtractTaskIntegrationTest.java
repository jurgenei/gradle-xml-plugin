package name.jurgenei.gradle.xml;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link SchematronSelExtractTask}.
 */
public class SchematronSelExtractTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void extractsGroupedSelFilesFromAnnotatedSchematron() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-task'\n");
        write("build.gradle", """
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

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:emit='true' sel:type='paragraph' sel:group='knowledge' sel:copy='.' sel:context='ancestor::c:Section[1]/c:Title'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
              <sch:pattern id='architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' sel:emit='true' sel:type='relationship-candidate' sel:group='architecture' sel:copy='.'>Connector evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write("src/main/xml/canonical.xml", """
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

        File knowledge = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        File architecture = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/architecture.xml");

        assertTrue(knowledge.exists());
        assertTrue(architecture.exists());
        assertTrue(read(knowledge).contains("group=\"knowledge\""));
        assertTrue(read(knowledge).contains("sel:Observation"));
        assertTrue(read(knowledge).contains("sel:Evidence"));
        assertTrue(read(knowledge).contains("<Paragraph") || read(knowledge).contains("<c:Paragraph"));
        assertTrue(read(knowledge).contains("Hello"));
        assertTrue(read(knowledge).contains("sel:Context"));
        assertTrue(read(knowledge).contains("<Title") || read(knowledge).contains("<c:Title"));
        assertTrue(read(knowledge).contains("Scope"));
        assertTrue(read(architecture).contains("type=\"relationship-candidate\""));
        assertTrue(read(architecture).contains("<Connector") || read(architecture).contains("<c:Connector"));
        assertTrue(read(architecture).contains("source=\"a\""));
        assertTrue(read(architecture).contains("target=\"b\""));
    }

    @Test
    public void supportsPrecompiledSelStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-precompiled'\n");
        write("build.gradle", """
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

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:emit='true' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write("src/main/xml/canonical.xml", """
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

        File knowledge = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("sel:Observation"));
    }

    @Test
    public void resolvesXirViaDocFunctionInPrecompiledExtractionStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-doc-xir'\n");
        write("build.gradle", """
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

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>
            """);
        write("src/main/xml/canonical.xml", """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Metadata><DocumentId>sample</DocumentId></Metadata>
            </Document>
            """);
        write("src/main/xir/lookup.xir", "(lookup (value \"doc-xir-ok\"))");
        String lookupUri = new File(testProjectDir.getRoot(), "src/main/xir/lookup.xir").toURI().toString();
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
        write("build.gradle", """
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

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='Paragraph'>
                  <sch:report test='normalize-space(.)' sel:emit='true' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write("src/main/json/canonical.json", """
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

        File knowledge = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("sel:Observation"));
    }

    @Test
    public void supportsSourceFilesetOverload() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-fileset'\n");
        write("build.gradle", """
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

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:emit='true' sel:type='paragraph' sel:group='knowledge' sel:copy='.'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        write("src/main/xml/canonical.xml", """
            <Document xmlns='http://jurgenei.name/canonical'>
              <Body><Paragraph>Hello fileset</Paragraph></Body>
            </Document>
            """);

        newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractSel")
            .withPluginClasspath()
            .build();

        File knowledge = new File(testProjectDir.getRoot(), "build/out/sel/canonical/sel/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("Hello fileset"));
    }

    @Test
    public void reportsMissingInputFiles() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-missing-input'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
            }
            """);
        write("src/main/schematron/sel.sch", "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");

        BuildResult result = newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractSel")
            .withPluginClasspath()
            .buildAndFail();

        assertTrue(result.getOutput().contains("No input files configured"));
    }

    @Test
    public void continuesWhenFailOnErrorFalseWithNativeJsonMode() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-native-json'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/json/input.json'
              jsonMode 'native'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
              failOnError.set(false)
            }
            """);
        write("src/main/schematron/sel.sch", "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");
        write("src/main/json/input.json", "{\"message\":\"native-json\"}");

        BuildResult result = newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractSel")
            .withPluginClasspath()
            .build();

        assertTrue(result.getOutput().contains("SEL extraction failed but failOnError=false"));
    }

    @Test
    public void failsOnUnsupportedJsonMode() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-extract-invalid-json-mode'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractSel', name.jurgenei.gradle.xml.SchematronSelExtractTask) {
              schema 'src/main/schematron/sel.sch'
              source 'src/main/json/input.json'
              jsonMode 'invalid'
              outputDir.set(layout.buildDirectory.dir('out/sel'))
            }
            """);
        write("src/main/schematron/sel.sch", "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'/>");
        write("src/main/json/input.json", "{\"message\":\"invalid-json-mode\"}");

        BuildResult result = newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractSel")
            .withPluginClasspath()
            .buildAndFail();

        assertTrue(result.getOutput().contains("Unsupported jsonMode 'invalid'"));
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
            """.formatted(lookupUri);
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
