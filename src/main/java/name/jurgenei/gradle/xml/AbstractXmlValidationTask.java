package name.jurgenei.gradle.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import name.jurgenei.gradle.xml.validation.ReportFormat;
import name.jurgenei.gradle.xml.validation.SvrlSupport;
import name.jurgenei.gradle.xml.validation.ValidationResult;
import name.jurgenei.gradle.xml.validation.ValidationTaskSpec;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Shared base task for XSD and Schematron validations producing SVRL and optional JUnit reports.
 */
@DisableCachingByDefault(because = "Validation is I/O heavy and depends on external schema resources")
public abstract class AbstractXmlValidationTask extends SourceTask implements ValidationTaskSpec {

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getOutputExtension();

    @Input
    public abstract Property<Integer> getWorkers();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @Input
    public abstract Property<Integer> getMaxFailures();

    @Input
    public abstract Property<ReportFormat> getReportFormat();

    @OutputDirectory
    public abstract DirectoryProperty getJunitOutputDir();

    @Input
    public abstract Property<String> getJunitSuiteName();

    @Input
    public abstract MapProperty<String, String> getParams();

    /**
     * Creates a validation task with default conventions.
     */
    public AbstractXmlValidationTask() {
        getOutputExtension().convention(".svrl.xml");
        getWorkers().convention(1);
        getFailOnError().convention(true);
        getMaxFailures().convention(1);
        getReportFormat().convention(ReportFormat.SVRL);
        getJunitSuiteName().convention(getName());
        getJunitOutputDir().convention(getProject().getLayout().getBuildDirectory().dir("reports/xml-validation/junit"));
    }

    /**
     * Adds a validation parameter available to concrete engines.
     *
     * @param name parameter name, must not be {@code null}
     * @param value parameter value converted to string
     */
    public void param(String name, Object value) {
        if (name == null) {
            throw new GradleException("Parameter name must not be null");
        }
        getParams().put(name, value == null ? "" : value.toString());
    }

    /**
     * Adds an Ant-like fileset rooted at baseDir.
     *
     * @param baseDir base directory object accepted by {@code Project.fileTree}
     * @param configureAction include/exclude configuration action
     */
    public void fileset(Object baseDir, Action<? super ConfigurableFileTree> configureAction) {
        ConfigurableFileTree tree = getProject().fileTree(baseDir);
        configureAction.execute(tree);
        source(tree);
    }

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    public org.gradle.api.file.FileTree getSource() {
        return super.getSource();
    }

    /**
     * Executes validation for all resolved source files.
     */
    @TaskAction
    public void validateAll() {
        List<File> inputFiles = new ArrayList<>(getSource().getFiles());
        Collections.sort(inputFiles);

        if (inputFiles.isEmpty()) {
            getLogger().lifecycle("{}: no input files matched", getName());
            return;
        }

        File outputRoot = getOutputDir().get().getAsFile();
        mkdirs(outputRoot);

        if (getReportFormat().get().writesJunit()) {
            mkdirs(getJunitOutputDir().get().getAsFile());
        }

        List<Exception> failures = Collections.synchronizedList(new ArrayList<>());
        int workers = Math.max(1, getWorkers().get());
        if (workers == 1 || inputFiles.size() == 1) {
            for (File inputFile : inputFiles) {
                validateOne(inputFile, outputRoot, failures);
            }
        } else {
            runParallel(inputFiles, outputRoot, workers, failures);
        }

        if (!failures.isEmpty() && getFailOnError().get()) {
            throw new GradleException("Validation failed for " + failures.size() + " input file(s)", failures.get(0));
        }
    }

    private void runParallel(List<File> inputFiles, File outputRoot, int workers, List<Exception> failures) {
        try (ExecutorService executor = Executors.newFixedThreadPool(workers, Thread.ofVirtual().name(getName() + "-vt-", 0).factory())) {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (File inputFile : inputFiles) {
                futures.add(executor.submit(() -> validateOne(inputFile, outputRoot, failures)));
            }
            for (java.util.concurrent.Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GradleException("Parallel validation interrupted", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception ex) {
                        failures.add(ex);
                    } else {
                        failures.add(new Exception(cause));
                    }
                }
            }
        }
    }

    private void validateOne(File inputFile, File outputRoot, List<Exception> failures) {
        File svrlFile = svrlFileFor(inputFile, outputRoot);
        mkdirs(svrlFile.getParentFile());

        try {
            ValidationResult result = validate(inputFile, getParams().get());
            String svrlXml = result.svrlXml() == null || result.svrlXml().isBlank()
                ? SvrlSupport.renderSvrl(inputFile.getPath(), result.issues())
                : result.svrlXml();

            if (getReportFormat().get().writesSvrl()) {
                Files.writeString(svrlFile.toPath(), svrlXml, StandardCharsets.UTF_8);
            }

            if (getReportFormat().get().writesJunit()) {
                File junitFile = junitFileFor(inputFile);
                mkdirs(junitFile.getParentFile());
                String junitXml = SvrlSupport.renderJunit(getJunitSuiteName().get(), inputFile.getName(), result.issues());
                Files.writeString(junitFile.toPath(), junitXml, StandardCharsets.UTF_8);
            }

            if (result.hasErrors()) {
                failures.add(new GradleException("Validation issues found in " + inputFile));
            }
        } catch (Exception e) {
            failures.add(new GradleException("Failed to validate " + inputFile, e));
        }
    }

    private File svrlFileFor(File inputFile, File outputRoot) {
        Path inputPath = inputFile.toPath().toAbsolutePath().normalize();
        Path projectPath = getProject().getProjectDir().toPath().toAbsolutePath().normalize();

        String relative = inputPath.startsWith(projectPath)
            ? projectPath.relativize(inputPath).toString()
            : inputFile.getName();

        int extensionIndex = relative.lastIndexOf('.');
        String replaced = extensionIndex >= 0
            ? relative.substring(0, extensionIndex) + getOutputExtension().get()
            : relative + getOutputExtension().get();
        return new File(outputRoot, replaced);
    }

    private File junitFileFor(File inputFile) {
        Path inputPath = inputFile.toPath().toAbsolutePath().normalize();
        Path projectPath = getProject().getProjectDir().toPath().toAbsolutePath().normalize();

        String relative = inputPath.startsWith(projectPath)
            ? projectPath.relativize(inputPath).toString()
            : inputFile.getName();

        int extensionIndex = relative.lastIndexOf('.');
        String replaced = extensionIndex >= 0
            ? relative.substring(0, extensionIndex) + ".junit.xml"
            : relative + ".junit.xml";
        return new File(getJunitOutputDir().get().getAsFile(), replaced);
    }

    private static void mkdirs(File dir) {
        try {
            Files.createDirectories(dir.toPath());
        } catch (Exception e) {
            throw new GradleException("Could not create directory: " + dir, e);
        }
    }

    /**
     * Validates a single input file and returns normalized findings.
     *
     * @param inputFile file to validate
     * @param params task-level validation parameters
     * @return normalized validation result
     * @throws Exception when validation cannot be performed
     */
    protected abstract ValidationResult validate(File inputFile, Map<String, String> params) throws Exception;
}

