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
 * Integration tests for {@link XSpecCompileTask}.
 */
public class XSpecCompileTaskIntegrationTest {

    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void compilesXspecIntoExecutableRunner() throws Exception {
        write("settings.gradle", "rootProject.name = 'xspec-compile-success'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileXSpec', name.jurgenei.gradle.xml.XSpecCompileTask) {
              source 'src/main/xspec/sample.xspec'
              outputDir.set(layout.buildDirectory.dir('generated/xspec'))
              failOnError.set(true)
            }
            """);
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

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("compileXSpec")
                .withPluginClasspath()
                .build();

        File runner = new File(testProjectDir.getRoot(), "build/generated/xspec/sample.xspec.xsl");
        assertTrue(runner.exists());
        String compiled = read(runner);
        assertTrue(compiled.contains("xsl:stylesheet"));
        assertTrue(compiled.contains("http://www.jenitennison.com/xslt/xspec"));
    }

    @Test
    public void failsWhenExplicitInputDoesNotExist() throws Exception {
        write("settings.gradle", "rootProject.name = 'xspec-compile-missing-input'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileXSpec', name.jurgenei.gradle.xml.XSpecCompileTask) {
              input 'src/main/xspec/missing.xspec'
              output 'build/generated/xspec/missing.xspec.xsl'
            }
            """);

        BuildResult result =
                newGradleRunner()
                        .withProjectDir(testProjectDir.getRoot())
                        .withArguments("compileXSpec")
                        .withPluginClasspath()
                        .buildAndFail();

        assertTrue(result.getOutput().contains("XSpec input file does not exist"));
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
