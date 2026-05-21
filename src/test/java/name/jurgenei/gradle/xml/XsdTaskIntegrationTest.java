package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration tests for {@link XsdTask}.
 */
public class XsdTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    /**
     * Verifies XSD validation emits SVRL and JUnit reports in AUTO engine mode.
     */
    @Test
    public void emitsSvrlAndJunitForInvalidInput() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xsd-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXsd', name.jurgenei.gradle.xml.XsdTask) {
              schema 'src/main/xsd/schema.xsd'
              source 'src/main/xml/invalid.xml'
              outputDir.set(layout.buildDirectory.dir('out/xsd'))
              reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
              failOnError.set(false)
              engine.set(name.jurgenei.gradle.xml.validation.XsdEngine.AUTO)
            }
            """);

        write("src/main/xsd/schema.xsd", """
            <?xml version='1.0' encoding='UTF-8'?>
            <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
              <xs:element name='root'>
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name='value' type='xs:string'/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """);
        write("src/main/xml/invalid.xml", """
            <root><wrong>bad</wrong></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXsd")
            .build();

        File svrl = new File(testProjectDir.getRoot(), "build/out/xsd/src/main/xml/invalid.svrl.xml");
        File junit = new File(testProjectDir.getRoot(), "build/reports/xml-validation/junit/src/main/xml/invalid.junit.xml");

        assertTrue(svrl.exists());
        assertTrue(junit.exists());
        assertTrue(read(svrl).contains("failed-assert"));
        assertTrue(read(junit).contains("<failure"));
    }

    /**
     * Verifies valid input creates no failed assertions.
     */
    @Test
    public void validatesValidInputWithoutFailedAssert() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xsd-valid-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXsd', name.jurgenei.gradle.xml.XsdTask) {
              schema 'src/main/xsd/schema.xsd'
              source 'src/main/xml/valid.xml'
              outputDir.set(layout.buildDirectory.dir('out/xsd'))
              reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL)
              failOnError.set(true)
              engine.set(name.jurgenei.gradle.xml.validation.XsdEngine.JAXP)
            }
            """);

        write("src/main/xsd/schema.xsd", """
            <?xml version='1.0' encoding='UTF-8'?>
            <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
              <xs:element name='root'>
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name='value' type='xs:string'/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """);
        write("src/main/xml/valid.xml", """
            <root><value>ok</value></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXsd")
            .build();

        File svrl = new File(testProjectDir.getRoot(), "build/out/xsd/src/main/xml/valid.svrl.xml");
        assertTrue(svrl.exists());
        assertTrue(!read(svrl).contains("failed-assert"));
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

