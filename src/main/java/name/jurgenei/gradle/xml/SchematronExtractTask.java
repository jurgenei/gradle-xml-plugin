package name.jurgenei.gradle.xml;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase-3 runtime task that executes Schematron-based observation extraction.
 *
 * <p>The task can consume a precompiled extraction stylesheet or compile one on the fly
 * from annotation-bearing Schematron rules.</p>
 */
@DisableCachingByDefault(because = "Extraction output fan-out depends on source trees and dynamic grouped mappings")
public abstract class SchematronExtractTask extends org.gradle.api.DefaultTask {

    @Inject
    public SchematronExtractTask() {
        getFailOnError().convention(true);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getStyle();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract MapProperty<String, String> getGroupOutputs();

    @Input
    public abstract Property<Boolean> getFailOnError();

    /**
     * Sets Schematron schema file.
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
     * Sets precompiled extraction stylesheet file.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void style(Object path) {
        getStyle().set(getProject().file(path));
    }

    /**
     * Adds input source(s) using Gradle file notation.
     *
     * @param source file, directory, fileTree, or collection.
     */
    public void source(Object source) {
        getSourceFiles().from(source);
    }

    /**
     * Adds source file tree include/exclude configuration.
     *
     * @param sourceDir root directory.
     * @param action include/exclude action.
     */
    public void source(Object sourceDir, org.gradle.api.Action<? super PatternFilterable> action) {
        org.gradle.api.file.ConfigurableFileTree tree = getProject().fileTree(sourceDir);
        action.execute(tree);
        source(tree);
    }

    /**
     * Configures output path mapping for a logical group.
     *
     * @param group observation group key.
     * @param relativePath output path under each input-stem root.
     */
    public void groupOutput(String group, String relativePath) {
        getGroupOutputs().put(group, relativePath);
    }

    @TaskAction
    public void extract() {
        Set<File> rawInputs = new LinkedHashSet<>(getSourceFiles().getFiles());
        if (rawInputs.isEmpty()) {
            throw new GradleException("No input files configured. Use source(...) to provide canonical XML files.");
        }

        List<File> inputs = new ArrayList<>(rawInputs);
        inputs.sort(Comparator.comparing(File::getAbsolutePath));

        try {
            Files.createDirectories(getOutputDir().get().getAsFile().toPath());
            RuntimeStylesheet runtimeStylesheet = resolveRuntimeStylesheet();
            for (File input : inputs) {
                runExtraction(runtimeStylesheet, input);
            }
        } catch (Exception e) {
            if (getFailOnError().get()) {
                throw new GradleException("Observation extraction failed", e);
            }
            getLogger().error("Observation extraction failed but failOnError=false", e);
        }
    }

    private RuntimeStylesheet resolveRuntimeStylesheet() throws Exception {
        Document schemaDoc = parseSchema(getSchema().get().getAsFile());
        List<ObservationRuleDescriptor> rules = ObservationRuleCollector.collect(schemaDoc);
        List<String> groups = collectGroups(rules);

        if (getStyle().isPresent()) {
            return new RuntimeStylesheet(getStyle().get().getAsFile().toPath(), groups);
        }

        String stylesheetXml = ObservationStylesheetCompiler.render(rules, getGroupOutputs().getOrElse(Map.of()));
        Path temp = Files.createTempFile("observation-compiled-", ".xsl");
        Files.writeString(temp, stylesheetXml, StandardCharsets.UTF_8);
        temp.toFile().deleteOnExit();
        return new RuntimeStylesheet(temp, groups);
    }

    private void runExtraction(RuntimeStylesheet runtimeStylesheet, File inputFile) throws Exception {
        Path base = getOutputDir().get().getAsFile().toPath().resolve(stem(inputFile.getName()));
        Files.createDirectories(base);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(runtimeStylesheet.stylesheet().toFile()));
        XsltTransformer transformer = executable.load();
        transformer.setSource(new StreamSource(inputFile));
        transformer.setParameter(new QName("source-document"), new XdmAtomicValue(inputFile.getName()));
        transformer.setBaseOutputURI(base.toUri().toString());

        for (String group : runtimeStylesheet.groups()) {
            String configured = getGroupOutputs().getOrElse(Map.of()).getOrDefault(group, "observations/" + group + ".xml");
            Path target = base.resolve(configured).normalize();
            Files.createDirectories(target.getParent());
            transformer.setParameter(new QName("output-" + group), new XdmAtomicValue(configured));
        }

        StringWriter sink = new StringWriter();
        Serializer serializer = processor.newSerializer(sink);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();
    }

    private List<String> collectGroups(List<ObservationRuleDescriptor> rules) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        for (ObservationRuleDescriptor rule : rules) {
            groups.add(rule.group());
        }
        if (groups.isEmpty()) {
            groups.add("default");
        }
        return new ArrayList<>(groups);
    }

    private Document parseSchema(File schemaFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(schemaFile);
    }

    private String stem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private record RuntimeStylesheet(Path stylesheet, List<String> groups) {
    }
}


