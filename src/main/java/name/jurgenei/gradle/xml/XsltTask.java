package name.jurgenei.gradle.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import name.jurgenei.gradle.xml.sexpr.SExpressionSerializer;
import name.jurgenei.gradle.xml.sexpr.SExpressionXmlReader;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.xml.sax.InputSource;

import org.gradle.work.DisableCachingByDefault;

/**
 * Transforms XML input files using a Saxon XSLT stylesheet.
 *
 * <p>Task behavior (sources, output mapping, parameters, and concurrency) is inherited
 * from {@link AbstractXmlTransformTask}.</p>
 */
@DisableCachingByDefault(because = "XSLT transformations depend on external stylesheet and input files")
public abstract class XsltTask extends AbstractXmlTransformTask {

    /**
     * Creates an XSLT transformation task.
     */
    public XsltTask() {
    }

    /**
     * Stylesheet file used to compile the XSLT transformation.
     *
     * @return stylesheet file property
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getStylesheet();

    @Override
    protected void transform(File inputFile, File outputFile, Map<String, String> params) throws SaxonApiException {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        XsltExecutable executable = compiler.compile(new StreamSource(getStylesheet().get().getAsFile()));
        XsltTransformer transformer = executable.load();
        transformer.setSource(sourceFor(inputFile));

        for (Map.Entry<String, String> entry : params.entrySet()) {
            transformer.setParameter(new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }

        if (isSexprFile(outputFile)) {
            try {
                if (outputFile.getParentFile() != null) {
                    Files.createDirectories(outputFile.getParentFile().toPath());
                }
                try (java.io.Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
                    Destination destination = new SAXDestination(new SExpressionSerializer(writer, resolveSexprOutputFormat()));
                    transformer.setDestination(destination);
                    transformer.transform();
                }
            } catch (Exception e) {
                throw new SaxonApiException("Failed to write S-expression output", e);
            }
            return;
        }

        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, resolveSerializerMethod(outputFile));
        transformer.setDestination(serializer);
        transformer.transform();
    }

    private Source sourceFor(File inputFile) {
        if (isSexprFile(inputFile)) {
            return new SAXSource(new SExpressionXmlReader(), new InputSource(inputFile.toURI().toString()));
        }
        return new StreamSource(inputFile);
    }

    private boolean isSexprFile(File file) {
        return file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".sexpr");
    }

    @Override
    protected long latestDependencyTimestamp(File inputFile) {
        long sourceTimestamp = inputFile.lastModified();
        long stylesheetTimestamp = getStylesheet().get().getAsFile().lastModified();
        return Math.max(sourceTimestamp, stylesheetTimestamp);
    }

    /**
     * Sets the XSLT stylesheet in Gradle DSL friendly form.
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void style(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Stylesheet file does not exist: " + file);
        }
        getStylesheet().set(file);
    }
}


