package name.jurgenei.gradle.xml;

import java.io.File;
import java.net.URL;
import java.util.Map;
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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

/**
 * Compiles XSLT XSpec descriptions into executable runner stylesheets.
 */
@DisableCachingByDefault(because = "Compilation depends on external XSpec descriptions and compiler resources")
public abstract class XSpecCompileTask extends AbstractXmlTransformTask {

    private static final String DEFAULT_COMPILER_CLASSPATH_RESOURCE = "io/xspec/xspec/impl/src/compiler/compile-xslt-tests.xsl";

    /**
     * Creates task with XSpec runner extension defaults.
     */
    public XSpecCompileTask() {
        getOutputExtension().convention(".xspec.xsl");
    }

    /**
     * Optional override for XSpec compiler stylesheet.
     *
     * @return compiler stylesheet property
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getCompilerStylesheet();

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

    @Override
    public void input(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("XSpec input file does not exist: " + file);
        }
        super.input(path);
    }

    @Override
    protected void transform(File inputFile, File outputFile, Map<String, String> params) throws Exception {
        Processor processor = new Processor(false);
        SaxonXirResolvers.configure(processor);
        XsltCompiler compiler = processor.newXsltCompiler();
        SaxonXirResolvers.configure(compiler);

        XsltExecutable executable = compiler.compile(compilerSource());
        XsltTransformer transformer = executable.load();
        transformer.setSource(new StreamSource(inputFile));
        for (Map.Entry<String, String> entry : params.entrySet()) {
            transformer.setParameter(new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }

        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();
    }

    @Override
    protected long latestDependencyTimestamp(File inputFile) {
        long sourceTimestamp = inputFile.lastModified();
        if (!getCompilerStylesheet().isPresent()) {
            return sourceTimestamp;
        }
        long compilerTimestamp = getCompilerStylesheet().get().getAsFile().lastModified();
        return Math.max(sourceTimestamp, compilerTimestamp);
    }

    private Source compilerSource() {
        if (getCompilerStylesheet().isPresent()) {
            return new StreamSource(getCompilerStylesheet().get().getAsFile());
        }
        URL resource = getClass().getClassLoader().getResource(DEFAULT_COMPILER_CLASSPATH_RESOURCE);
        if (resource == null) {
            throw new GradleException("Could not find XSpec compiler at classpath resource " + DEFAULT_COMPILER_CLASSPATH_RESOURCE);
        }
        return new StreamSource(resource.toExternalForm());
    }
}
