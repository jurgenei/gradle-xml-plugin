package name.jurgenei.gradle.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import name.jurgenei.gradle.xml.validation.ValidationIssue;
import name.jurgenei.gradle.xml.validation.ValidationResult;
import name.jurgenei.gradle.xml.validation.XsdEngine;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SchemaManager;
import net.sf.saxon.s9api.SchemaValidator;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Validates XML files against XSD and emits findings normalized as SVRL.
 */
public abstract class XsdTask extends AbstractXmlValidationTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    @Input
    public abstract Property<XsdEngine> getEngine();

    public XsdTask() {
        getEngine().convention(XsdEngine.AUTO);
    }

    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("XSD schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    @Override
    protected ValidationResult validate(File inputFile, Map<String, String> params) throws Exception {
        XsdEngine engine = resolveEngine();
        List<ValidationIssue> issues = engine == XsdEngine.SAXON
            ? validateWithSaxon(inputFile)
            : validateWithJaxp(inputFile);

        return new ValidationResult(issues, null);
    }

    private XsdEngine resolveEngine() {
        XsdEngine requested = getEngine().get();
        if (requested == XsdEngine.JAXP) {
            return XsdEngine.JAXP;
        }
        if (requested == XsdEngine.SAXON) {
            if (!isSaxonSchemaAwareAvailable()) {
                throw new GradleException("Saxon schema-aware validation requires Saxon PE/EE; configure engine = JAXP or AUTO");
            }
            return XsdEngine.SAXON;
        }
        return isSaxonSchemaAwareAvailable() ? XsdEngine.SAXON : XsdEngine.JAXP;
    }

    private boolean isSaxonSchemaAwareAvailable() {
        try {
            Processor processor = new Processor(false);
            SchemaManager manager = processor.getSchemaManager();
            return manager != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private List<ValidationIssue> validateWithSaxon(File inputFile) {
        List<ValidationIssue> issues = new ArrayList<>();
        try {
            Processor processor = new Processor(false);
            SchemaManager manager = processor.getSchemaManager();
            if (manager == null) {
                throw new GradleException("Saxon SchemaManager unavailable in current runtime");
            }
            manager.load(new StreamSource(getSchema().get().getAsFile()));
            SchemaValidator validator = manager.newSchemaValidator();
            validator.validate(new StreamSource(inputFile));
        } catch (SaxonApiException e) {
            issues.add(ValidationIssue.error(e.getMessage(), inputFile.getPath()));
        }
        return issues;
    }

    private List<ValidationIssue> validateWithJaxp(File inputFile) throws Exception {
        List<ValidationIssue> issues = new ArrayList<>();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        javax.xml.validation.Schema schema = factory.newSchema(getSchema().get().getAsFile());
        javax.xml.validation.Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                issues.add(ValidationIssue.error(exception.getMessage(), location(inputFile, exception)));
            }

            @Override
            public void error(SAXParseException exception) {
                issues.add(ValidationIssue.error(exception.getMessage(), location(inputFile, exception)));
            }

            @Override
            public void fatalError(SAXParseException exception) {
                issues.add(ValidationIssue.error(exception.getMessage(), location(inputFile, exception)));
            }
        });

        try {
            validator.validate(new StreamSource(inputFile));
        } catch (Exception ignored) {
            // Findings are collected through ErrorHandler so we can continue and emit reports.
        }
        return issues;
    }

    private static String location(File input, SAXParseException exception) {
        return input + ":" + exception.getLineNumber() + ":" + exception.getColumnNumber();
    }
}

