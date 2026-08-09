package name.jurgenei.gradle.xml;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Compiles annotation-bearing Schematron rules into an executable phase-2 observation stylesheet.
 */
@DisableCachingByDefault(because = "Compiler output depends on schema content and extraction annotation metadata")
public abstract class SchematronObservationCompileTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    @OutputFile
    public abstract RegularFileProperty getOutputStylesheet();

    @Input
    public abstract MapProperty<String, String> getGroupOutputs();

    @Inject
    public SchematronObservationCompileTask() {
    }

    /**
     * Sets the Schematron schema source in DSL-friendly form.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Schematron schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    /**
     * Sets compiled extraction stylesheet output path.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void output(Object path) {
        getOutputStylesheet().set(getProject().file(path));
    }

    /**
     * Maps logical observation group to output document path used by generated stylesheet.
     *
     * @param group logical group key.
     * @param outputPath output path expression.
     */
    public void groupOutput(String group, String outputPath) {
        getGroupOutputs().put(group, outputPath);
    }

    @TaskAction
    public void compile() {
        try {
            Document document = parseSchema(getSchema().get().getAsFile());
            List<ObservationRuleDescriptor> rules = ObservationRuleCollector.collect(document);
            Map<String, String> groupOutputs = getGroupOutputs().getOrElse(Map.of());
            String stylesheet = ObservationStylesheetCompiler.render(rules, groupOutputs);

            File outputFile = getOutputStylesheet().get().getAsFile();
            File parent = outputFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Files.writeString(outputFile.toPath(), stylesheet, StandardCharsets.UTF_8);
            getLogger().lifecycle("Compiled observation extraction stylesheet with {} rule(s): {}", rules.size(), outputFile);
        } catch (Exception e) {
            throw new GradleException("Failed to compile Schematron observation stylesheet", e);
        }
    }

    private Document parseSchema(File schemaFile) throws Exception {
        try (InputStream input = Files.newInputStream(schemaFile.toPath())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        }
    }
}

