package name.jurgenei.gradle.xml;

import org.gradle.testkit.runner.BuildResult;
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
 * Integration tests for {@link SchematronBootstrapTask}.
 */
public class SchematronBootstrapTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void bootstrapsFromOoXmlSchemaUrlAndValidatesCanonicalXml() throws Exception {
        File xmlPluginDir = new File(System.getProperty("user.dir"));
        File ooxmlPluginDir = new File(xmlPluginDir.getParentFile(), "gradle-ooxml-plugin");

        write("settings.gradle", """
            pluginManagement {
              includeBuild('%s')
              includeBuild('%s')
            }
            rootProject.name = 'schematron-bootstrap-flow'
            """.formatted(gradlePath(xmlPluginDir), gradlePath(ooxmlPluginDir)));

        write("build.gradle", """
            plugins {
              id 'name.jurgenei.gradle.ooxml'
              id 'name.jurgenei.gradle.xml'
            }

            tasks.register('bootstrapCanonicalSchematron', name.jurgenei.gradle.xml.SchematronBootstrapTask) {
              def ooxmlExt = project.extensions.getByType(name.jurgenei.gradle.ooxml.OoXmlExtension)
              schemaUrl(ooxmlExt.canonicalSchemaUrl.get())
              output 'src/main/schematron/canonical-observation.sch'
            }

            tasks.register('copyCanonicalXsd') {
              doLast {
                def ooxmlExt = project.extensions.getByType(name.jurgenei.gradle.ooxml.OoXmlExtension)
                def target = file('src/main/xsd/canonical.local.xsd')
                if (!target.exists()) {
                  target.parentFile.mkdirs()
                  target.text = new URL(ooxmlExt.canonicalSchemaUrl.get()).getText('UTF-8')
                }
              }
            }

            tasks.register('bootstrapFromLocalXsd', name.jurgenei.gradle.xml.SchematronBootstrapTask) {
              dependsOn tasks.named('copyCanonicalXsd')
              schemaFile.set(layout.projectDirectory.file('src/main/xsd/canonical.local.xsd'))
              output 'src/main/schematron/canonical-local.sch'
            }

            tasks.register('validateCanonicalSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              dependsOn tasks.named('bootstrapCanonicalSchematron')
              schema.set(layout.projectDirectory.file('src/main/schematron/canonical-observation.sch'))
              source 'src/main/xml/canonical.xml'
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
              failOnError.set(true)
            }
            """);

        write("src/main/xml/canonical.xml", """
            <c:Document xmlns:c='http://jurgenei.name/canonical'>
              <c:Metadata>
                <c:DocumentId>sample</c:DocumentId>
                <c:Version>1</c:Version>
                <c:SourceFile>sample.docx</c:SourceFile>
                <c:DocumentType>DOCX</c:DocumentType>
              </c:Metadata>
              <c:Body>
                <c:Paragraph source-document='sample.docx' source-path='/word/document/p[1]'>
                  <c:Text>Hello</c:Text>
                </c:Paragraph>
              </c:Body>
            </c:Document>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("bootstrapCanonicalSchematron", "bootstrapFromLocalXsd", "validateCanonicalSchematron")
            .withPluginClasspath()
            .build();

        File generatedSch = new File(testProjectDir.getRoot(), "src/main/schematron/canonical-observation.sch");
        File localSch = new File(testProjectDir.getRoot(), "src/main/schematron/canonical-local.sch");
        File copiedXsd = new File(testProjectDir.getRoot(), "src/main/xsd/canonical.local.xsd");
        File svrl = new File(testProjectDir.getRoot(), "build/out/schematron/canonical.svrl.xml");

        assertTrue(generatedSch.exists());
        assertTrue(localSch.exists());
        assertTrue(copiedXsd.exists());
        assertTrue(svrl.exists());
        assertTrue(read(generatedSch).contains("Bootstrap observation Schematron"));
        assertTrue(!read(svrl).contains("failed-assert"));
    }

    @Test
    public void doesNotOverwriteExistingSchematronAndLogsWarning() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-bootstrap-safe'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('bootstrapSchematron', name.jurgenei.gradle.xml.SchematronBootstrapTask) {
              schema 'src/main/xsd/schema.xsd'
              output 'src/main/schematron/rules.sch'
            }
            """);

        write("src/main/xsd/schema.xsd", """
            <?xml version='1.0' encoding='UTF-8'?>
            <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' targetNamespace='http://example.com/sample' elementFormDefault='qualified'>
              <xs:element name='Root' type='xs:string'/>
            </xs:schema>
            """);
        write("src/main/schematron/rules.sch", "<sentinel/>\n");

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("bootstrapSchematron")
            .withPluginClasspath()
            .build();

        File output = new File(testProjectDir.getRoot(), "src/main/schematron/rules.sch");
        assertTrue(read(output).contains("<sentinel/>"));
        assertTrue(result.getOutput().contains("Schematron bootstrap skipped"));
    }

    private static String gradlePath(File file) {
        return file.getAbsolutePath().replace('\\', '/');
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

