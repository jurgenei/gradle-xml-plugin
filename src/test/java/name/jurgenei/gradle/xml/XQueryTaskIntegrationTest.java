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
     * Verifies serializer method is inferred as json from output extension.
     */
    @Test
    public void infersJsonOutputMethodFromExtension() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-json-method-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.json')
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xquery/main.xq", """
            map { 'value': data(/root/value) }
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);

        File output = new File(testProjectDir.getRoot(), "build/out/xquery/input.json");
        assertTrue(output.exists());
        assertEquals("{\"value\":\"Gradle\"}", read(output).replaceAll("\\s+", ""));
    }

    @Test
    public void autoModeWritesHierarchicalJsonForXmlNodeResults() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-json-auto-hierarchical-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.json')
              jsonMode.set('auto')
            }
            """);

        write("src/main/xml/input.xml", "<book><title>XML</title></book>");
        write("src/main/xquery/main.xq", ".");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.json"));
        assertTrue(output.contains("\"type\":\"element\"") || output.contains("\"type\" : \"element\""));
        assertTrue(output.contains("\"name\":\"book\"") || output.contains("\"name\" : \"book\""));
        assertTrue(!output.contains("\"attributes\" : { }"));
        assertTrue(!output.contains("\"attributes\":{}"));
    }

    @Test
    public void nativeModeWritesHierarchicalJsonForXmlNodeResults() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-json-native-hierarchical-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.json')
              jsonMode.set('native')
            }
            """);

        write("src/main/xml/input.xml", "<book><title>XML</title></book>");
        write("src/main/xquery/main.xq", ".");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.json"));
        assertTrue(output.contains("\"type\":\"element\"") || output.contains("\"type\" : \"element\""));
        assertTrue(output.contains("\"name\":\"book\"") || output.contains("\"name\" : \"book\""));
        assertTrue(!output.contains("\"attributes\" : { }"));
        assertTrue(!output.contains("\"attributes\":{}"));
    }

    /**
     * Verifies explicit outputMethod overrides extension-based inference.
     */
    @Test
    public void usesExplicitTextOutputMethod() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-text-method-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.xml')
              outputMethod.set('text')
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xquery/main.xq", """
            concat('VALUE=', data(/root/value))
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
        assertEquals("VALUE=Gradle", read(output).trim());
    }

    /**
     * Verifies explicit single-file mode using input/output properties.
     */
    @Test
    public void transformsSingleFileWithExplicitInputOutput() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-explicit-io-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              input 'src/main/xml/a.xml'
              output 'build/custom/b.xml'
            }
            """);

        write("src/main/xml/a.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xquery/main.xq", """
            <result>{ /root/value/text() }</result>
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);

        File output = new File(testProjectDir.getRoot(), "build/custom/b.xml");
        assertTrue(output.exists());
        assertTrue(read(output).contains("<result>Gradle</result>"));
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
        assertTrue(firstRun.getOutput().contains("[SUCCESS]"));

        long futureTimestamp = System.currentTimeMillis() + 60_000;
        assertTrue(output.setLastModified(futureTimestamp));

        BuildResult secondRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery", "--rerun-tasks")
            .build();

        assertTrue(secondRun.getOutput().contains("[SKIP]"));
    }

    /**
     * Verifies timestamp-based skip still applies even when non-file inputs (params) change.
     */
    @Test
    public void skipsTransformationWhenOutputIsNewerEvenIfParamsChange() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xquery-param-fingerprint-test'
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

        BuildResult firstRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery", "--rerun-tasks")
            .build();

        File output = new File(testProjectDir.getRoot(), "build/out/xquery/input.xml");
        assertTrue(output.exists());
        assertTrue(firstRun.getOutput().contains("[SUCCESS]"));
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));

        long futureTimestamp = System.currentTimeMillis() + 60_000;
        assertTrue(output.setLastModified(futureTimestamp));

        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              param 'prefix', 'Hi '
            }
            """);

        BuildResult secondRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery", "--rerun-tasks")
            .build();

        assertTrue(secondRun.getOutput().contains("[SKIP]"));
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));
    }

    @Test
    public void transformsSexprInputWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-sexpr-input-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/sexpr/input.sexpr'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
            }
            """);
        write("src/main/sexpr/input.sexpr", "(book [id \"b1\"] (title \"XML\"))");
        write("src/main/xquery/main.xq", "<result>{/book/title/text()}</result>");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.xml"));
        assertTrue(output.contains("<result>XML</result>"));
    }

    @Test
    public void writesSexprOutputWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-sexpr-output-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.sexpr')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xquery/main.xq", "<book id='b1'><title>{/book/title/text()}</title></book>");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.sexpr"));
        assertTrue(output.contains("(book"));
        assertTrue(output.contains("[id \"b1\"]"));
        assertTrue(output.contains("(title \"XML\")"));
    }

    @Test
    public void writesBeautifiedSexprOutputWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-sexpr-output-beautified-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.sexpr')
              sexprFormat.set('beautified')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xquery/main.xq", "<book id='b1'><title>{/book/title/text()}</title></book>");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.sexpr"));
        assertTrue(output.contains("(book"));
        assertTrue(output.contains("[id \"b1\"]"));
        assertTrue(output.contains("\n  (title \"XML\")"));
    }

    @Test
    public void transformsCanonicalJsonInputWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-json-input-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/json/input.json'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              jsonMode.set('canonical')
            }
            """);
        write("src/main/json/input.json", """
            {
              "type": "element",
              "name": "book",
              "attributes": { "id": "b1" },
              "children": [
                {
                  "type": "element",
                  "name": "title",
                  "attributes": {},
                  "children": [
                    { "type": "text", "value": "XML" }
                  ]
                }
              ]
            }
            """);
        write("src/main/xquery/main.xq", "<result>{/book/title/text()}</result>");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.xml"));
        assertTrue(output.contains("<result>XML</result>"));
    }

    @Test
    public void writesCanonicalJsonOutputWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-json-output-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXQuery', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xquery'))
              outputExtension.set('.json')
              jsonMode.set('canonical')
              sexprFormat.set('beautified')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xquery/main.xq", "<book id='b1'><title>{/book/title/text()}</title></book>");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXQuery")
            .build()
            .task(":runXQuery")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xquery/input.json"));
        assertTrue(output.contains("\n"));
        assertTrue(output.contains("\"name\" : \"book\""));
        assertTrue(output.contains("\"value\" : \"XML\""));
        assertTrue(!output.contains("\"attributes\" : { }"));
        assertTrue(!output.contains("\"attributes\":{}"));
    }

    @Test
    public void roundtripsXmlThroughCanonicalJsonWithXQuery() throws IOException {
        write("settings.gradle", "rootProject.name = 'xquery-json-roundtrip-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('xmlToJson', name.jurgenei.gradle.xml.XQueryTask) {
              query 'src/main/xquery/main.xq'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/json'))
              outputExtension.set('.json')
              jsonMode.set('canonical')
            }
            tasks.register('jsonToXml', name.jurgenei.gradle.xml.XQueryTask) {
              dependsOn 'xmlToJson'
              query 'src/main/xquery/main.xq'
              input 'build/out/json/input.json'
              output 'build/out/xml/result.xml'
              jsonMode.set('canonical')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xquery/main.xq", ".");

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("jsonToXml")
            .build()
            .task(":jsonToXml")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xml/result.xml"));
        assertTrue(output.contains("<book id=\"b1\">"));
        assertTrue(output.contains("<title>XML</title>"));
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

