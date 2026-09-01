package name.jurgenei.gradle.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import name.jurgenei.gradle.xml.json.JsonCanonicalSerializer;
import name.jurgenei.gradle.xml.json.JsonCanonicalXmlReader;
import name.jurgenei.gradle.xml.sexpr.SExpressionSerializer;
import name.jurgenei.gradle.xml.sexpr.SExpressionXmlReader;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
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
import org.xml.sax.InputSource;

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

        if (isSexprFile(outputFile)) {
            if (outputFile.getParentFile() != null) {
                Files.createDirectories(outputFile.getParentFile().toPath());
            }
            try (java.io.Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
                XQueryEvaluator evaluator = createEvaluator(executable, processor, inputFile, params);
                Destination destination = new SAXDestination(new SExpressionSerializer(writer, resolveSexprOutputFormat()));
                evaluator.run(destination);
            }
            return;
        }

        if (shouldAttemptCanonicalJsonOutput(outputFile)) {
            boolean strict = isStrictCanonicalJsonOutput(outputFile);
            if (outputFile.getParentFile() != null) {
                Files.createDirectories(outputFile.getParentFile().toPath());
            }
            try (java.io.Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
                XQueryEvaluator evaluator = createEvaluator(executable, processor, inputFile, params);
                Destination destination = new SAXDestination(new JsonCanonicalSerializer(writer, resolveSexprOutputFormat()));
                evaluator.run(destination);
                return;
            } catch (Exception e) {
                if (strict) {
                    throw e;
                }
            }
        }

        XQueryEvaluator evaluator = createEvaluator(executable, processor, inputFile, params);
        Serializer serializer = processor.newSerializer(outputFile);
        serializer.setOutputProperty(Serializer.Property.METHOD, resolveSerializerMethod(outputFile));
        evaluator.run(serializer);
    }

    private XQueryEvaluator createEvaluator(XQueryExecutable executable, Processor processor, File inputFile, Map<String, String> params) throws Exception {
        XQueryEvaluator evaluator = executable.load();
        XdmNode context = processor.newDocumentBuilder().build(sourceFor(inputFile));
        evaluator.setContextItem(context);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            evaluator.setExternalVariable(new QName(entry.getKey()), new XdmAtomicValue(entry.getValue()));
        }
        return evaluator;
    }

    private Source sourceFor(File inputFile) {
        if (isSexprFile(inputFile)) {
            return new SAXSource(new SExpressionXmlReader(), new InputSource(inputFile.toURI().toString()));
        }
        if (useCanonicalJsonInput(inputFile)) {
            return new SAXSource(new JsonCanonicalXmlReader(), new InputSource(inputFile.toURI().toString()));
        }
        return new StreamSource(inputFile);
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



