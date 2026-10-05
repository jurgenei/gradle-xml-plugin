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
 * Integration tests for {@link ShaclSelExtractTask}.
 */
public class ShaclSelExtractTaskIntegrationTest {

    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void extractsGroupedSelFromPrecompiledShaclStyle() throws Exception {
        write("settings.gradle", "rootProject.name = 'shacl-sel-extract'\n");
        write(
                "build.gradle",
                """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileShaclSel', name.jurgenei.gradle.xml.ShaclSelCompileTask) {
              schema 'src/main/shacl/collibra.shacl.xml'
              output 'build/generated/sel/shacl-sel.xsl'
              outputSchematron 'build/generated/sel/shacl-sel.sch'
              groupOutput 'relations', 'sel/relations.xml'
            }

            tasks.register('extractShaclSel', name.jurgenei.gradle.xml.ShaclSelExtractTask) {
              dependsOn tasks.named('compileShaclSel')
              schema 'build/generated/sel/shacl-sel.sch'
              style 'build/generated/sel/shacl-sel.xsl'
              source 'src/main/xml/case.xml'
              outputDir.set(layout.buildDirectory.dir('out/shacl-sel'))
              groupOutput 'relations', 'sel/relations.xml'
              failOnError.set(true)
            }
            """);

        write(
                "src/main/shacl/collibra.shacl.xml",
                """
            <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                     xmlns:sh='http://www.w3.org/ns/shacl#'
                     xmlns:cr='https://example.org/collibra#'>
              <sh:NodeShape rdf:about='https://example.org/collibra#ApplicationComponentShape'>
                <sh:targetClass rdf:resource='https://example.org/collibra#ApplicationComponent'/>
                <sh:property>
                  <sh:PropertyShape>
                    <sh:path rdf:resource='https://example.org/collibra#hasDataSet'/>
                    <sh:class rdf:resource='https://example.org/collibra#DataSet'/>
                  </sh:PropertyShape>
                </sh:property>
              </sh:NodeShape>
            </rdf:RDF>
            """);

        write(
                "src/main/xml/case.xml",
                """
            <Case>
              <ApplicationComponent id='app-1'>
                <hasDataSet>
                  <DataSet id='ds-1'/>
                </hasDataSet>
              </ApplicationComponent>
            </Case>
            """);

        newGradleRunner()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("extractShaclSel")
                .withPluginClasspath()
                .build();

        File output =
                new File(testProjectDir.getRoot(), "build/out/shacl-sel/case/sel/relations.xml");
        String xml = read(output);
        assertTrue(output.exists());
        assertTrue(xml.contains("group=\"relations\""));
        assertTrue(xml.contains("type=\"hasDataSet\""));
        assertTrue(xml.contains("ApplicationComponent"));
    }

    private void write(String relativePath, String content) throws IOException {
        File file = new File(testProjectDir.getRoot(), relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private GradleRunner newGradleRunner() {
        return TestKitCoverageSupport.newGradleRunner(testProjectDir.getRoot());
    }

    private String read(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
