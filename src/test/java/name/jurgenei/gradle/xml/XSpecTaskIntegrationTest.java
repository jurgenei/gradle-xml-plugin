/* (C)2026 */
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
 * Integration tests for {@link XSpecTask}.
 */
public class XSpecTaskIntegrationTest {

    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void runsXspecFromSourceAndProducesXmlAndJunitReports() throws Exception {
        write("settings.gradle", "rootProject.name = 'xspec-run-success'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('runXSpec', name.jurgenei.gradle.xml.XSpecTask) {
              source 'src/main/xspec/sample.xspec'
              outputDir.set(layout.buildDirectory.dir('reports/xspec'))
              failOnError.set(true)
            }
            """);
        writeCommonXsltAndXspec();

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("runXSpec")
                .withPluginClasspath()
                .build();

        File xmlReport = new File(testProjectDir.getRoot(), "build/reports/xspec/sample.xspec.xml");
        File junitReport =
                new File(
                        testProjectDir.getRoot(),
                        "build/reports/xspec/junit/sample.xspec.junit.xml");

        assertTrue(xmlReport.exists());
        assertTrue(junitReport.exists());
        assertTrue(read(xmlReport).contains("http://www.jenitennison.com/xslt/xspec"));
        assertTrue(read(xmlReport).contains("successful=\"true\""));
        assertTrue(read(junitReport).contains("<testsuites"));
        assertTrue(read(junitReport).contains("<testcase"));
    }

    @Test
    public void supportsPrecompiledRunnerReuse() throws Exception {
        write("settings.gradle", "rootProject.name = 'xspec-run-precompiled'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileXSpec', name.jurgenei.gradle.xml.XSpecCompileTask) {
              input 'src/main/xspec/sample.xspec'
              output 'build/generated/xspec/sample-runner.xsl'
            }

            tasks.register('runXSpec', name.jurgenei.gradle.xml.XSpecTask) {
              dependsOn tasks.named('compileXSpec')
              input 'build/generated/xspec/sample-runner.xsl'
              output 'build/reports/xspec/sample-report.xml'
              failOnError.set(true)
            }
            """);
        writeCommonXsltAndXspec();

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("runXSpec")
                .withPluginClasspath()
                .build();

        File xmlReport =
                new File(testProjectDir.getRoot(), "build/reports/xspec/sample-report.xml");
        File junitReport =
                new File(
                        testProjectDir.getRoot(),
                        "build/reports/xspec/junit/sample-report.junit.xml");
        assertTrue(xmlReport.exists());
        assertTrue(junitReport.exists());
        assertTrue(read(xmlReport).contains("successful=\"true\""));
    }

    private void writeCommonXsltAndXspec() throws IOException {
        write(
                "src/main/xslt/main.xsl",
                """
            <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:template match="/">
                <result><xsl:value-of select="/doc/value"/></result>
              </xsl:template>
            </xsl:stylesheet>
            """);
        write(
                "src/main/xspec/sample.xspec",
                """
            <x:description xmlns:x="http://www.jenitennison.com/xslt/xspec"
                           stylesheet="../xslt/main.xsl">
              <x:scenario label="main transform">
                <x:context><doc><value>Hello</value></doc></x:context>
                <x:expect label="value rendered" test="true()"/>
              </x:scenario>
            </x:description>
            """);
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

    private GradleRunner newGradleRunner() {
        return TestKitCoverageSupport.newGradleRunner(testProjectDir.getRoot());
    }
}
