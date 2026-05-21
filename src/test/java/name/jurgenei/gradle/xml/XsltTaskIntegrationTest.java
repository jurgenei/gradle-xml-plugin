package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertEquals;
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
 * Integration tests for {@link XsltTask} executed through Gradle TestKit.
 */
public class XsltTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    /**
     * Verifies parameter passing for a single source file transformation.
     */
    @Test
    public void transformsSingleFileWithParameters() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              param 'prefix', 'Hello '
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:param name='prefix'/>
              <xsl:template match='/'>
                <result><xsl:value-of select='$prefix'/><xsl:value-of select='/root/value'/></result>
              </xsl:template>
            </xsl:stylesheet>
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt")
            .build()
            .task(":runXslt")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/src/main/xml/input.xml");
        assertTrue(output.exists());
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));
    }

    /**
     * Verifies include/exclude filtering and multi-worker execution on a file tree.
     */
    @Test
    public void transformsFileTreeWithPatternsAndWorkers() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-tree-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source(fileTree('src/main/xml') {
                include '**/*.xml'
                exclude '**/skip*.xml'
              })
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.out.xml')
              workers.set(4)
            }
            """);

        write("src/main/xml/a.xml", """
            <root><value>A</value></root>
            """);
        write("src/main/xml/b.xml", """
            <root><value>B</value></root>
            """);
        write("src/main/xml/skip.xml", """
            <root><value>SKIP</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:template match='/'>
                <result><xsl:value-of select='/root/value'/></result>
              </xsl:template>
            </xsl:stylesheet>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt")
            .build();

        File outputA = new File(testProjectDir.getRoot(), "build/out/xslt/src/main/xml/a.out.xml");
        File outputB = new File(testProjectDir.getRoot(), "build/out/xslt/src/main/xml/b.out.xml");
        File skipped = new File(testProjectDir.getRoot(), "build/out/xslt/src/main/xml/skip.out.xml");

        assertTrue(outputA.exists());
        assertTrue(outputB.exists());
        assertTrue(!skipped.exists());
        assertTrue(read(outputA).contains("<result>A</result>"));
        assertTrue(read(outputB).contains("<result>B</result>"));
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

