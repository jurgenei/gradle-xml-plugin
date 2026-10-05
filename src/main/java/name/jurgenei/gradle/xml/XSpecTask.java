/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import name.jurgenei.gradle.xml.saxon.SaxonXirResolvers;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Executes XSpec runners and writes XML + JUnit reports.
 */
@DisableCachingByDefault(
        because = "Execution depends on external XSpec files and runtime stylesheet resources")
public abstract class XSpecTask extends AbstractXmlTransformTask {

    private static final String XSPEC_NS = "http://www.jenitennison.com/xslt/xspec";
    private static final QName XSPEC_MAIN_TEMPLATE = new QName(XSPEC_NS, "main");
    private static final String DEFAULT_COMPILER_CLASSPATH_RESOURCE =
            "io/xspec/xspec/impl/src/compiler/compile-xslt-tests.xsl";
    private static final String DEFAULT_JUNIT_REPORTER_CLASSPATH_RESOURCE =
            "io/xspec/xspec/impl/src/reporter/junit-report.xsl";

    /**
     * Creates task with XSpec report defaults.
     */
    public XSpecTask() {
        getOutputExtension().convention(".xspec.xml");
        getJunitOutputDir()
                .convention(
                        getProject().getLayout().getBuildDirectory().dir("reports/xspec/junit"));
    }

    /**
     * Optional override for XSpec compiler stylesheet used when input is .xspec.
     *
     * @return compiler stylesheet property
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getCompilerStylesheet();

    /**
     * Optional override for JUnit reporter stylesheet.
     *
     * @return reporter stylesheet property
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getJunitReporterStylesheet();

    /**
     * Destination directory for generated JUnit XML reports.
     *
     * @return JUnit output directory property
     */
    @OutputDirectory
    public abstract DirectoryProperty getJunitOutputDir();

    /**
     * Optional suite name exposed to future report customizations.
     *
     * @return suite name property
     */
    @Optional
    @Input
    public abstract org.gradle.api.provider.Property<String> getJunitSuiteName();

    /**
     * Sets custom compiler stylesheet in Gradle DSL-friendly form.
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void compilerStylesheet(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("XSpec compiler stylesheet does not exist: " + file);
        }
        getCompilerStylesheet().set(file);
    }

    /**
     * Sets custom XSpec JUnit reporter stylesheet.
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void junitReporterStylesheet(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("XSpec JUnit reporter stylesheet does not exist: " + file);
        }
        getJunitReporterStylesheet().set(file);
    }

    @Override
    protected void transform(File inputFile, File outputFile, Map<String, String> params)
            throws Exception {
        File runnerStylesheet = inputFile;
        File autoCompiledRunner = null;
        if (isXspec(inputFile)) {
            autoCompiledRunner = Files.createTempFile("xspec-runner-", ".xsl").toFile();
            autoCompiledRunner.deleteOnExit();
            compileXspec(inputFile, autoCompiledRunner);
            runnerStylesheet = autoCompiledRunner;
        }

        Processor processor = new Processor(false);
        SaxonXirResolvers.configure(processor);
        XsltCompiler compiler = processor.newXsltCompiler();
        SaxonXirResolvers.configure(compiler);
        XsltExecutable executable = compiler.compile(new StreamSource(runnerStylesheet));
        XsltTransformer transformer = executable.load();
        transformer.setInitialTemplate(XSPEC_MAIN_TEMPLATE);
        transformer.setBaseOutputURI(outputFile.toURI().toString());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            transformer.setParameter(
                    new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }

        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();

        File junitFile = junitFileFor(outputFile);
        File junitParent = junitFile.getParentFile();
        if (junitParent != null) {
            Files.createDirectories(junitParent.toPath());
        }
        writeJunitReport(outputFile, junitFile, processor);

        FailedTestSummary failedTestSummary = summarizeFailures(outputFile);
        if (failedTestSummary.failures() > 0) {
            throw new GradleException(
                    "XSpec failed for "
                            + inputFile
                            + " with "
                            + failedTestSummary.failures()
                            + " failing test(s) out of "
                            + failedTestSummary.total()
                            + ".");
        }
    }

    @Override
    protected long latestDependencyTimestamp(File inputFile) {
        long sourceTimestamp = inputFile.lastModified();
        long junitReporterTimestamp =
                getJunitReporterStylesheet().isPresent()
                        ? getJunitReporterStylesheet().get().getAsFile().lastModified()
                        : sourceTimestamp;

        if (isXspec(inputFile)) {
            long compilerTimestamp =
                    getCompilerStylesheet().isPresent()
                            ? getCompilerStylesheet().get().getAsFile().lastModified()
                            : sourceTimestamp;
            return Math.max(sourceTimestamp, Math.max(compilerTimestamp, junitReporterTimestamp));
        }

        return Math.max(sourceTimestamp, junitReporterTimestamp);
    }

    private void compileXspec(File xspecFile, File outputStylesheet) throws Exception {
        Processor processor = new Processor(false);
        SaxonXirResolvers.configure(processor);
        XsltCompiler compiler = processor.newXsltCompiler();
        SaxonXirResolvers.configure(compiler);
        XsltExecutable executable = compiler.compile(compilerSource());
        XsltTransformer transformer = executable.load();
        transformer.setSource(new StreamSource(xspecFile));
        Serializer serializer = processor.newSerializer(outputStylesheet);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();
    }

    private Source compilerSource() {
        if (getCompilerStylesheet().isPresent()) {
            return new StreamSource(getCompilerStylesheet().get().getAsFile());
        }
        URL resource = getClass().getClassLoader().getResource(DEFAULT_COMPILER_CLASSPATH_RESOURCE);
        if (resource == null) {
            throw new GradleException(
                    "Could not find XSpec compiler at classpath resource "
                            + DEFAULT_COMPILER_CLASSPATH_RESOURCE);
        }
        return new StreamSource(resource.toExternalForm());
    }

    private Source junitReporterSource() {
        if (getJunitReporterStylesheet().isPresent()) {
            return new StreamSource(getJunitReporterStylesheet().get().getAsFile());
        }
        URL resource =
                getClass().getClassLoader().getResource(DEFAULT_JUNIT_REPORTER_CLASSPATH_RESOURCE);
        if (resource == null) {
            throw new GradleException(
                    "Could not find XSpec JUnit reporter at classpath resource "
                            + DEFAULT_JUNIT_REPORTER_CLASSPATH_RESOURCE);
        }
        return new StreamSource(resource.toExternalForm());
    }

    private void writeJunitReport(File xspecReportFile, File junitOutputFile, Processor processor)
            throws Exception {
        XsltCompiler compiler = processor.newXsltCompiler();
        SaxonXirResolvers.configure(compiler);
        XsltExecutable executable = compiler.compile(junitReporterSource());
        XsltTransformer transformer = executable.load();
        transformer.setSource(new StreamSource(xspecReportFile));
        Serializer serializer = processor.newSerializer(junitOutputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();
    }

    private File junitFileFor(File outputFile) {
        String relative = outputFile.getName();
        if (getOutputDir().isPresent()) {
            File outputRoot = getOutputDir().get().getAsFile();
            java.nio.file.Path outputPath = outputFile.toPath().toAbsolutePath().normalize();
            java.nio.file.Path rootPath = outputRoot.toPath().toAbsolutePath().normalize();
            if (outputPath.startsWith(rootPath)) {
                relative = rootPath.relativize(outputPath).toString();
            }
        }
        int extensionIndex = relative.lastIndexOf('.');
        String replaced =
                extensionIndex >= 0
                        ? relative.substring(0, extensionIndex) + ".junit.xml"
                        : relative + ".junit.xml";
        File junitRoot = getJunitOutputDir().get().getAsFile();
        java.nio.file.Path targetPath = junitRoot.toPath().resolve(replaced).normalize();
        java.nio.file.Path junitRootPath = junitRoot.toPath().toAbsolutePath().normalize();
        java.nio.file.Path absoluteTarget = targetPath.toAbsolutePath().normalize();
        if (!absoluteTarget.startsWith(junitRootPath)) {
            throw new GradleException(
                    "Resolved JUnit report path escapes output directory: " + replaced);
        }
        return absoluteTarget.toFile();
    }

    private FailedTestSummary summarizeFailures(File xmlReportFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(xmlReportFile);
        NodeList tests = document.getElementsByTagNameNS(XSPEC_NS, "test");
        int total = tests.getLength();
        int failed = 0;
        for (int i = 0; i < total; i++) {
            org.w3c.dom.Node node = tests.item(i);
            org.w3c.dom.Node successful =
                    node.getAttributes() == null
                            ? null
                            : node.getAttributes().getNamedItem("successful");
            if (successful != null && !Boolean.parseBoolean(successful.getNodeValue())) {
                failed++;
            }
        }
        return new FailedTestSummary(total, failed);
    }

    private boolean isXspec(File file) {
        return file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xspec");
    }

    private record FailedTestSummary(int total, int failures) {}
}
