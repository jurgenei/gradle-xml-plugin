package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
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

        File svrl = new File(testProjectDir.getRoot(), "build/out/schematron/invalid.svrl.xml");
        File junit = new File(testProjectDir.getRoot(), "build/reports/xml-validation/junit/invalid.junit.xml");

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
        write("src/main/xml/foo/b.xml", """
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

        File outputA = new File(testProjectDir.getRoot(), "build/out/schematron/a.svrl.xml");
        File outputB = new File(testProjectDir.getRoot(), "build/out/schematron/foo/b.svrl.xml");
        File skipped = new File(testProjectDir.getRoot(), "build/out/schematron/skip.svrl.xml");

        assertTrue(outputA.exists());
        assertTrue(outputB.exists());
        assertTrue(!skipped.exists());
        assertTrue(!read(outputA).contains("failed-assert"));
        assertTrue(read(outputB).contains("failed-assert"));
    }

    /**
     * Verifies compiled style reuse and recompilation triggers.
     */
    @Test
    public void reusesCompiledStyleAndRecompilesWhenTranspilerParametersChange() throws Exception {
        write("settings.gradle", """
            rootProject.name = 'schematron-style-cache-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              schema 'src/main/schematron/rules.sch'
              transpilerStylesheet 'src/main/schematron/transpile.xsl'
              style 'build/generated/schematron/compiled.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              failOnError.set(false)
            }
            """);

        write("src/main/schematron/rules.sch", """
            <schema xmlns='http://purl.oclc.org/dsdl/schematron'/>
            """);
        write("src/main/schematron/transpile.xsl", transpiler());
        write("src/main/xml/input.xml", """
            <root><value>BAD</value></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build();

        File compiledStyle = new File(testProjectDir.getRoot(), "build/generated/schematron/compiled.xsl");
        assertTrue(compiledStyle.exists());
        long firstCompiledTimestamp = compiledStyle.lastModified();

        Thread.sleep(1200L);

        TaskOutcome secondOutcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build()
            .task(":runSchematron")
            .getOutcome();

        long secondCompiledTimestamp = compiledStyle.lastModified();
        assertTrue(secondOutcome == TaskOutcome.SUCCESS || secondOutcome == TaskOutcome.UP_TO_DATE);
        assertTrue("Expected compiled style timestamp to stay unchanged when nothing changed",
            secondCompiledTimestamp == firstCompiledTimestamp);

        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              schema 'src/main/schematron/rules.sch'
              transpilerStylesheet 'src/main/schematron/transpile.xsl'
              style 'build/generated/schematron/compiled.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              phase.set('#ALL')
              failOnError.set(false)
            }
            """);

        Thread.sleep(1200L);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build();

        long thirdCompiledTimestamp = compiledStyle.lastModified();
        assertTrue("Expected compiled style timestamp to increase after transpiler parameter change",
            thirdCompiledTimestamp > secondCompiledTimestamp);
    }

    /**
     * Verifies debug mode pretty-prints SVRL output for readability.
     */
    @Test
    public void debugProducesIndentedSvrlOutput() throws Exception {
        write("settings.gradle", """
            rootProject.name = 'schematron-debug-format-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runSchematron', name.jurgenei.gradle.xml.SchematronTask) {
              schema 'src/main/schematron/rules.sch'
              style 'build/generated/schematron/compiled.xsl'
              source 'src/main/xml/invalid.xml'
              outputDir.set(layout.buildDirectory.dir('out/schematron'))
              debug.set(true)
              failOnError.set(false)
            }
            """);

        write("src/main/schematron/rules.sch", """
            <schema xmlns='http://purl.oclc.org/dsdl/schematron'>
              <pattern id='value-is-ok'>
                <rule context='/root'>
                  <assert test="value = 'OK'">Value must be OK</assert>
                </rule>
              </pattern>
            </schema>
            """);
        write("src/main/xml/invalid.xml", """
            <root><value>BAD</value></root>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runSchematron")
            .build();

        File svrl = new File(testProjectDir.getRoot(), "build/out/schematron/invalid.svrl.xml");
        String svrlXml = read(svrl);

        assertTrue(svrl.exists());
        assertTrue(svrlXml.contains("failed-assert"));
        assertTrue("Expected indented/debug-formatted SVRL with line breaks", svrlXml.contains("\n"));
        assertTrue("Expected indented/debug-formatted SVRL elements", svrlXml.contains("\n   <svrl:"));
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

