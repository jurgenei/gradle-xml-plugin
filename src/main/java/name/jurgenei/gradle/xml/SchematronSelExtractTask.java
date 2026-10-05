/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import name.jurgenei.gradle.xml.json.JsonCanonicalXmlReader;
import name.jurgenei.gradle.xml.saxon.SaxonXirResolvers;
import name.jurgenei.xir.XirReader;
import name.jurgenei.xir.XirSerializer;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Phase-3 runtime task that executes Schematron SEL extraction.
 *
 * <p>The task can consume a precompiled extraction stylesheet or compile one on the fly
 * from annotation-bearing Schematron rules.</p>
 */
@DisableCachingByDefault(
        because = "Extraction output fan-out depends on source trees and dynamic grouped mappings")
public abstract class SchematronSelExtractTask extends org.gradle.api.DefaultTask {
    private static final Set<String> SUPPORTED_SEXPR_FORMATS = Set.of("compact", "beautified");
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    /**
     * JSON routing mode for .json SEL input files.
     */
    public enum JsonMode {
        /** Detect mode from content and extension heuristics. */
        AUTO,
        /** Read JSON directly as XDM maps/arrays (no canonical XML conversion). */
        NATIVE,
        /** Convert JSON into canonical XML representation before extraction. */
        CANONICAL
    }

    /**
     * Creates extraction task with fail-fast behavior enabled by default.
     */
    @Inject
    public SchematronSelExtractTask() {
        getFailOnError().convention(true);
        getJsonMode().convention("auto");
        getXirFormat().convention("compact");
        getPhase().convention("#DEFAULT");
    }

    /**
     * Schematron schema containing SEL rule metadata.
     *
     * @return schema file property
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSchema();

    /**
     * Optional precompiled extraction stylesheet.
     *
     * @return stylesheet property
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getStyle();

    /**
     * Canonical sources used for extraction.
     *
     * @return source file collection
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    /**
     * Output directory root for grouped extraction documents.
     *
     * @return output directory property
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /**
     * Group-to-relative-path mapping for emitted SEL documents.
     *
     * @return group output mapping
     */
    @Input
    public abstract MapProperty<String, String> getGroupOutputs();

    /**
     * Optional SEL output namespace URI override for generated extraction output.
     *
     * <p>When set, overrides schema-level SEL output namespace defaults.</p>
     *
     * @return optional namespace URI
     */
    @Input
    @Optional
    public abstract Property<String> getOutputNamespaceUri();

    /**
     * Optional SEL output namespace prefix override for generated extraction output.
     *
     * <p>Use empty string to emit SEL elements in default namespace.</p>
     *
     * @return optional namespace prefix
     */
    @Input
    @Optional
    public abstract Property<String> getOutputNamespacePrefix();

    /**
     * Controls build failure behavior when extraction errors occur.
     *
     * @return fail-on-error property
     */
    @Input
    public abstract Property<Boolean> getFailOnError();

    /**
     * Optional JSON mode controlling how .json input is parsed.
     *
     * @return JSON mode property: auto, native, canonical
     */
    @Input
    @Optional
    public abstract Property<String> getJsonMode();

    /**
     * Canonical output-mode alias for {@link #getJsonMode()}.
     *
     * @return JSON routing mode property
     */
    @Internal
    public Property<String> getOutputMode() {
        return getJsonMode();
    }

    /**
     * Backward-compatible alias for {@link #getOutputMode()}.
     *
     * @return JSON routing mode property
     */
    @Internal
    public Property<String> getMode() {
        return getOutputMode();
    }

    /**
     * Optional XIR output format used when emitting {@code .xir} group outputs.
     *
     * <p>Supported values are {@code compact} (default) and {@code beautified}.</p>
     *
     * @return XIR serializer output format property
     */
    @Input
    @Optional
    public abstract Property<String> getXirFormat();

    /**
     * Canonical output-format alias for {@link #getXirFormat()}.
     *
     * @return XIR serializer output format property
     */
    @Internal
    public Property<String> getOutputFormat() {
        return getXirFormat();
    }

    /**
     * Backward-compatible alias for {@link #getOutputFormat()}.
     *
     * @return XIR serializer output format property
     */
    @Internal
    public Property<String> getXformat() {
        return getOutputFormat();
    }

    /**
     * Active Schematron phase used for on-the-fly SEL compilation.
     *
     * <p>Supported values:</p>
     * <ul>
     *     <li>{@code #DEFAULT} (default) — uses {@code sch:schema/@defaultPhase}, else all rules.</li>
     *     <li>{@code #ALL} — includes all SEL-annotated rules.</li>
     *     <li>explicit phase id — includes patterns activated by that phase.</li>
     * </ul>
     *
     * <p>When {@link #getStyle()} is configured, phase filtering is expected to be applied at compile time
     * of that precompiled stylesheet and this property is ignored by extraction runtime.</p>
     *
     * @return optional phase selector
     */
    @Input
    @Optional
    public abstract Property<String> getPhase();

    /**
     * Sets Schematron schema file.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void schema(Object path) {
        File file = getProject().file(path);
        if (!file.exists()) {
            throw new GradleException("Schematron schema does not exist: " + file);
        }
        getSchema().set(file);
    }

    /**
     * Sets precompiled extraction stylesheet file.
     *
     * @param path file notation accepted by {@code Project.file}.
     */
    public void style(Object path) {
        getStyle().set(getProject().file(path));
    }

    /**
     * Adds input source(s) using Gradle file notation.
     *
     * @param source file, directory, fileTree, or collection.
     */
    public void source(Object source) {
        getSourceFiles().from(source);
    }

    /**
     * Adds source file tree include/exclude configuration.
     *
     * @param sourceDir root directory.
     * @param action include/exclude action.
     */
    public void source(Object sourceDir, org.gradle.api.Action<? super PatternFilterable> action) {
        org.gradle.api.file.ConfigurableFileTree tree = getProject().fileTree(sourceDir);
        action.execute(tree);
        source(tree);
    }

    /**
     * Configures output path mapping for a logical group.
     *
     * @param group SEL group key.
     * @param relativePath output path under each input-stem root.
     */
    public void groupOutput(String group, String relativePath) {
        getGroupOutputs().put(group, relativePath);
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
     * @param value prefix (empty string for default namespace output)
     */
    public void outputNamespacePrefix(String value) {
        getOutputNamespacePrefix().set(value);
    }

    /**
     * Sets JSON routing mode (Gradle DSL friendly).
     *
     * @param mode one of auto, native, canonical
     */
    public void jsonMode(String mode) {
        getJsonMode().set(mode);
    }

    /**
     * Sets JSON routing mode using canonical DSL name.
     *
     * @param mode one of auto, native, canonical
     */
    public void outputMode(String mode) {
        getOutputMode().set(mode);
    }

    /**
     * Groovy/Kotlin assignment-style alias for {@link #outputMode(String)}.
     *
     * @param mode one of auto, native, canonical
     */
    public void setOutputMode(String mode) {
        outputMode(mode);
    }

    /**
     * Backward-compatible alias for {@link #outputMode(String)}.
     *
     * @param mode one of auto, native, canonical
     */
    public void mode(String mode) {
        outputMode(mode);
    }

    /**
     * Groovy/Kotlin assignment-style alias for {@link #mode(String)}.
     *
     * @param mode one of auto, native, canonical
     */
    public void setMode(String mode) {
        mode(mode);
    }

    /**
     * Sets S-expression output format for {@code .xir} group outputs (Gradle DSL friendly).
     *
     * @param format one of compact, beautified
     */
    public void xirFormat(String format) {
        getXirFormat().set(format);
    }

    /**
     * Sets S-expression output format using canonical DSL name.
     *
     * @param format one of compact, beautified
     */
    public void outputFormat(String format) {
        getOutputFormat().set(format);
    }

    /**
     * Groovy/Kotlin assignment-style alias for {@link #outputFormat(String)}.
     *
     * @param format one of compact, beautified
     */
    public void setOutputFormat(String format) {
        outputFormat(format);
    }

    /**
     * Backward-compatible alias for {@link #outputFormat(String)}.
     *
     * @param format one of compact, beautified
     */
    public void xformat(String format) {
        outputFormat(format);
    }

    /**
     * Groovy/Kotlin assignment-style alias for {@link #xformat(String)}.
     *
     * @param format one of compact, beautified
     */
    public void setXformat(String format) {
        xformat(format);
    }

    /**
     * Sets Schematron phase used by on-the-fly SEL compilation.
     *
     * @param value phase id, {@code #DEFAULT}, or {@code #ALL}
     */
    public void phase(String value) {
        getPhase().set(value);
    }

    /**
     * Runs SEL extraction for all configured source files.
     */
    @TaskAction
    public void extract() {
        Set<File> rawInputs = new LinkedHashSet<>(getSourceFiles().getFiles());
        if (rawInputs.isEmpty()) {
            throw new GradleException(
                    "No input files configured. Use source(...) to provide canonical XML/XIR/JSON"
                            + " files.");
        }

        List<File> inputs = new ArrayList<>(rawInputs);
        inputs.sort(Comparator.comparing(File::getAbsolutePath));

        try {
            Files.createDirectories(getOutputDir().get().getAsFile().toPath());
            RuntimeStylesheet runtimeStylesheet = resolveRuntimeStylesheet();
            for (File input : inputs) {
                runExtraction(runtimeStylesheet, input);
            }
        } catch (Exception e) {
            if (getFailOnError().get()) {
                throw new GradleException("SEL extraction failed", e);
            }
            getLogger().error("SEL extraction failed but failOnError=false", e);
        }
    }

    private RuntimeStylesheet resolveRuntimeStylesheet() throws Exception {
        Document schemaDoc = parseSchema(getSchema().get().getAsFile());
        SelRuleSet collected = SelRuleCollector.collect(schemaDoc);
        List<SelRuleDescriptor> allRules = collected.rules();
        List<String> groups = collectGroups(allRules);

        if (getStyle().isPresent()) {
            String phase = normalizedPhase();
            if (!"#DEFAULT".equals(phase)) {
                getLogger()
                        .warn(
                                "Phase '{}' ignored because precompiled style is configured via"
                                        + " style(...)",
                                phase);
            }
            return new RuntimeStylesheet(getStyle().get().getAsFile().toPath(), groups);
        }

        List<SelRuleDescriptor> activeRules = collected.rulesForPhase(normalizedPhase());
        SelOutputConfig outputConfig =
                SelOutputConfig.resolve(
                        collected.outputConfig(),
                        getOutputNamespaceUri().isPresent() ? getOutputNamespaceUri().get() : null,
                        getOutputNamespacePrefix().isPresent()
                                ? getOutputNamespacePrefix().get()
                                : null);
        String stylesheetXml =
                SelStylesheetCompiler.render(
                        activeRules, getGroupOutputs().getOrElse(Map.of()), outputConfig);
        Path temp = Files.createTempFile("sel-compiled-", ".xsl");
        Files.writeString(temp, stylesheetXml, StandardCharsets.UTF_8);
        temp.toFile().deleteOnExit();
        return new RuntimeStylesheet(temp, collectGroups(activeRules));
    }

    private void runExtraction(RuntimeStylesheet runtimeStylesheet, File inputFile)
            throws Exception {
        Path base = getOutputDir().get().getAsFile().toPath().resolve(stem(inputFile.getName()));
        Files.createDirectories(base);

        Processor processor = new Processor(false);
        SaxonXirResolvers.configure(processor);
        XsltCompiler compiler = processor.newXsltCompiler();
        SaxonXirResolvers.configure(compiler);
        XsltExecutable executable =
                compiler.compile(stylesheetSource(runtimeStylesheet.stylesheet().toFile()));
        XsltTransformer transformer = executable.load();
        transformer.setSource(sourceForInput(inputFile));
        transformer.setParameter(
                new QName("source-document"), new XdmAtomicValue(inputFile.getName()));
        transformer.setBaseOutputURI(base.toUri().toString());

        Map<String, Path> groupTargets = new LinkedHashMap<>();
        for (String group : runtimeStylesheet.groups()) {
            String configured =
                    getGroupOutputs()
                            .getOrElse(Map.of())
                            .getOrDefault(group, "sel/" + group + ".xml");
            Path target = base.resolve(configured).normalize();
            groupTargets.put(group, target);
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            transformer.setParameter(new QName("output-" + group), new XdmAtomicValue(configured));
        }

        StringWriter sink = new StringWriter();
        Serializer serializer = processor.newSerializer(sink);
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        transformer.setDestination(serializer);
        transformer.transform();

        for (Path target : groupTargets.values()) {
            if (isXirPath(target)) {
                convertXmlFileToXir(target);
            }
        }
    }

    private List<String> collectGroups(List<SelRuleDescriptor> rules) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        for (SelRuleDescriptor rule : rules) {
            groups.add(rule.group());
        }
        if (groups.isEmpty()) {
            groups.add("default");
        }
        return new ArrayList<>(groups);
    }

    private Source sourceForInput(File inputFile) {
        if (isXirFile(inputFile)) {
            return new SAXSource(new XirReader(), new InputSource(inputFile.toURI().toString()));
        }
        if (useCanonicalJsonInput(inputFile)) {
            return new SAXSource(
                    new JsonCanonicalXmlReader(), new InputSource(inputFile.toURI().toString()));
        }
        return new StreamSource(inputFile);
    }

    private Source stylesheetSource(File stylesheetFile) {
        if (isXirFile(stylesheetFile)) {
            return new SAXSource(
                    new XirReader(), new InputSource(stylesheetFile.toURI().toString()));
        }
        return new StreamSource(stylesheetFile);
    }

    private boolean isXirFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".xir");
    }

    private boolean isJsonFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private boolean useCanonicalJsonInput(File inputFile) {
        if (!isJsonFile(inputFile)) {
            return false;
        }
        JsonMode mode = resolveJsonMode();
        return mode == JsonMode.AUTO || mode == JsonMode.CANONICAL;
    }

    private JsonMode resolveJsonMode() {
        String configured = getJsonMode().getOrElse("auto");
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "native" -> JsonMode.NATIVE;
            case "canonical" -> JsonMode.CANONICAL;
            case "auto" -> JsonMode.AUTO;
            default -> throw new GradleException(
                    "Unsupported jsonMode '"
                            + configured
                            + "'. Supported values: auto, native, canonical");
        };
    }

    private XirSerializer.OutputFormat resolveXirOutputFormat() {
        String configured = getXirFormat().getOrElse("compact");
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if ("pretty".equals(normalized)) {
            normalized = "beautified";
        }
        if (!SUPPORTED_SEXPR_FORMATS.contains(normalized)) {
            throw new GradleException(
                    "Unsupported xirFormat '"
                            + configured
                            + "'. Supported values: compact, beautified");
        }
        return "beautified".equals(normalized)
                ? XirSerializer.OutputFormat.BEAUTIFIED
                : XirSerializer.OutputFormat.COMPACT;
    }

    private boolean isXirPath(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        return fileName.toString().toLowerCase(Locale.ROOT).endsWith(".xir");
    }

    private void convertXmlFileToXir(Path targetFile) throws Exception {
        Path targetParent = targetFile.getParent();
        if (targetParent == null || targetFile.getFileName() == null) {
            throw new GradleException(
                    "Target file must include parent directory and file name: " + targetFile);
        }
        Path tempFile = Files.createTempFile(targetParent, "xir", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                Document document = parseXmlDocument(targetFile.toFile());
                normalizeNamespacesForXir(document);
                Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
                SAXResult destination =
                        new SAXResult(new XirSerializer(writer, resolveXirOutputFormat()));
                transformer.transform(new DOMSource(document), destination);
            }
            moveReplacing(targetFile, tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void moveReplacing(Path targetFile, Path sourceFile) throws Exception {
        try {
            Files.move(
                    sourceFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String normalizedPhase() {
        String configured = getPhase().getOrElse("#DEFAULT");
        return configured.trim().isEmpty() ? "#DEFAULT" : configured.trim();
    }

    private Document parseXmlDocument(File sourceFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(sourceFile);
    }

    private void normalizeNamespacesForXir(Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }

        Map<String, String> usedMappings = new LinkedHashMap<>();
        collectUsedNamespaceMappings(root, usedMappings);
        hoistNamespacesToRoot(root, usedMappings);

        Map<String, String> rootScope = namespaceDeclarations(root);
        Node child = root.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                pruneRedundantNamespaceDeclarations((Element) child, rootScope);
            }
            child = child.getNextSibling();
        }
    }

    private void collectUsedNamespaceMappings(Element element, Map<String, String> mappings) {
        registerNodeNamespace(element, mappings);
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (XMLNS_URI.equals(attribute.getNamespaceURI())) {
                continue;
            }
            registerNodeNamespace(attribute, mappings);
        }

        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collectUsedNamespaceMappings((Element) child, mappings);
            }
            child = child.getNextSibling();
        }
    }

    private void registerNodeNamespace(Node node, Map<String, String> mappings) {
        String prefix = node.getPrefix();
        String namespaceUri = node.getNamespaceURI();
        if (prefix == null || prefix.isBlank() || namespaceUri == null || namespaceUri.isBlank()) {
            return;
        }
        mappings.putIfAbsent(prefix, namespaceUri);
    }

    private void hoistNamespacesToRoot(Element root, Map<String, String> usedMappings) {
        List<String> prefixes = new ArrayList<>(usedMappings.keySet());
        Collections.sort(prefixes);
        if (prefixes.remove("sel")) {
            prefixes.add(0, "sel");
        }
        if (prefixes.remove("c")) {
            int index = prefixes.isEmpty() ? 0 : 1;
            prefixes.add(index, "c");
        }
        for (String prefix : prefixes) {
            String uri = usedMappings.get(prefix);
            if (uri == null || uri.isBlank()) {
                continue;
            }
            String existing = root.lookupNamespaceURI(prefix);
            if (!uri.equals(existing)) {
                root.setAttributeNS(XMLNS_URI, "xmlns:" + prefix, uri);
            }
        }
    }

    private Map<String, String> namespaceDeclarations(Element element) {
        Map<String, String> declarations = new LinkedHashMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (!XMLNS_URI.equals(attribute.getNamespaceURI())) {
                continue;
            }
            String localName = attribute.getLocalName();
            String prefix = "xmlns".equals(localName) ? "" : localName;
            declarations.put(prefix, attribute.getNodeValue());
        }
        return declarations;
    }

    private void pruneRedundantNamespaceDeclarations(
            Element element, Map<String, String> inheritedScope) {
        Map<String, String> localDeclarations = namespaceDeclarations(element);
        List<Node> toRemove = new ArrayList<>();
        for (Map.Entry<String, String> declaration : localDeclarations.entrySet()) {
            String prefix = declaration.getKey();
            String uri = declaration.getValue();
            String inherited = inheritedScope.get(prefix);
            if (inherited != null && inherited.equals(uri)) {
                String attributeName = prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix;
                Node attribute = element.getAttributeNode(attributeName);
                if (attribute != null) {
                    toRemove.add(attribute);
                }
            }
        }
        for (Node attribute : toRemove) {
            element.removeAttributeNode((org.w3c.dom.Attr) attribute);
            String localName = attribute.getLocalName();
            String prefix = "xmlns".equals(localName) ? "" : localName;
            localDeclarations.remove(prefix);
        }

        Map<String, String> scope = new LinkedHashMap<>(inheritedScope);
        scope.putAll(localDeclarations);

        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                pruneRedundantNamespaceDeclarations((Element) child, scope);
            }
            child = child.getNextSibling();
        }
    }

    private Document parseSchema(File schemaFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(schemaFile);
    }

    private String stem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private record RuntimeStylesheet(Path stylesheet, List<String> groups) {}
}
