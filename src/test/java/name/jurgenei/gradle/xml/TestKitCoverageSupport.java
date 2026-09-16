package name.jurgenei.gradle.xml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.gradle.testkit.runner.GradleRunner;

/**
 * Configures Gradle TestKit runner to attach JaCoCo agent to nested Gradle builds.
 */
final class TestKitCoverageSupport {

    private TestKitCoverageSupport() {
    }

    static GradleRunner newGradleRunner(File projectDir) {
        configureProjectJvmArgs(projectDir);
        return GradleRunner.create();
    }

    private static void configureProjectJvmArgs(File projectDir) {
        String agentPath = System.getProperty("test.jacoco.agent.path");
        String destFile = System.getProperty("test.jacoco.destfile");
        if (isBlank(agentPath) || isBlank(destFile)) {
            return;
        }
        String javaAgent = "-javaagent:" + new File(agentPath).getAbsolutePath()
            + "=destfile=" + new File(destFile).getAbsolutePath() + ",append=true";
        File gradleProperties = new File(projectDir, "gradle.properties");
        String propertyLine = "org.gradle.jvmargs=" + javaAgent;
        try {
            if (!gradleProperties.exists()) {
                Files.writeString(gradleProperties.toPath(), propertyLine + System.lineSeparator(), StandardCharsets.UTF_8);
                return;
            }
            List<String> lines = Files.readAllLines(gradleProperties.toPath(), StandardCharsets.UTF_8);
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("org.gradle.jvmargs=")) {
                    lines.set(i, propertyLine);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add(propertyLine);
            }
            Files.write(gradleProperties.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to configure gradle.properties for TestKit coverage", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
