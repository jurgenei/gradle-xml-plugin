package name.jurgenei.gradle.xml;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import name.jurgenei.gradle.xml.validation.SvrlSupport;
import name.jurgenei.gradle.xml.validation.ValidationResult;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import org.gradle.work.DisableCachingByDefault;

/**
 * Validates XML files via Schematron by compiling .sch to XSLT and producing SVRL output.
 */
@DisableCachingByDefault(because = "Schematron compilation and validation depends on external schema files")
public abstract class SchematronTask extends AbstractXmlValidationTask {

    private static final String SCHXSLT_NS = "http://dmaus.name/ns/2023/schxslt";
    private static final Set<String> STATIC_TRANSPILER_PARAMS = Set.of(
        "debug",
        "streamable",
        "location-function",
        "fail-early",
        "terminate-validation-on-error",
        "report-active-pattern",
        "report-fired-rule",
        "report-suppressed-rule",
        "report-skipped-assertion",
        "compact-report",
        "check-assembled-schema"
    );
    private final Object transpilerCompileLock = new Object();

    /**
     * Creates a Schematron validation task.
     */
    public SchematronTask() {
    }

    /**
     * Returns the Schematron rules file used for validation.
     *
     * @return Schematron schema file used for validation
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    /**
     * Returns an optional override for the bundled SchXslt transpiler stylesheet.
     *
     * @return optional transpiler stylesheet overriding bundled SchXslt transpiler
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getTranspilerStylesheet();

    /**
     * Optional destination for a compiled Schematron stylesheet.
     *
     * <p>When set, the task reuses this file and recompiles only when needed based on
     * schema/transpiler timestamps and transpiler-parameter fingerprint changes.</p>
     *
     * @return optional compiled stylesheet location
     */
    @Optional
    @OutputFile
    public abstract RegularFileProperty getStyle();

    /**
     * SchXslt transpiler parameter {@code schxslt:debug}.
     * Enable or disable debugging. When debugging is enable, the validation stylesheet is indented.
     * Defaults to false.
     *
     * @return optional debug flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * SchXslt transpiler parameter {@code schxslt:phase}.
     * Name of the validation phase. The value '#DEFAULT' selects the pattern in the
     * {@code sch:schema/@defaultPhase} attribute or '#ALL' if this attribute is not present.
     * The value '#ALL' selects all patterns. Defaults to '#DEFAULT'.
     *
     * @return optional validation phase
     */
    @Optional
    @Input
    public abstract Property<String> getPhase();

    /**
     * SchXslt transpiler parameter {@code schxslt:expand-text}.
     * When set to boolean true, the validation stylesheet globally enables text value templates and
     * you may use them in assertion or diagnostic messages. Defaults to false.
     *
     * @return optional expand-text flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getExpandText();

    /**
     * SchXslt transpiler parameter {@code schxslt:streamable}.
     * Set to boolean true to create a streamable validation stylesheet. This does not check the
     * streamability of XPath expressions in rules, assertions, variables etc. It merely declares the
     * modes in the validation stylesheet to be streamable and removes the {@code @location} attribute
     * from the SVRL output when no location function is given because the default {@code fn:path()}
     * is not streamable. Defaults to false.
     *
     * @return optional streamable flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getStreamable();

    /**
     * SchXslt transpiler parameter {@code schxslt:location-function}.
     * Name of a function {@code f($context as node()) as xs:string} that provides location information
     * for the SVRL report. Defaults to {@code fn:path()} when not set.
     *
     * @return optional location function name
     */
    @Optional
    @Input
    public abstract Property<String> getLocationFunction();

    /**
     * SchXslt transpiler parameter {@code schxslt:fail-early}.
     * When set to boolean true, the validation stylesheet stops as soon as it encounters the first
     * failed assertion or successful report. Defaults to false.
     *
     * @return optional fail-early flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getFailEarly();

    /**
     * SchXslt transpiler parameter {@code schxslt:terminate-validation-on-error}.
     * When set to boolean true, the validation stylesheet terminates the XSLT processor when it
     * encounters a dynamic error. Defaults to true.
     *
     * @return optional terminate-on-error flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getTerminateValidationOnError();

    /**
     * SchXslt transpiler parameter {@code schxslt:report-active-pattern}.
     * When set to boolean true, the validation stylesheet reports active patterns and groups.
     * Defaults to true.
     *
     * @return optional report-active-pattern flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getReportActivePattern();

    /**
     * SchXslt transpiler parameter {@code schxslt:report-fired-rule}.
     * When set to boolean true, the validation stylesheet reports fired rules. Defaults to true.
     *
     * @return optional report-fired-rule flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getReportFiredRule();

    /**
     * SchXslt transpiler parameter {@code schxslt:report-suppressed-rule}.
     * When set to boolean true, the validation stylesheet reports suppressed rules. Defaults to true.
     *
     * @return optional report-suppressed-rule flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getReportSuppressedRule();

    /**
     * SchXslt transpiler parameter {@code schxslt:report-skipped-assertion}.
     * When set to boolean true, the validation stylesheet reports assertions that are skipped.
     * Defaults to true.
     *
     * @return optional report-skipped-assertion flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getReportSkippedAssertion();

    /**
     * SchXslt transpiler parameter {@code schxslt:compact-report}.
     * When set to boolean true, the validation stylesheet only reports failed assertions, successful
     * reports and errors. Defaults to false.
     *
     * @return optional compact-report flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getCompactReport();

    /**
     * SchXslt transpiler parameter {@code schxslt:severity-threshold}.
     * Assertions with a severity lesser than the threshold are not checked. One of 'info', 'warning',
     * 'error', or 'fatal'. Defaults to 'info'.
     *
     * @return optional severity threshold
     */
    @Optional
    @Input
    public abstract Property<String> getSeverityThreshold();

    /**
     * SchXslt transpiler parameter {@code schxslt:default-severity}.
     * Severity of assertions without an {@code @severity} attribute. One of 'info', 'warning',
     * 'error', or 'fatal'. Defaults to 'fatal'.
     *
     * @return optional default severity
     */
    @Optional
    @Input
    public abstract Property<String> getDefaultSeverity();

    /**
     * SchXslt transpiler parameter {@code schxslt:default-from}.
     * Default value of the expression that selects the subset of the document to be validate.
     * Can be overwritten on a per-phase basis by the {@code @from} attribute. The default from
     * expression also applies to the phase '#ALL'. Defaults to 'root()'.
     *
     * @return optional default from expression
     */
    @Optional
    @Input
    public abstract Property<String> getDefaultFrom();

    /**
     * SchXslt transpiler parameter {@code schxslt:check-assembled-schema}.
     * When set to boolean true, the transpiler performs some plausability checks after all external
     * definitions are included. It terminates with an error if it finds errors in the assembled
     * schema. Defaults to false.
     *
     * @return optional check-assembled-schema flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getCheckAssembledSchema();

    /**
     * SchXslt transpiler parameter {@code schxslt:handle-dynamic-errors}.
     * When set to boolean true, the validation stylesheet writes dynamic errors as a
     * {@code svrl:error} element to the validation report and terminates with a
     * {@code schxslt:ValidationError} error. When set to false, dynamic errors bubble up to the
     * XSLT processor. Defaults to true.
     *
     * @return optional handle-dynamic-errors flag
     */
    @Optional
    @Input
    public abstract Property<Boolean> getHandleDynamicErrors();

    /**
     * Sets the Schematron schema file using Gradle file notation.
     *
     * @param path file notation accepted by {@code Project.file}
     */
    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Schematron schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    /**
     * Sets a custom transpiler stylesheet.
     *
     * @param path file notation accepted by {@code Project.file}
     */
    public void transpilerStylesheet(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Transpiler stylesheet does not exist: " + file);
        }
        getTranspilerStylesheet().set(file);
    }

    /**
     * Sets the compiled Schematron stylesheet output location.
     *
     * @param path file notation accepted by {@code Project.file}
     */
    public void style(Object path) {
        getStyle().set(getProject().file(path));
    }

    @Override
    protected ValidationResult validate(File inputFile, Map<String, String> params) throws Exception {
        Map<String, Object> transpilerParameters = collectTranspilerParameters();
        File compiledFile = resolveCompiledStylesheet(transpilerParameters);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(compiledFile));
        XsltTransformer validator = executable.load();
        validator.setSource(new StreamSource(inputFile));

        StringWriter output = new StringWriter();
        Serializer serializer = processor.newSerializer(output);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        if (getDebug().getOrElse(false)) {
            // Keep debug mode readable for generated SVRL output.
            serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        }
        validator.setDestination(serializer);
        validator.transform();

        String svrlXml = output.toString();
        List<name.jurgenei.gradle.xml.validation.ValidationIssue> issues = SvrlSupport.parseSvrlIssues(svrlXml);
        return new ValidationResult(issues, svrlXml);
    }


    private File resolveCompiledStylesheet(Map<String, Object> transpilerParameters) throws Exception {
        if (!getStyle().isPresent()) {
            File compiledFile = Files.createTempFile("schematron-compiled-", ".xsl").toFile();
            compiledFile.deleteOnExit();
            compileSchematron(compiledFile, transpilerParameters);
            return compiledFile;
        }

        File compiledFile = getStyle().get().getAsFile();
        synchronized (transpilerCompileLock) {
            if (needsRecompile(compiledFile, transpilerParameters)) {
                compileSchematron(compiledFile, transpilerParameters);
                writeFingerprint(fingerprintMarkerFor(compiledFile), computeTranspilerFingerprint(transpilerParameters));
            }
        }
        return compiledFile;
    }

    private boolean needsRecompile(File compiledFile, Map<String, Object> transpilerParameters) {
        if (!compiledFile.isFile()) {
            return true;
        }

        long compiledTimestamp = compiledFile.lastModified();
        long schemaTimestamp = getSchema().get().getAsFile().lastModified();
        if (compiledTimestamp < schemaTimestamp) {
            return true;
        }

        if (getTranspilerStylesheet().isPresent()) {
            long transpilerTimestamp = getTranspilerStylesheet().get().getAsFile().lastModified();
            if (compiledTimestamp < transpilerTimestamp) {
                return true;
            }
        }

        String currentFingerprint = computeTranspilerFingerprint(transpilerParameters);
        return !isFingerprintCurrent(fingerprintMarkerFor(compiledFile), currentFingerprint);
    }

    private void compileSchematron(File compiledFile, Map<String, Object> transpilerParameters) throws Exception {
        File parent = compiledFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        Source transpiler = loadTranspilerSource();
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        for (Map.Entry<String, Object> entry : transpilerParameters.entrySet()) {
            String localName = localNameFromClarkName(entry.getKey());
            if (STATIC_TRANSPILER_PARAMS.contains(localName)) {
                compiler.setParameter(qNameFromClarkName(entry.getKey()), toAtomicValue(entry.getValue()));
            }
        }

        XsltExecutable executable = compiler.compile(transpiler);
        XsltTransformer transpilerTransformer = executable.load();

        for (Map.Entry<String, Object> entry : transpilerParameters.entrySet()) {
            String localName = localNameFromClarkName(entry.getKey());
            if (!STATIC_TRANSPILER_PARAMS.contains(localName)) {
                transpilerTransformer.setParameter(qNameFromClarkName(entry.getKey()), toAtomicValue(entry.getValue()));
            }
        }

        transpilerTransformer.setSource(new StreamSource(getSchema().get().getAsFile()));
        Serializer serializer = processor.newSerializer(compiledFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transpilerTransformer.setDestination(serializer);
        transpilerTransformer.transform();
    }

    private static QName qNameFromClarkName(String clarkName) {
        int start = clarkName.indexOf('{');
        int end = clarkName.indexOf('}');
        if (start == 0 && end > 0 && end < clarkName.length() - 1) {
            String namespace = clarkName.substring(1, end);
            String localName = clarkName.substring(end + 1);
            return new QName(namespace, localName);
        }
        return new QName(clarkName);
    }

    private static String localNameFromClarkName(String clarkName) {
        int end = clarkName.indexOf('}');
        if (clarkName.startsWith("{") && end > 0 && end < clarkName.length() - 1) {
            return clarkName.substring(end + 1);
        }
        return clarkName;
    }

    private static XdmAtomicValue toAtomicValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return new XdmAtomicValue(booleanValue);
        }
        return new XdmAtomicValue(value == null ? "" : value.toString());
    }

    private File fingerprintMarkerFor(File compiledFile) {
        return new File(compiledFile.getParentFile(), compiledFile.getName() + ".transpiler.sha256");
    }

    private boolean isFingerprintCurrent(File markerFile, String currentFingerprint) {
        if (!markerFile.isFile()) {
            return false;
        }
        try {
            String recorded = Files.readString(markerFile.toPath(), StandardCharsets.UTF_8).trim();
            return currentFingerprint.equals(recorded);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void writeFingerprint(File markerFile, String fingerprint) throws Exception {
        Files.writeString(markerFile.toPath(), fingerprint, StandardCharsets.UTF_8);
    }

    private String computeTranspilerFingerprint(Map<String, Object> transpilerParameters) {
        StringBuilder builder = new StringBuilder();
        builder.append("schema=").append(getSchema().get().getAsFile().toPath().toAbsolutePath().normalize()).append('\n');
        builder.append("schemaTimestamp=").append(getSchema().get().getAsFile().lastModified()).append('\n');

        if (getTranspilerStylesheet().isPresent()) {
            File transpilerFile = getTranspilerStylesheet().get().getAsFile();
            builder.append("transpiler=").append(transpilerFile.toPath().toAbsolutePath().normalize()).append('\n');
            builder.append("transpilerTimestamp=").append(transpilerFile.lastModified()).append('\n');
        } else {
            builder.append("transpiler=classpath:content/transpile.xsl\n");
        }

        List<String> names = new ArrayList<>(transpilerParameters.keySet());
        Collections.sort(names);
        for (String name : names) {
            Object value = transpilerParameters.get(name);
            builder.append("transpilerParam:").append(name).append('=').append(value == null ? "" : value).append('\n');
        }

        return sha256(builder.toString());
    }

    private String sha256(String text) {
        final byte[] digest;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }

        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private Map<String, Object> collectTranspilerParameters() {
        Map<String, Object> transpilerParams = new LinkedHashMap<>();

        if (getDebug().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}debug", getDebug().get());
        }
        if (getPhase().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}phase", getPhase().get());
        }
        if (getExpandText().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}expand-text", getExpandText().get());
        }
        if (getStreamable().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}streamable", getStreamable().get());
        }
        if (getLocationFunction().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}location-function", getLocationFunction().get());
        }
        if (getFailEarly().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}fail-early", getFailEarly().get());
        }
        if (getTerminateValidationOnError().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}terminate-validation-on-error", getTerminateValidationOnError().get());
        }
        if (getReportActivePattern().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}report-active-pattern", getReportActivePattern().get());
        }
        if (getReportFiredRule().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}report-fired-rule", getReportFiredRule().get());
        }
        if (getReportSuppressedRule().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}report-suppressed-rule", getReportSuppressedRule().get());
        }
        if (getReportSkippedAssertion().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}report-skipped-assertion", getReportSkippedAssertion().get());
        }
        if (getCompactReport().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}compact-report", getCompactReport().get());
        }
        if (getSeverityThreshold().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}severity-threshold", getSeverityThreshold().get());
        }
        if (getDefaultSeverity().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}default-severity", getDefaultSeverity().get());
        }
        if (getDefaultFrom().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}default-from", getDefaultFrom().get());
        }
        if (getCheckAssembledSchema().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}check-assembled-schema", getCheckAssembledSchema().get());
        }
        if (getHandleDynamicErrors().isPresent()) {
            transpilerParams.put("{" + SCHXSLT_NS + "}handle-dynamic-errors", getHandleDynamicErrors().get());
        }

        return transpilerParams;
    }

    private Source loadTranspilerSource() throws Exception {
        if (getTranspilerStylesheet().isPresent()) {
            return new StreamSource(getTranspilerStylesheet().get().getAsFile());
        }

        try (java.io.InputStream stream = getClass().getClassLoader().getResourceAsStream("content/transpile.xsl")) {
            if (stream == null) {
                throw new GradleException("Could not find SchXslt transpiler at classpath resource content/transpile.xsl");
            }
            String xsl = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new StreamSource(new StringReader(xsl));
        }
    }
}

