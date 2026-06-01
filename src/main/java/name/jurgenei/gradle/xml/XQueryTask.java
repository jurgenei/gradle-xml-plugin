package name.jurgenei.gradle.xml;

import java.io.File;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import org.gradle.work.DisableCachingByDefault;

/**
 * Transforms XML input files using a Saxon XQuery script.
 *
 * <p>Task behavior (sources, output mapping, parameters, and concurrency) is inherited
 * from {@link AbstractXmlTransformTask}.</p>
 */
@DisableCachingByDefault(because = "XQuery transformations depend on external query files and input XML")
public abstract class XQueryTask extends AbstractXmlTransformTask {

    /**
     * Creates an XQuery transformation task.
     */
    public XQueryTask() {
    }

    /**
     * XQuery file used to compile the transformation.
     *
     * @return query file property
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getQueryFile();

    @Override
    protected void transform(File inputFile, File outputFile, Map<String, String> params) throws Exception {
        Processor processor = new Processor(false);
        XQueryCompiler compiler = processor.newXQueryCompiler();

        XQueryExecutable executable = compiler.compile(getQueryFile().get().getAsFile());
        XQueryEvaluator evaluator = executable.load();

        XdmNode context = processor.newDocumentBuilder().build(new StreamSource(inputFile));
        evaluator.setContextItem(context);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            evaluator.setExternalVariable(new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }

        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        evaluator.run(serializer);
    }

    @Override
    protected long latestDependencyTimestamp(File inputFile) {
        long sourceTimestamp = inputFile.lastModified();
        long queryTimestamp = getQueryFile().get().getAsFile().lastModified();
        return Math.max(sourceTimestamp, queryTimestamp);
    }

    /**
     * Sets the XQuery file in Gradle DSL friendly form.
     *
     * @param path file notation supported by {@code Project.file}
     */
    public void query(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("XQuery file does not exist: " + file);
        }
        getQueryFile().set(file);
    }
}



