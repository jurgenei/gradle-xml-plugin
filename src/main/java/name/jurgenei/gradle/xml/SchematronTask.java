package name.jurgenei.gradle.xml;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import name.jurgenei.gradle.xml.validation.SvrlSupport;
import name.jurgenei.gradle.xml.validation.ValidationResult;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/**
 * Validates XML files via Schematron by compiling .sch to XSLT and producing SVRL output.
 */
public abstract class SchematronTask extends AbstractXmlValidationTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getTranspilerStylesheet();

    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Schematron schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    public void transpilerStylesheet(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Transpiler stylesheet does not exist: " + file);
        }
        getTranspilerStylesheet().set(file);
    }

    @Override
    protected ValidationResult validate(File inputFile, Map<String, String> params) throws Exception {
        File compiledFile = Files.createTempFile("schematron-compiled-", ".xsl").toFile();
        compiledFile.deleteOnExit();

        Source transpiler = loadTranspilerSource();

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.newTransformer(transpiler).transform(
            new StreamSource(getSchema().get().getAsFile()),
            new StreamResult(compiledFile));

        StringWriter output = new StringWriter();
        tf.newTransformer(new StreamSource(compiledFile)).transform(
            new StreamSource(inputFile),
            new StreamResult(output));

        String svrlXml = output.toString();
        List<name.jurgenei.gradle.xml.validation.ValidationIssue> issues = SvrlSupport.parseSvrlIssues(svrlXml);
        return new ValidationResult(issues, svrlXml);
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

