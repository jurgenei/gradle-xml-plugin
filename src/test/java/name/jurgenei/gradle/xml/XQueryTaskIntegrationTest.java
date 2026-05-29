package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration tests for {@link XQueryTask} executed through Gradle TestKit.
 */
public class XQueryTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    /**
     * Verifies external variable passing for a single source file transformation.
     */
    @Test
    public void transformsSingleFileWithParameters() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              param 'prefix', 'Hello '
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xquery/main.xq", """
            declare variable $prefix external;
            <result>{ $prefix }{ /root/value/text() }</result>
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);

        File output = new File(testProjectDir.getRoot(), "build/out/xquery/input.xml");
        assertTrue(output.exists());
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));
    }

    /**
     * Verifies include/exclude filtering and multi-worker execution on a file tree.
     */
    @Test
    public void transformsFileTreeWithPatternsAndWorkers() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-tree-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source(fileTree('src/main/xml') {
                include '**/*.xml'
                exclude '**/skip*.xml'
              })
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.out.xml')
              workers.set(4)
            }
            """);

        write("src/main/xml/a.xml", """
            <root><value>A</value></root>
            """);
        write("src/main/xml/foo/b.xml", """
            <root><value>B</value></root>
            """);
        write("src/main/xml/skip.xml", """
            <root><value>SKIP</value></root>
            """);
        write("src/main/xquery/main.xq", """
            <result>{ /root/value/text() }</result>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build();

        File outputA = new File(testProjectDir.getRoot(), "build/out/xquery/a.out.xml");
        File outputB = new File(testProjectDir.getRoot(), "build/out/xquery/foo/b.out.xml");
        File skipped = new File(testProjectDir.getRoot(), "build/out/xquery/skip.out.xml");

        assertTrue(outputA.exists());
        assertTrue(outputB.exists());
        assertTrue(!skipped.exists());
        assertTrue(read(outputA).contains("<result>A</result>"));
        assertTrue(read(outputB).contains("<result>B</result>"));
    }

    /**
     * Verifies per-file timestamp checks skip transformation and emit lifecycle logs.
     */
    @Test
    public void skipsTransformationWhenOutputIsNewerThanSourceAndQuery() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-skip-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xquery/main.xq", """
            <result>{ /root/value/text() }</result>
            """);

        BuildResult firstRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery", "--rerun-tasks")
            .build();

        File output = new File(testProjectDir.getRoot(), "build/out/xquery/input.xml");
        assertTrue(output.exists());
        assertTrue(firstRun.getOutput().contains("+ PROCESSED ->"));

        long futureTimestamp = System.currentTimeMillis() + 60_000;
        assertTrue(output.setLastModified(futureTimestamp));

        BuildResult secondRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery", "--rerun-tasks")
            .build();

        assertTrue(secondRun.getOutput().contains("+ SKIP"));
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

