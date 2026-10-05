/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Compiles SHACL relation shapes into an executable SEL extraction stylesheet.
 *
 * <p>Input must be RDF/XML SHACL for deterministic namespace-aware parsing.
 * Generated stylesheet follows same grouped SEL structure as Schematron SEL compile flow.</p>
 */
@DisableCachingByDefault(
        because = "Compiler output depends on SHACL schema content and grouped output mapping")
public abstract class ShaclSelCompileTask extends DefaultTask {
    private static final String SH_NS = "http://www.w3.org/ns/shacl#";
    private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String SCH_NS = "http://purl.oclc.org/dsdl/schematron";
    private static final String SEL_NS = "http://jurgenei.name/sel";

    /**
     * Returns SHACL RDF/XML schema source.
     *
     * @return schema file property
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    /**
     * Returns generated SEL stylesheet output target.
     *
     * @return stylesheet output file property
     */
    @OutputFile
    public abstract RegularFileProperty getOutputStylesheet();

    /**
     * Returns generated intermediate Schematron output target.
     *
     * @return Schematron output file property
     */
    @OutputFile
    public abstract RegularFileProperty getOutputSchematron();

    /**
     * Returns logical group to output-path mapping.
     *
     * @return configured group outputs
     */
    @Input
    public abstract MapProperty<String, String> getGroupOutputs();

    /**
     * Returns optional override for SEL namespace URI.
     *
     * @return namespace URI property
     */
    @Input
    @Optional
    public abstract Property<String> getOutputNamespaceUri();

    /**
     * Returns optional override for SEL namespace prefix.
     *
     * @return namespace prefix property
     */
    @Input
    @Optional
    public abstract Property<String> getOutputNamespacePrefix();

    /**
     * Creates compile task with default generated Schematron location.
     */
    @Inject
    public ShaclSelCompileTask() {
        getOutputSchematron()
                .convention(
                        getProject()
                                .getLayout()
                                .getBuildDirectory()
                                .file("generated/shacl-sel/compiled.sch"));
    }

    /**
     * Sets SHACL schema source path.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("SHACL schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    /**
     * Sets stylesheet output path.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void output(Object path) {
        getOutputStylesheet().set(getProject().file(path));
    }

    /**
     * Sets generated Schematron output path.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void outputSchematron(Object path) {
        getOutputSchematron().set(getProject().file(path));
    }

    /**
     * Configures output mapping for logical group.
     *
     * @param group group key
     * @param outputPath output file path relative to extraction stem directory
     */
    public void groupOutput(String group, String outputPath) {
        getGroupOutputs().put(group, outputPath);
    }

    /**
     * Sets SEL output namespace URI override.
     *
     * @param value namespace URI
     */
    public void outputNamespaceUri(String value) {
        getOutputNamespaceUri().set(value);
    }

    /**
     * Sets SEL output namespace prefix override.
     *
     * @param value namespace prefix; empty string means default namespace output
     */
    public void outputNamespacePrefix(String value) {
        getOutputNamespacePrefix().set(value);
    }

    /**
     * Compiles SHACL relation rules into SEL extraction stylesheet and generated Schematron.
     */
    @TaskAction
    public void compile() {
        try {
            Document shacl = parseXml(getSchema().get().getAsFile());
            List<ShaclRule> rules = collectRules(shacl);
            String schematron = renderSchematron(rules);

            File schematronFile = getOutputSchematron().get().getAsFile();
            File schematronParent = schematronFile.getParentFile();
            if (schematronParent != null) {
                Files.createDirectories(schematronParent.toPath());
            }
            Files.writeString(schematronFile.toPath(), schematron, StandardCharsets.UTF_8);

            Document schematronDoc = parseXml(schematronFile);
            SelRuleSet collected = SelRuleCollector.collect(schematronDoc);
            SelOutputConfig outputConfig =
                    SelOutputConfig.resolve(
                            collected.outputConfig(),
                            getOutputNamespaceUri().isPresent()
                                    ? getOutputNamespaceUri().get()
                                    : null,
                            getOutputNamespacePrefix().isPresent()
                                    ? getOutputNamespacePrefix().get()
                                    : null);
            String stylesheet =
                    SelStylesheetCompiler.render(
                            collected.rules(), getGroupOutputs().getOrElse(Map.of()), outputConfig);

            File stylesheetFile = getOutputStylesheet().get().getAsFile();
            File stylesheetParent = stylesheetFile.getParentFile();
            if (stylesheetParent != null) {
                Files.createDirectories(stylesheetParent.toPath());
            }
            Files.writeString(stylesheetFile.toPath(), stylesheet, StandardCharsets.UTF_8);
            getLogger()
                    .lifecycle(
                            "Compiled SHACL SEL extraction stylesheet with {} rule(s): {}",
                            collected.rules().size(),
                            stylesheetFile);
        } catch (Exception e) {
            throw new GradleException("Failed to compile SHACL SEL stylesheet", e);
        }
    }

    private Document parseXml(File file) throws Exception {
        try (InputStream input = Files.newInputStream(file.toPath())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private List<ShaclRule> collectRules(Document shacl) {
        List<ShaclRule> rules = new ArrayList<>();
        NodeList nodeShapes = shacl.getElementsByTagNameNS(SH_NS, "NodeShape");
        for (int i = 0; i < nodeShapes.getLength(); i++) {
            Element nodeShape = (Element) nodeShapes.item(i);
            String shapeRef = attribute(nodeShape, RDF_NS, "about");
            String sourceType =
                    localNameFromIri(firstChildResource(nodeShape, SH_NS, "targetClass"));
            if (sourceType.isBlank()) {
                continue;
            }

            for (Element propertyShape : propertyShapes(nodeShape)) {
                String path = localNameFromIri(firstChildResource(propertyShape, SH_NS, "path"));
                String targetType =
                        localNameFromIri(firstChildResource(propertyShape, SH_NS, "class"));
                if (path.isBlank() || targetType.isBlank()) {
                    continue;
                }
                rules.add(
                        new ShaclRule(
                                sourceType,
                                path,
                                targetType,
                                shapeRef.isBlank() ? sourceType + "Shape" : shapeRef));
            }
        }
        return rules;
    }

    private List<Element> propertyShapes(Element nodeShape) {
        List<Element> result = new ArrayList<>();
        NodeList propertyNodes = nodeShape.getElementsByTagNameNS(SH_NS, "property");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Node propertyNode = propertyNodes.item(i);
            if (!(propertyNode instanceof Element propertyElement)) {
                continue;
            }
            Element directPropertyShape =
                    firstChildElement(propertyElement, SH_NS, "PropertyShape");
            if (directPropertyShape != null) {
                result.add(directPropertyShape);
                continue;
            }
            // Fallback for compact RDF/XML where sh:path etc. are nested directly.
            result.add(propertyElement);
        }
        return result;
    }

    private String renderSchematron(List<ShaclRule> rules) {
        try {
            SaxXmlWriter xml = SaxXmlWriter.createPretty();
            xml.startDocument();

            xml.startElement(
                    SCH_NS,
                    "schema",
                    "sch:schema",
                    List.of(),
                    Map.of("sch", SCH_NS, "sel", SEL_NS));
            xml.startElement(
                    SCH_NS,
                    "pattern",
                    "sch:pattern",
                    List.of(SaxXmlWriter.attr("id", "shacl-relations")),
                    Map.of());

            if (rules.isEmpty()) {
                xml.startElement(
                        SCH_NS,
                        "rule",
                        "sch:rule",
                        List.of(SaxXmlWriter.attr("context", "*")),
                        Map.of());
                xml.startElement(
                        SCH_NS,
                        "assert",
                        "sch:assert",
                        List.of(SaxXmlWriter.attr("test", "true()")),
                        Map.of());
                xml.text("No SHACL relation rules collected");
                xml.endElement(SCH_NS, "assert", "sch:assert");
                xml.endElement(SCH_NS, "rule", "sch:rule");
            } else {
                for (ShaclRule rule : rules) {
                    String relationPath = "*[local-name()='" + rule.path() + "']";
                    xml.startElement(
                            SCH_NS,
                            "rule",
                            "sch:rule",
                            List.of(
                                    SaxXmlWriter.attr(
                                            "context",
                                            "*[local-name()='" + rule.sourceType() + "']")),
                            Map.of());
                    xml.startElement(
                            SCH_NS,
                            "report",
                            "sch:report",
                            List.of(
                                    SaxXmlWriter.attr("test", relationPath),
                                    SaxXmlWriter.attr(SEL_NS, "type", "sel:type", rule.path()),
                                    SaxXmlWriter.attr(SEL_NS, "group", "sel:group", "relations"),
                                    SaxXmlWriter.attr(SEL_NS, "copy", "sel:copy", "."),
                                    SaxXmlWriter.attr(
                                            SEL_NS, "context", "sel:context", relationPath)),
                            Map.of());
                    xml.text("SHACL relation " + rule.sourceType() + " -> " + rule.targetType());
                    xml.endElement(SCH_NS, "report", "sch:report");
                    xml.endElement(SCH_NS, "rule", "sch:rule");
                }
            }

            xml.endElement(SCH_NS, "pattern", "sch:pattern");
            xml.endElement(SCH_NS, "schema", "sch:schema");
            xml.endDocument();
            return xml.build();
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to render SHACL Schematron", e);
        }
    }

    private Element firstChildElement(Element parent, String namespaceUri, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element
                    && namespaceUri.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private String firstChildResource(Element parent, String namespaceUri, String localName) {
        Element child = firstChildElement(parent, namespaceUri, localName);
        if (child == null) {
            return "";
        }
        String resource = attribute(child, RDF_NS, "resource");
        if (!resource.isBlank()) {
            return resource;
        }
        return child.getTextContent() == null ? "" : child.getTextContent().trim();
    }

    private String localNameFromIri(String iri) {
        if (iri == null || iri.isBlank()) {
            return "";
        }
        String trimmed = iri.trim();
        int hash = trimmed.lastIndexOf('#');
        if (hash >= 0 && hash + 1 < trimmed.length()) {
            return trimmed.substring(hash + 1);
        }
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < trimmed.length()) {
            return trimmed.substring(slash + 1);
        }
        int colon = trimmed.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < trimmed.length()) {
            return trimmed.substring(colon + 1);
        }
        return trimmed;
    }

    private String attribute(Element element, String namespaceUri, String localName) {
        String value = element.getAttributeNS(namespaceUri, localName);
        return value.trim();
    }

    private record ShaclRule(String sourceType, String path, String targetType, String shapeRef) {}
}
