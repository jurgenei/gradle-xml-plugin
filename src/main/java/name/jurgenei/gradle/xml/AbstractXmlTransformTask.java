package name.jurgenei.gradle.xml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import name.jurgenei.gradle.xml.sexpr.SExpressionSerializer;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Shared base task for Saxon-backed file-to-file XML transformations.
 *
 * <p>This task provides Gradle-style input selection via {@link #source(Object...)} or
 * {@link #fileset(Object, Action)}, output mapping, parameter passing, and optional
 * parallel processing with virtual-thread workers.</p>
 */
@DisableCachingByDefault(because = "Uses external transform engines and file trees")
public abstract class AbstractXmlTransformTask extends SourceTask {

    private static final Set<String> SUPPORTED_OUTPUT_METHODS = Set.of("xml", "json", "text");
    private static final Set<String> SUPPORTED_SEXPR_FORMATS = Set.of("compact", "beautified");
    private static final Set<String> SUPPORTED_JSON_MODES = Set.of("auto", "native", "canonical");

    /**
     * JSON routing mode for {@code .json} files.
     */
    public enum JsonMode {
        /**
         * Compatibility mode: canonical JSON parser for input, Saxon serializer for output.
         */
        AUTO,
        /**
         * Native Saxon routing for JSON output and no custom JSON parser for input.
         */
        NATIVE,
        /**
         * Canonical JSON parser and serializer for both JSON input and output.
         */
        CANONICAL
    }

    /**
     * Destination root directory for transformed files.
     *
     * @return output directory property
     */
    @Optional
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /**
     * Optional explicit single input file.
     *
     * <p>When set together with {@link #getOutputFile()}, the task runs in single-file mode and
     * does not require {@link #getOutputDir()} mapping.</p>
     *
     * @return explicit input file property
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    /**
     * Optional explicit single output file.
     *
     * @return explicit output file property
     */
    @Optional
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    /**
     * Output file extension used when mapping each input file to an output file.
     *
     * @return extension property, defaults to {@code .xml}
     */
    @Input
    public abstract Property<String> getOutputExtension();

    /**
     * Optional explicit serializer output method used by Saxon.
     *
     * <p>Supported values are {@code xml}, {@code json}, and {@code text}. If not set,
     * the method is inferred from the destination file extension.</p>
     *
     * @return output method property
     */
    @Input
    @Optional
    public abstract Property<String> getOutputMethod();

    /**
     * Optional S-expression output format used when writing {@code .sexpr} files.
     *
     * <p>Supported values are {@code compact} (default) and {@code beautified}.</p>
     *
     * @return S-expression output format property
     */
    @Input
    @Optional
    public abstract Property<String> getSexprFormat();

    /**
     * Optional JSON mode controlling how {@code .json} input/output is routed.
     *
     * <p>Supported values are {@code auto} (default), {@code native}, and {@code canonical}.</p>
     *
     * @return JSON routing mode property
     */
    @Input
    @Optional
    public abstract Property<String> getJsonMode();

    /**
     * Transform parameters exposed to the execution engine.
     *
     * @return map of parameter names to values
     */
    @Input
    public abstract MapProperty<String, String> getParams();

    /**
     * Number of worker threads used when processing multiple files.
     *
     * @return worker-count property, defaults to {@code 1}
     */
    @Input
    public abstract Property<Integer> getWorkers();

    /**
     * Controls whether a task failure should fail the build when a single file transform fails.
     *
     * @return fail-on-error property
     */
    @Input
    public abstract Property<Boolean> getFailOnError();

    /**
     * Creates a task with default conventions.
     */
    public AbstractXmlTransformTask() {
        getOutputExtension().convention(".xml");
        getSexprFormat().convention("compact");
        getJsonMode().convention("auto");
        getWorkers().convention(1);
        getFailOnError().convention(true);
    }

    /**
     * Adds or overrides a transformation parameter.
     *
     * @param name parameter name, must not be {@code null}
     * @param value parameter value, converted to string
     */
    public void param(String name, Object value) {
        if (name == null) {
            throw new GradleException("Parameter name must not be null");
        }
        getParams().put(name, value == null ? "" : value.toString());
    }

    /**
     * Adds an Ant-like fileset rooted at {@code baseDir}.
     *
     * @param baseDir base directory object accepted by {@code Project.fileTree}
     * @param configureAction include/exclude configuration action
     */
    public void fileset(Object baseDir, Action<? super ConfigurableFileTree> configureAction) {
        ConfigurableFileTree tree = getProject().fileTree(baseDir);
        configureAction.execute(tree);
        source(tree);
    }

    /**
     * Sets an explicit single input file (Ant-like {@code in}).
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void input(Object path) {
        File file = getProject().file(path);
        getInputFile().set(file);
        // Keep SourceTask non-empty so Gradle does not short-circuit task execution.
        source(file);
    }

    /**
     * Sets an explicit single output file (Ant-like {@code out}).
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void output(Object path) {
        getOutputFile().set(getProject().file(path));
    }

    /**
     * Sets an explicit serializer output method (Gradle DSL friendly).
     *
     * @param method one of {@code xml}, {@code json}, {@code text}
     */
    public void outputMethod(String method) {
        getOutputMethod().set(method);
    }

    /**
     * Sets explicit S-expression output format (Gradle DSL friendly).
     *
     * @param format one of {@code compact} or {@code beautified}
     */
    public void sexprFormat(String format) {
        getSexprFormat().set(format);
    }

    /**
     * Sets JSON routing mode (Gradle DSL friendly).
     *
     * @param mode one of {@code auto}, {@code native}, or {@code canonical}
     */
    public void jsonMode(String mode) {
        getJsonMode().set(mode);
    }

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    public org.gradle.api.file.FileTree getSource() {
        return super.getSource();
    }

    /**
     * Executes transformations for all resolved source files.
     *
     * <p>Files are processed deterministically and may run in parallel when
     * {@code workers > 1}.</p>
     */
    @TaskAction
    public void transformAll() {
        boolean hasExplicitInput = getInputFile().isPresent();
        boolean hasExplicitOutput = getOutputFile().isPresent();
        if (hasExplicitInput != hasExplicitOutput) {
            throw new GradleException("Both inputFile and outputFile must be set together for single-file mode");
        }

        Map<String, String> params = Collections.unmodifiableMap(new HashMap<>(getParams().getOrElse(Map.of())));

        if (hasExplicitInput) {
            transformExplicit(params);
            return;
        }

        List<File> inputFiles = new ArrayList<>(getSource().getFiles());
        Collections.sort(inputFiles);
        Map<Path, String> relativePaths = resolveRelativePaths();

        if (inputFiles.isEmpty()) {
            getLogger().lifecycle("{}: no input files matched", getName());
            return;
        }

        if (!getOutputDir().isPresent()) {
            throw new GradleException("outputDir is required when outputFile is not set");
        }

        File outputRoot = getOutputDir().get().getAsFile();
        if (!outputRoot.exists() && !outputRoot.mkdirs()) {
            throw new GradleException("Could not create output directory: " + outputRoot);
        }

        int workers = Math.max(1, getWorkers().get());
        List<Exception> failures = Collections.synchronizedList(new ArrayList<>());

        if (workers == 1 || inputFiles.size() == 1) {
            for (File inputFile : inputFiles) {
                transformOne(inputFile, outputRoot, relativePaths, params, failures);
            }
        } else {
            runParallel(inputFiles, outputRoot, relativePaths, params, workers, failures);
        }

        if (!failures.isEmpty()) {
            throw new GradleException("Transformation failed for " + failures.size() + " input file(s)", failures.get(0));
        }
    }

    private void transformExplicit(Map<String, String> params) {
        File inputFile = getInputFile().get().getAsFile();
        File outputFile = getOutputFile().get().getAsFile();
        if (!inputFile.isFile()) {
            throw new GradleException("Input file does not exist: " + inputFile);
        }

        long newestDependencyTimestamp = latestDependencyTimestamp(inputFile);
        if (outputFile.exists()
                && outputFile.lastModified() >= newestDependencyTimestamp) {
            getLogger().lifecycle("[SKIP] {}", inputFile);
            return;
        }

        File outputParent = outputFile.getParentFile();
        try {
            if (outputParent != null) {
                Files.createDirectories(outputParent.toPath());
            }
            transform(inputFile, outputFile, params);
            getLogger().lifecycle("[SUCCESS] {} -> {}", inputFile, outputFile);
        } catch (Exception e) {
            getLogger().error("Failed to transform {}", inputFile, e);
            getLogger().lifecycle("[FAILURE] {}", inputFile);
            if (getFailOnError().get()) {
                throw new GradleException("Failed to transform: " + inputFile, e);
            }
        }
    }

    private void runParallel(List<File> inputFiles, File outputRoot, Map<Path, String> relativePaths, Map<String, String> params, int workers, List<Exception> failures) {
        try (ExecutorService executor = Executors.newFixedThreadPool(workers, Thread.ofVirtual().name(getName() + "-vt-", 0).factory())) {
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (File inputFile : inputFiles) {
                futures.add(executor.submit(() -> transformOne(inputFile, outputRoot, relativePaths, params, failures)));
            }
            for (java.util.concurrent.Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GradleException("Parallel transform interrupted", e);
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

    private void transformOne(File inputFile, File outputRoot, Map<Path, String> relativePaths, Map<String, String> params, List<Exception> failures) {
        File outputFile = outputFileFor(inputFile, outputRoot, relativePaths);
        long newestDependencyTimestamp = latestDependencyTimestamp(inputFile);

        if (outputFile.exists()
                && outputFile.lastModified() >= newestDependencyTimestamp) {
            getLogger().lifecycle("[SKIP] {}", inputFile);
            return;
        }

        File outputParent = outputFile.getParentFile();
        try {
            Files.createDirectories(outputParent.toPath());
        } catch (Exception e) {
            failures.add(new GradleException("Could not create output directory: " + outputParent));
            return;
        }

        try {
            transform(inputFile, outputFile, params);
            getLogger().lifecycle("[SUCCESS] {} -> {}", inputFile, outputFile);
        } catch (Exception e) {
            getLogger().error("Failed to transform {}", inputFile, e);
            getLogger().lifecycle("[FAILURE] {}", inputFile);
            if (getFailOnError().get()) {
                failures.add(new GradleException("Failed to transform: " + inputFile, e));
            }
        }
    }

    /**
     * Returns the newest timestamp of any file dependency that influences a transformation.
     *
     * <p>By default this is the source file timestamp. Subclasses should override to include
     * additional inputs such as stylesheets or query files.</p>
     *
     * @param inputFile source XML document
     * @return newest dependency timestamp in epoch milliseconds
     */
    protected long latestDependencyTimestamp(File inputFile) {
        return inputFile.lastModified();
    }

    /**
     * Resolves the serializer output method from explicit task setting or output extension.
     *
     * @param outputFile destination file
     * @return serializer method to pass to Saxon
     */
    protected String resolveSerializerMethod(File outputFile) {
        if (getOutputMethod().isPresent()) {
            return normalizeAndValidateMethod(getOutputMethod().get());
        }
        return inferSerializerMethod(outputFile);
    }

    /**
     * Resolves S-expression serializer format from explicit task setting.
     *
     * @return serializer format to use for {@code .sexpr} and canonical {@code .json} output
     */
    protected SExpressionSerializer.OutputFormat resolveSexprOutputFormat() {
        String configured = getSexprFormat().getOrElse("compact");
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if ("pretty".equals(normalized)) {
            normalized = "beautified";
        }
        if (!SUPPORTED_SEXPR_FORMATS.contains(normalized)) {
            throw new GradleException("Unsupported sexprFormat '" + configured
                + "'. Supported values: compact, beautified");
        }
        return "beautified".equals(normalized)
            ? SExpressionSerializer.OutputFormat.BEAUTIFIED
            : SExpressionSerializer.OutputFormat.COMPACT;
    }

    /**
     * Resolves JSON routing mode from explicit task setting.
     *
     * @return JSON mode
     */
    protected JsonMode resolveJsonMode() {
        String configured = getJsonMode().getOrElse("auto");
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_JSON_MODES.contains(normalized)) {
            throw new GradleException("Unsupported jsonMode '" + configured
                + "'. Supported values: auto, native, canonical");
        }
        return switch (normalized) {
            case "native" -> JsonMode.NATIVE;
            case "canonical" -> JsonMode.CANONICAL;
            default -> JsonMode.AUTO;
        };
    }

    /**
     * Checks whether file extension matches S-expression input/output.
     *
     * @param file candidate file
     * @return true when file extension is {@code .sexpr}
     */
    protected boolean isSexprFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".sexpr");
    }

    /**
     * Checks whether file extension matches JSON input/output.
     *
     * @param file candidate file
     * @return true when file extension is {@code .json}
     */
    protected boolean isJsonFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    /**
     * AUTO and CANONICAL parse {@code .json} via canonical JSON reader.
     *
     * @param inputFile candidate source file
     * @return true when task should parse input using canonical JSON mapping
     */
    protected boolean useCanonicalJsonInput(File inputFile) {
        if (!isJsonFile(inputFile)) {
            return false;
        }
        JsonMode mode = resolveJsonMode();
        return mode == JsonMode.AUTO || mode == JsonMode.CANONICAL;
    }

    /**
     * JSON output should attempt canonical hierarchical serialization.
     *
     * <p>CANONICAL mode always attempts canonical JSON. AUTO/NATIVE attempt canonical JSON when
     * the resolved serializer method is {@code json}.</p>
     *
     * @param outputFile candidate destination file
     * @return true when task should try canonical JSON serializer before native fallback
     */
    protected boolean shouldAttemptCanonicalJsonOutput(File outputFile) {
        if (!isJsonFile(outputFile)) {
            return false;
        }
        JsonMode mode = resolveJsonMode();
        if (mode == JsonMode.CANONICAL) {
            return true;
        }
        return "json".equals(resolveSerializerMethod(outputFile));
    }

    /**
     * CANONICAL mode treats canonical JSON serialization failures as hard errors.
     *
     * @param outputFile candidate destination file
     * @return true when canonical JSON serialization failure must fail task
     */
    protected boolean isStrictCanonicalJsonOutput(File outputFile) {
        return isJsonFile(outputFile) && resolveJsonMode() == JsonMode.CANONICAL;
    }

    private String inferSerializerMethod(File outputFile) {
        String fileName = outputFile.getName().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".json")) {
            return "json";
        }
        if (fileName.endsWith(".txt") || fileName.endsWith(".text")) {
            return "text";
        }
        return "xml";
    }

    private String normalizeAndValidateMethod(String configuredMethod) {
        String normalized = configuredMethod == null ? "" : configuredMethod.trim().toLowerCase(Locale.ROOT);
        if ("txt".equals(normalized)) {
            normalized = "text";
        }
        if (!SUPPORTED_OUTPUT_METHODS.contains(normalized)) {
            throw new GradleException("Unsupported outputMethod '" + configuredMethod
                + "'. Supported values: xml, json, text");
        }
        return normalized;
    }


    private File outputFileFor(File inputFile, File outputRoot, Map<Path, String> relativePaths) {
        Path inputPath = inputFile.toPath().toAbsolutePath().normalize();
        Path projectPath = getProject().getProjectDir().toPath().toAbsolutePath().normalize();

        String relative = relativePaths.get(inputPath);
        if (relative == null) {
            relative = inputPath.startsWith(projectPath)
                ? projectPath.relativize(inputPath).toString()
                : inputFile.getName();
        }

        int extensionIndex = relative.lastIndexOf('.');
        String extension = getOutputExtension().get();
        String replaced = extensionIndex >= 0 ? relative.substring(0, extensionIndex) + extension : relative + extension;
        return new File(outputRoot, replaced);
    }

    private Map<Path, String> resolveRelativePaths() {
        Map<Path, String> relativePaths = new HashMap<>();
        getSource().visit(details -> {
            if (details.isDirectory()) {
                return;
            }
            Path absolutePath = details.getFile().toPath().toAbsolutePath().normalize();
            String relativePath = details.getRelativePath().getPathString();
            relativePaths.merge(absolutePath, relativePath,
                (existing, candidate) -> existing.length() <= candidate.length() ? existing : candidate);
        });
        return relativePaths;
    }

    /**
     * Executes one transformation from input file to output file.
     *
     * @param inputFile source XML document
     * @param outputFile destination file
     * @param params immutable task parameter view
     * @throws Exception any transform exception raised by the engine implementation
     */
    protected abstract void transform(File inputFile, File outputFile, Map<String, String> params) throws Exception;
}


