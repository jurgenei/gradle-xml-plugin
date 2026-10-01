package name.jurgenei.gradle.xml;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link SchematronSelCompileTask}.
 */
public class SchematronSelCompileTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void compilesSelStylesheetSkeletonFromAnnotatedRules() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-compile'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileSel', name.jurgenei.gradle.xml.SchematronSelCompileTask) {
              schema 'src/main/schematron/sel.sch'
              output 'build/generated/sel/sel.xsl'
              groupOutput 'knowledge', 'sel/knowledge.xml'
              groupOutput 'architecture', 'sel/architecture.xml'
            }
            """);

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:sel='http://jurgenei.name/sel'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' sel:emit='true' sel:type='paragraph' sel:group='knowledge' sel:copy='.' sel:context='ancestor::c:Section[1]/c:Title'>
                    Paragraph evidence
                  </sch:report>
                </sch:rule>
              </sch:pattern>
              <sch:pattern id='architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' sel:emit='true' sel:type='relationship-candidate' sel:group='architecture' sel:copy='.'>
                    Connector evidence
                  </sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("compileSel")
            .withPluginClasspath()
            .build();

        File output = new File(testProjectDir.getRoot(), "build/generated/sel/sel.xsl");
        String stylesheet = read(output);

        assertTrue(output.exists());
        assertTrue(stylesheet.contains("sel:Observations group=\"knowledge\""));
        assertTrue(stylesheet.contains("sel:Observations group=\"architecture\""));
        assertTrue(stylesheet.contains("<sel:Observation type=\"paragraph\""));
        assertTrue(stylesheet.contains("<sel:Observation type=\"relationship-candidate\""));
        assertTrue(stylesheet.contains("<xsl:copy-of select=\".\"/>"));
        assertTrue(stylesheet.contains("<xsl:copy-of select=\"ancestor::c:Section[1]/c:Title\"/>"));
    }

    @Test
    public void compilesDefaultGroupWhenNoSelEmitRulesExist() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-default'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileSel', name.jurgenei.gradle.xml.SchematronSelCompileTask) {
              schema 'src/main/schematron/sel.sch'
              output 'build/generated/sel/sel.xsl'
            }
            """);

        write("src/main/schematron/sel.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'>
              <sch:pattern id='noop'>
                <sch:rule context='*'>
                  <sch:assert test='true()'>No extraction annotations</sch:assert>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("compileSel")
            .withPluginClasspath()
            .build();

        File output = new File(testProjectDir.getRoot(), "build/generated/sel/sel.xsl");
        String stylesheet = read(output);

        assertTrue(output.exists());
        assertTrue(stylesheet.contains("output-default"));
        assertTrue(stylesheet.contains("sel:Observations group=\"default\""));
    }

    @Test
    public void failsWhenSchemaPathDoesNotExist() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-sel-missing-schema'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileSel', name.jurgenei.gradle.xml.SchematronSelCompileTask) {
              schema 'src/main/schematron/missing.sch'
              output 'build/generated/sel/sel.xsl'
            }
            """);

        BuildResult result = newGradleRunner()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("compileSel")
            .withPluginClasspath()
            .buildAndFail();

        assertTrue(result.getOutput().contains("Schematron schema does not exist"));
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
