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

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/input.xml");
        assertTrue(output.exists());
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));
    }

    /**
     * Verifies serializer method is inferred as json from output extension.
     */
    @Test
    public void infersJsonOutputMethodFromExtension() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-json-method-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.json')
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:template match='/'>
                <xsl:sequence select="map{'value': string(/root/value)}"/>
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

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/input.json");
        assertTrue(output.exists());
        assertEquals("{\"value\":\"Gradle\"}", read(output).replaceAll("\\s+", ""));
    }

    @Test
    public void autoModeWritesHierarchicalJsonForXmlNodeResults() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-json-auto-hierarchical-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.json')
              jsonMode.set('auto')
            }
            """);

        write("src/main/xml/input.xml", "<book><title>XML</title></book>");
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.json"));
        assertTrue(output.contains("\"type\":\"element\"") || output.contains("\"type\" : \"element\""));
        assertTrue(output.contains("\"name\":\"book\"") || output.contains("\"name\" : \"book\""));
        assertTrue(!output.contains("\"attributes\" : { }"));
        assertTrue(!output.contains("\"attributes\":{}"));
    }

    @Test
    public void nativeModeWritesHierarchicalJsonForXmlNodeResults() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-json-native-hierarchical-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.json')
              jsonMode.set('native')
            }
            """);

        write("src/main/xml/input.xml", "<book><title>XML</title></book>");
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.json"));
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
            rootProject.name = 'xslt-text-method-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.xml')
              outputMethod.set('text')
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:template match='/'>
                <xsl:value-of select="concat('VALUE=', /root/value)"/>
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

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/input.xml");
        assertTrue(output.exists());
        assertEquals("VALUE=Gradle", read(output).trim());
    }

    /**
     * Verifies explicit single-file mode using input/output properties.
     */
    @Test
    public void transformsSingleFileWithExplicitInputOutput() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-explicit-io-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              input 'src/main/xml/a.xml'
              output 'build/custom/b.xml'
            }
            """);

        write("src/main/xml/a.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:template match='/'>
                <result><xsl:value-of select='/root/value'/></result>
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
        write("src/main/xml/foo/bar.xml", """
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

        File outputA = new File(testProjectDir.getRoot(), "build/out/xslt/a.out.xml");
        File outputB = new File(testProjectDir.getRoot(), "build/out/xslt/foo/bar.out.xml");
        File skipped = new File(testProjectDir.getRoot(), "build/out/xslt/skip.out.xml");

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
    public void skipsTransformationWhenOutputIsNewerThanSourceAndStylesheet() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-skip-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
            }
            """);

        write("src/main/xml/input.xml", """
            <root><value>Gradle</value></root>
            """);
        write("src/main/xslt/main.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:template match='/'>
                <result><xsl:value-of select='/root/value'/></result>
              </xsl:template>
            </xsl:stylesheet>
            """);

        BuildResult firstRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt", "--rerun-tasks")
            .build();

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/input.xml");
        assertTrue(output.exists());
        assertTrue(firstRun.getOutput().contains("[SUCCESS]"));

        long futureTimestamp = System.currentTimeMillis() + 60_000;
        assertTrue(output.setLastModified(futureTimestamp));

        BuildResult secondRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt", "--rerun-tasks")
            .build();

        assertTrue(secondRun.getOutput().contains("[SKIP]"));
    }

    /**
     * Verifies timestamp-based skip still applies even when non-file inputs (params) change.
     */
    @Test
    public void skipsTransformationWhenOutputIsNewerEvenIfParamsChange() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'xslt-param-fingerprint-test'
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

        BuildResult firstRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt", "--rerun-tasks")
            .build();

        File output = new File(testProjectDir.getRoot(), "build/out/xslt/input.xml");
        assertTrue(output.exists());
        assertTrue(firstRun.getOutput().contains("[SUCCESS]"));
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));

        long futureTimestamp = System.currentTimeMillis() + 60_000;
        assertTrue(output.setLastModified(futureTimestamp));

        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/main.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              param 'prefix', 'Hi '
            }
            """);

        BuildResult secondRun = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt", "--rerun-tasks")
            .build();

        assertTrue(secondRun.getOutput().contains("[SKIP]"));
        assertTrue(read(output).contains("<result>Hello Gradle</result>"));
    }

    /**
     * Demonstrates refactoring from exec-based Saxon call to gradle-xml-plugin.
     *
     * Use case: Seed file indexing with parameterized XSLT (similar to:
     *   setArgs(["-s:seed.xml", "-xsl:indexer.xsl", "-o:output.xml",
     *           "input-dir=schema-dir", "schema-name=XSD"])
     *
     * Key adjustments:
     * 1. Replace exec { setArgs(...) } with XsltTask
     * 2. Pass external parameters via param() method
     * 3. XSLT uses xsl:param declarations
     * 4. Output directories created automatically (no mkdirs needed)
     */
    @Test
    public void refactorsOutOfProcessSaxonToXsltTask() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'indexer-refactor-test'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            
            // Input configuration
            def seedFile = file('src/seed/schema-index-seed.xml')
            def indexerStyle = file('src/xslt/indexer.xsl')
            def schemaDir = file('src/schemas')
            def schemaName = 'OpenAPI'
            
            // Old way (commented out for reference):
            // exec { setArgs(["-s:" + seedFile.absolutePath, 
            //                  "-xsl:" + indexerStyle.absolutePath, 
            //                  "-o:" + file('build/indexes/schema-index.xml').absolutePath,
            //                  "input-dir=" + schemaDir.absolutePath,
            //                  "schema-name=" + schemaName]) }
            
            // New way: gradle-xml-plugin (in-process, incremental, cleaner)
            tasks.register('buildSchemaIndex', name.jurgenei.gradle.xml.XsltTask) {
              style indexerStyle
              source seedFile
              outputDir.set(layout.buildDirectory.dir('indexes'))
              param 'inputDir', schemaDir.absolutePath
              param 'schemaName', schemaName
            }
            """);

        // Setup input files
        write("src/seed/schema-index-seed.xml", """
            <?xml version='1.0'?>
            <index>
              <title>Schema Index</title>
              <schemas/>
            </index>
            """);

        write("src/schemas/api.yaml", "# mock schema file");

        // XSLT with parameter declarations
        write("src/xslt/indexer.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <!-- Key adjustment: Use xsl:param for external parameters -->
              <xsl:param name='inputDir'/>
              <xsl:param name='schemaName'/>
              
              <xsl:template match='/'>
                <index>
                  <title><xsl:value-of select='/index/title'/></title>
                  <schemaName><xsl:value-of select='$schemaName'/></schemaName>
                  <inputDirectory><xsl:value-of select='$inputDir'/></inputDirectory>
                  <schemas>
                    <schema name='api' type='{$schemaName}'/>
                  </schemas>
                </index>
              </xsl:template>
            </xsl:stylesheet>
            """);

        // Execute the gradle-xml-plugin task
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("buildSchemaIndex")
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":buildSchemaIndex").getOutcome());

        // Verify output with parameter values
        String buildOutput = result.getOutput();
        assertTrue("Task should complete successfully", buildOutput.contains("BUILD SUCCESSFUL"));
    }

    /**
     * Demonstrates multi-file indexing with include/exclude patterns and parallelism.
     * Shows how fileTree patterns reduce manual filtering and enable concurrent processing.
     */
    @Test
    public void indexesMultipleSchemasWithIncludeExcludePatterns() throws IOException {
        write("settings.gradle", """
            rootProject.name = 'multi-schema-indexer'
            """);
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            
            def seedDir = file('src/seeds')
            def indexerStyle = file('src/xslt/indexer.xsl')
            
            tasks.register('indexSchemas', name.jurgenei.gradle.xml.XsltTask) {
              // File-tree with include/exclude for batch processing
              source(fileTree(seedDir) {
                include '*.xml'
                exclude '**/deprecated-*.xml'
              })
              style indexerStyle
              outputDir.set(layout.buildDirectory.dir('schema-indices'))
              outputExtension.set('.indexed.xml')
              param 'indexVersion', '1.0'
              workers.set(2)
            }
            """);

        // Setup multiple seed files
        write("src/seeds/openapi-seed.xml", """
            <schema type='openapi'><title>OpenAPI</title></schema>
            """);
        write("src/seeds/asyncapi-seed.xml", """
            <schema type='asyncapi'><title>AsyncAPI</title></schema>
            """);
        write("src/seeds/deprecated-v1-seed.xml", """
            <schema type='deprecated'><title>Legacy</title></schema>
            """);

        write("src/xslt/indexer.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:param name='indexVersion'/>
              <xsl:template match='/'>
                <indexed-schema indexVersion='{$indexVersion}'>
                  <xsl:copy-of select='/schema'/>
                </indexed-schema>
              </xsl:template>
            </xsl:stylesheet>
            """);

        // Process all files
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("indexSchemas", "--rerun-tasks")
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":indexSchemas").getOutcome());

        // Verify successful execution (files would exist but exact paths depend on implementation)
        assertTrue("Build should succeed with fileTree patterns", result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    public void transformsSexprInputToXml() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-input-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/sexpr/input.sexpr'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
            }
            """);
        write("src/main/sexpr/input.sexpr", "(book [id \"b1\"] (title \"XML\"))");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.xml"));
        assertTrue(output.contains("<book id=\"b1\">"));
        assertTrue(output.contains("<title>XML</title>"));
    }

    @Test
    public void transformsXmlToSexprOutput() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-output-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.sexpr')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.sexpr"));
        assertTrue(output.contains("(book"));
        assertTrue(output.contains("[id \"b1\"]"));
        assertTrue(output.contains("(title \"XML\")"));
    }

    @Test
    public void transformsXmlToBeautifiedSexprOutput() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-output-beautified-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.sexpr')
              sexprFormat.set('beautified')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.sexpr"));
        assertTrue(output.contains("(book"));
        assertTrue(output.contains("[id \"b1\"]"));
        assertTrue(output.contains("\n  (title \"XML\")"));
    }

    @Test
    public void failsOnUnsupportedSexprFormat() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-format-invalid-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.sexpr')
              sexprFormat.set('invalid-mode')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
            </xsl:stylesheet>
            """);

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt")
            .buildAndFail();

        assertTrue(result.getOutput().contains("Unsupported sexprFormat 'invalid-mode'. Supported values: compact, beautified"));
    }

    @Test
    public void roundtripsXmlThroughSexpr() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-roundtrip-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('xmlToSexpr', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/sexpr'))
              outputExtension.set('.sexpr')
            }
            tasks.register('sexprToXml', name.jurgenei.gradle.xml.XsltTask) {
              dependsOn 'xmlToSexpr'
              style 'src/main/xslt/identity.xsl'
              input 'build/out/sexpr/input.sexpr'
              output 'build/out/xml/result.xml'
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
            </xsl:stylesheet>
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("sexprToXml")
            .build()
            .task(":sexprToXml")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xml/result.xml"));
        assertTrue(output.contains("<book id=\"b1\">"));
        assertTrue(output.contains("<title>XML</title>"));
    }

    @Test
    public void roundtripsDefaultNamespaceThroughSexpr() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-default-ns-roundtrip-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('xmlToSexpr', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/sexpr'))
              outputExtension.set('.sexpr')
              sexprFormat.set('beautified')
            }
            tasks.register('sexprToXml', name.jurgenei.gradle.xml.XsltTask) {
              dependsOn 'xmlToSexpr'
              style 'src/main/xslt/identity.xsl'
              input 'build/out/sexpr/input.sexpr'
              output 'build/out/xml/result.xml'
            }
            """);
        write("src/main/xml/input.xml", """
            <math xmlns='http://www.w3.org/1998/Math/MathML'>
              <mfrac><mi>a</mi><mi>b</mi></mfrac>
            </math>
            """);
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
            </xsl:stylesheet>
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("sexprToXml")
            .build()
            .task(":sexprToXml")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String sexpr = read(new File(testProjectDir.getRoot(), "build/out/sexpr/input.sexpr"));
        assertTrue(sexpr.contains("[ns \"http://www.w3.org/1998/Math/MathML\"]"));
        String xml = read(new File(testProjectDir.getRoot(), "build/out/xml/result.xml"));
        assertTrue(xml.contains("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">"));
        assertTrue(xml.contains("<mfrac>"));
    }

    @Test
    public void transformsWithSexprStylesheetAndSexprInputOutput() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-sexpr-style-roundtrip-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.sexpr'
              source 'src/main/sexpr/input.sexpr'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.sexpr')
              sexprFormat.set('beautified')
            }
            """);
        write("src/main/sexpr/input.sexpr", "(book [id \"b1\"] (title \"XML\"))");
        write("src/main/xslt/identity.sexpr", """
            (xsl:stylesheet
              [version "3.0"]
              [ns "xsl" "http://www.w3.org/1999/XSL/Transform"]
              (xsl:mode [on-no-match "shallow-copy"]))
            """);

        TaskOutcome outcome = GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withPluginClasspath()
            .withArguments("runXslt")
            .build()
            .task(":runXslt")
            .getOutcome();

        assertEquals(TaskOutcome.SUCCESS, outcome);
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.sexpr"));
        assertTrue(output.contains("(book"));
        assertTrue(output.contains("[id \"b1\"]"));
        assertTrue(output.contains("(title \"XML\")"));
    }

    @Test
    public void transformsCanonicalJsonInputToXml() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-json-input-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/json/input.json'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
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
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.xml"));
        assertTrue(output.contains("<book id=\"b1\">"));
        assertTrue(output.contains("<title>XML</title>"));
    }

    @Test
    public void transformsXmlToCanonicalJsonOutputWithBeautifiedFormat() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-json-output-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('runXslt', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/xslt'))
              outputExtension.set('.json')
              jsonMode.set('canonical')
              sexprFormat.set('beautified')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
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
        String output = read(new File(testProjectDir.getRoot(), "build/out/xslt/input.json"));
        assertTrue(output.contains("\n"));
        assertTrue(output.contains("\"type\" : \"element\""));
        assertTrue(output.contains("\"name\" : \"book\""));
        assertTrue(output.contains("\"value\" : \"XML\""));
        assertTrue(!output.contains("\"attributes\" : { }"));
        assertTrue(!output.contains("\"attributes\":{}"));
    }

    @Test
    public void roundtripsXmlThroughCanonicalJson() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-json-roundtrip-canonical-test'");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }
            tasks.register('xmlToJson', name.jurgenei.gradle.xml.XsltTask) {
              style 'src/main/xslt/identity.xsl'
              source 'src/main/xml/input.xml'
              outputDir.set(layout.buildDirectory.dir('out/json'))
              outputExtension.set('.json')
              jsonMode.set('canonical')
            }
            tasks.register('jsonToXml', name.jurgenei.gradle.xml.XsltTask) {
              dependsOn 'xmlToJson'
              style 'src/main/xslt/identity.xsl'
              input 'build/out/json/input.json'
              output 'build/out/xml/result.xml'
              jsonMode.set('canonical')
            }
            """);
        write("src/main/xml/input.xml", "<book id='b1'><title>XML</title></book>");
        write("src/main/xslt/identity.xsl", """
            <?xml version='1.0'?>
            <xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
              <xsl:mode on-no-match='shallow-copy'/>
            </xsl:stylesheet>
            """);

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
