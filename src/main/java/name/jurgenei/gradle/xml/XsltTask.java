package name.jurgenei.gradle.xml;

import java.io.File;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
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

/**
 * Transforms XML input files using a Saxon XSLT stylesheet.
 *
 * <p>Task behavior (sources, output mapping, parameters, and concurrency) is inherited
 * from {@link AbstractXmlTransformTask}.</p>
 */
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
        transformer.setSource(new StreamSource(inputFile));

        for (Map.Entry<String, String> entry : params.entrySet()) {
            transformer.setParameter(new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }

        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();
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


