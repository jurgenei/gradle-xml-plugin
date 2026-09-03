package name.jurgenei.gradle.xml;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link SchematronExtractTask}.
 */
public class SchematronExtractTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void extractsGroupedObservationFilesFromAnnotatedSchematron() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-extract-task'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('extractObs', name.jurgenei.gradle.xml.SchematronExtractTask) {
              schema 'src/main/schematron/observations.sch'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/observations'))
              groupOutput 'knowledge', 'observations/knowledge.xml'
              groupOutput 'architecture', 'observations/architecture.xml'
              failOnError.set(true)
            }
            """);

        write("src/main/schematron/observations.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:obs='http://jurgenei.name/observation'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' obs:emit='true' obs:type='paragraph' obs:group='knowledge' obs:copy='.' obs:context='ancestor::c:Section[1]/c:Title'>Paragraph evidence</sch:report>
                </sch:rule>
              </sch:pattern>
              <sch:pattern id='architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' obs:emit='true' obs:type='relationship-candidate' obs:group='architecture' obs:copy='.'>Connector evidence</sch:report>
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

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractObs")
            .withPluginClasspath()
            .build();

        File knowledge = new File(testProjectDir.getRoot(), "build/out/observations/canonical/observations/knowledge.xml");
        File architecture = new File(testProjectDir.getRoot(), "build/out/observations/canonical/observations/architecture.xml");

        assertTrue(knowledge.exists());
        assertTrue(architecture.exists());
        assertTrue(read(knowledge).contains("group=\"knowledge\""));
        assertTrue(read(knowledge).contains("obs:Observation"));
        assertTrue(read(knowledge).contains("obs:Evidence"));
        assertTrue(read(knowledge).contains("<Paragraph") || read(knowledge).contains("<c:Paragraph"));
        assertTrue(read(knowledge).contains("Hello"));
        assertTrue(read(knowledge).contains("obs:Context"));
        assertTrue(read(knowledge).contains("<Title") || read(knowledge).contains("<c:Title"));
        assertTrue(read(knowledge).contains("Scope"));
        assertTrue(read(architecture).contains("type=\"relationship-candidate\""));
        assertTrue(read(architecture).contains("<Connector") || read(architecture).contains("<c:Connector"));
        assertTrue(read(architecture).contains("source=\"a\""));
        assertTrue(read(architecture).contains("target=\"b\""));
    }

    @Test
    public void supportsPrecompiledObservationStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-extract-precompiled'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileObservation', name.jurgenei.gradle.xml.SchematronObservationCompileTask) {
              schema 'src/main/schematron/observations.sch'
              output 'build/generated/observation/observations.xsl'
              groupOutput 'knowledge', 'observations/knowledge.xml'
            }

            tasks.register('extractObs', name.jurgenei.gradle.xml.SchematronExtractTask) {
              dependsOn tasks.named('compileObservation')
              schema 'src/main/schematron/observations.sch'
              style 'build/generated/observation/observations.xsl'
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/observations'))
              groupOutput 'knowledge', 'observations/knowledge.xml'
              failOnError.set(true)
            }
            """);

        write("src/main/schematron/observations.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:obs='http://jurgenei.name/observation'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' obs:emit='true' obs:type='paragraph' obs:group='knowledge' obs:copy='.'>Paragraph evidence</sch:report>
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

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("extractObs")
            .withPluginClasspath()
            .build();

        File knowledge = new File(testProjectDir.getRoot(), "build/out/observations/canonical/observations/knowledge.xml");
        assertTrue(knowledge.exists());
        assertTrue(read(knowledge).contains("obs:Observation"));
    }

    private void write(String relativePath, String content) throws IOException {
        File file = new File(testProjectDir.getRoot(), relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private String read(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}

