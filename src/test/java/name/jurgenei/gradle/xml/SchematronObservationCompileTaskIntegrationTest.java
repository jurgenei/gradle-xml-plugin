package name.jurgenei.gradle.xml;

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
 * Integration tests for {@link SchematronObservationCompileTask}.
 */
public class SchematronObservationCompileTaskIntegrationTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void compilesObservationStylesheetSkeletonFromAnnotatedRules() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-obs-compile'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileObservation', name.jurgenei.gradle.xml.SchematronObservationCompileTask) {
              schema 'src/main/schematron/observations.sch'
              output 'build/generated/observation/observations.xsl'
              groupOutput 'knowledge', 'observations/knowledge.xml'
              groupOutput 'architecture', 'observations/architecture.xml'
            }
            """);

        write("src/main/schematron/observations.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
                        xmlns:c='http://jurgenei.name/canonical'
                        xmlns:obs='http://jurgenei.name/observation'>
              <sch:pattern id='knowledge'>
                <sch:rule context='c:Paragraph'>
                  <sch:report test='normalize-space(.)' obs:emit='true' obs:type='paragraph' obs:group='knowledge' obs:copy='.' obs:context='ancestor::c:Section[1]/c:Title'>
                    Paragraph evidence
                  </sch:report>
                </sch:rule>
              </sch:pattern>
              <sch:pattern id='architecture'>
                <sch:rule context='c:Connector'>
                  <sch:report test='@source and @target' obs:emit='true' obs:type='relationship-candidate' obs:group='architecture' obs:copy='.'>
                    Connector evidence
                  </sch:report>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("compileObservation")
            .withPluginClasspath()
            .build();

        File output = new File(testProjectDir.getRoot(), "build/generated/observation/observations.xsl");
        String stylesheet = read(output);

        assertTrue(output.exists());
        assertTrue(stylesheet.contains("obs:Observations group=\"knowledge\""));
        assertTrue(stylesheet.contains("obs:Observations group=\"architecture\""));
        assertTrue(stylesheet.contains("<obs:Observation type=\"paragraph\""));
        assertTrue(stylesheet.contains("<obs:Observation type=\"relationship-candidate\""));
        assertTrue(stylesheet.contains("<xsl:copy-of select=\".\"/>"));
        assertTrue(stylesheet.contains("<xsl:copy-of select=\"ancestor::c:Section[1]/c:Title\"/>"));
    }

    @Test
    public void compilesDefaultGroupWhenNoObsEmitRulesExist() throws Exception {
        write("settings.gradle", "rootProject.name = 'schematron-obs-default'\n");
        write("build.gradle", """
            plugins { id 'name.jurgenei.gradle.xml' }

            tasks.register('compileObservation', name.jurgenei.gradle.xml.SchematronObservationCompileTask) {
              schema 'src/main/schematron/observations.sch'
              output 'build/generated/observation/observations.xsl'
            }
            """);

        write("src/main/schematron/observations.sch", """
            <sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'>
              <sch:pattern id='noop'>
                <sch:rule context='*'>
                  <sch:assert test='true()'>No extraction annotations</sch:assert>
                </sch:rule>
              </sch:pattern>
            </sch:schema>
            """);

        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments("compileObservation")
            .withPluginClasspath()
            .build();

        File output = new File(testProjectDir.getRoot(), "build/generated/observation/observations.xsl");
        String stylesheet = read(output);

        assertTrue(output.exists());
        assertTrue(stylesheet.contains("output-default"));
        assertTrue(stylesheet.contains("obs:Observations group=\"default\""));
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

