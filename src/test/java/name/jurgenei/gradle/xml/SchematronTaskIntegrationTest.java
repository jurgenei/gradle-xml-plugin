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
 * Integration tests for {@link SchematronTask}.
 */
public class SchematronTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    /**
     * Verifies Schematron transpilation and SVRL/JUnit generation.
     */
    @Test
    public void validatesWithCustomTranspilerAndGeneratesReports() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'schematron-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              schema 'src/main/schematron/rules.sch'
              transpilerStylesheet 'src/main/schematron/transpile.xsl'
              source 'src/main/xml/invalid.xml'
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              reportFormat.set(name.jurgenei.gradle.xml.validation.ReportFormat.SVRL_AND_JUNIT)
              failOnError.set(false)
            }
            """);

        write("src/main/schematron/rules.sch", """
            <schema xmlns='http://purl.oclc.org/dsdl/schematron'/>
            """);
        write("src/main/schematron/transpile.xsl", transpiler());
        write("src/main/xml/invalid.xml", """
            <root><value>BAD</value></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build();

        File svrl = new File(testProjectDir.getRoot(), "build/out/schematron/src/main/xml/invalid.svrl.xml");
        File junit = new File(testProjectDir.getRoot(), "build/reports/xml-validation/junit/src/main/xml/invalid.junit.xml");

        assertTrue(svrl.exists());
        assertTrue(junit.exists());
        assertTrue(read(svrl).contains("failed-assert"));
        assertTrue(read(junit).contains("<failure"));
    }

    /**
     * Verifies include/exclude file tree support with parallel workers.
     */
    @Test
    public void validatesFileTreeWithPatternsAndWorkers() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'schematron-tree-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              schema 'src/main/schematron/rules.sch'
              transpilerStylesheet 'src/main/schematron/transpile.xsl'
              source(fileTree('src/main/xml') {
                include '**/*.xml'
                exclude '**/skip*.xml'
              })
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              workers.set(4)
              failOnError.set(false)
            }
            """);

        write("src/main/schematron/rules.sch", """
            <schema xmlns='http://purl.oclc.org/dsdl/schematron'/>
            """);
        write("src/main/schematron/transpile.xsl", transpiler());
        write("src/main/xml/a.xml", """
            <root><value>OK</value></root>
            """);
        write("src/main/xml/b.xml", """
            <root><value>BAD</value></root>
            """);
        write("src/main/xml/skip.xml", """
            <root><value>BAD</value></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build();

        File outputA = new File(testProjectDir.getRoot(), "build/out/schematron/src/main/xml/a.svrl.xml");
        File outputB = new File(testProjectDir.getRoot(), "build/out/schematron/src/main/xml/b.svrl.xml");
        File skipped = new File(testProjectDir.getRoot(), "build/out/schematron/src/main/xml/skip.svrl.xml");

        assertTrue(outputA.exists());
        assertTrue(outputB.exists());
        assertTrue(!skipped.exists());
        assertTrue(!read(outputA).contains("failed-assert"));
        assertTrue(read(outputB).contains("failed-assert"));
    }

    private static String transpiler() {
        return """
            <xsl:stylesheet version='1.0'
                xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                xmlns:axsl='http://www.w3.org/1999/XSL/TransformAlias'
                xmlns:svrl='http://purl.oclc.org/dsdl/svrl'>
              <xsl:output method='xml' indent='yes'/>
              <xsl:namespace-alias stylesheet-prefix='axsl' result-prefix='xsl'/>

              <xsl:template match='/'>
                <axsl:stylesheet version='1.0' xmlns:svrl='http://purl.oclc.org/dsdl/svrl'>
                  <axsl:output method='xml' indent='yes'/>
                  <axsl:template match='/'>
                    <svrl:schematron-output>
                      <axsl:if test="not(/root/value='OK')">
                        <svrl:failed-assert test="/root/value='OK'" location='/root/value'>
                          <svrl:text>Value must be OK</svrl:text>
                        </svrl:failed-assert>
                      </axsl:if>
                    </svrl:schematron-output>
                  </axsl:template>
                </axsl:stylesheet>
              </xsl:template>
            </xsl:stylesheet>
            """;
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

