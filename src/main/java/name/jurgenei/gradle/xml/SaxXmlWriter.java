/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Minimal SAX XML writer with pretty-print output and helper methods for namespace-aware emission.
 */
public final class SaxXmlWriter {
    private static final String INDENT_AMOUNT_PROPERTY =
            "{http://xml.apache.org/xslt}indent-amount";

    private final StringWriter output;
    private final TransformerHandler handler;
    private final Deque<List<String>> elementPrefixMappings;

    private SaxXmlWriter(StringWriter output, TransformerHandler handler) {
        this.output = output;
        this.handler = handler;
        this.elementPrefixMappings = new ArrayDeque<>();
    }

    /**
     * Creates pretty-printing SAX writer with secure transformer defaults.
     *
     * @return initialized XML writer instance
     */
    public static SaxXmlWriter createPretty() {
        try {
            StringWriter output = new StringWriter();
            SAXTransformerFactory factory =
                    (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            TransformerHandler handler = factory.newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(INDENT_AMOUNT_PROPERTY, "2");
            handler.setResult(new StreamResult(output));
            return new SaxXmlWriter(output, handler);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SAX XML writer", e);
        }
    }

    /**
     * Starts XML document emission.
     *
     * @throws SAXException if downstream handler fails
     */
    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    /**
     * Completes XML document emission.
     *
     * @throws SAXException if downstream handler fails
     */
    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    /**
     * Starts element with optional namespace declarations and attributes.
     *
     * @param namespaceUri namespace URI or empty string
     * @param localName local element name
     * @param qName qualified element name
     * @param attributes optional element attributes
     * @param namespaceMappings optional prefix to URI mappings declared on this element
     * @throws SAXException if downstream handler fails
     */
    public void startElement(
            String namespaceUri,
            String localName,
            String qName,
            List<XmlAttribute> attributes,
            Map<String, String> namespaceMappings)
            throws SAXException {
        List<String> startedPrefixes = new ArrayList<>();
        if (namespaceMappings != null && !namespaceMappings.isEmpty()) {
            for (Map.Entry<String, String> mapping : namespaceMappings.entrySet()) {
                String prefix = mapping.getKey() == null ? "" : mapping.getKey();
                String uri = mapping.getValue() == null ? "" : mapping.getValue();
                handler.startPrefixMapping(prefix, uri);
                startedPrefixes.add(prefix);
            }
        }
        elementPrefixMappings.push(startedPrefixes);
        handler.startElement(
                safeNamespace(namespaceUri),
                safeLocal(localName, qName),
                qName,
                toAttributes(attributes));
    }

    /**
     * Ends element and closes namespace declarations opened for it.
     *
     * @param namespaceUri namespace URI or empty string
     * @param localName local element name
     * @param qName qualified element name
     * @throws SAXException if downstream handler fails
     */
    public void endElement(String namespaceUri, String localName, String qName)
            throws SAXException {
        handler.endElement(safeNamespace(namespaceUri), safeLocal(localName, qName), qName);
        List<String> startedPrefixes =
                elementPrefixMappings.isEmpty()
                        ? Collections.emptyList()
                        : elementPrefixMappings.pop();
        for (int i = startedPrefixes.size() - 1; i >= 0; i--) {
            handler.endPrefixMapping(startedPrefixes.get(i));
        }
    }

    /**
     * Emits empty element with optional attributes and namespace mappings.
     *
     * @param namespaceUri namespace URI or empty string
     * @param localName local element name
     * @param qName qualified element name
     * @param attributes optional element attributes
     * @param namespaceMappings optional prefix to URI mappings declared on this element
     * @throws SAXException if downstream handler fails
     */
    public void emptyElement(
            String namespaceUri,
            String localName,
            String qName,
            List<XmlAttribute> attributes,
            Map<String, String> namespaceMappings)
            throws SAXException {
        startElement(namespaceUri, localName, qName, attributes, namespaceMappings);
        endElement(namespaceUri, localName, qName);
    }

    /**
     * Emits text node.
     *
     * @param text text content; blank values are ignored
     * @throws SAXException if downstream handler fails
     */
    public void text(String text) throws SAXException {
        if (text == null || text.isEmpty()) {
            return;
        }
        char[] chars = text.toCharArray();
        handler.characters(chars, 0, chars.length);
    }

    /**
     * Emits XML comment.
     *
     * @param text comment text; blank values are ignored
     * @throws SAXException if downstream handler fails
     */
    public void comment(String text) throws SAXException {
        if (text == null || text.isEmpty()) {
            return;
        }
        char[] chars = text.toCharArray();
        handler.comment(chars, 0, chars.length);
    }

    /**
     * Parses and appends XML fragment under secure parser settings.
     *
     * @param fragment XML fragment to append
     * @param namespaceHints optional namespace hints used while wrapping fragment root
     */
    public void appendFragment(String fragment, Map<String, String> namespaceHints) {
        if (fragment == null || fragment.isBlank()) {
            return;
        }
        try {
            String wrapped = wrapFragment(fragment, namespaceHints);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Node root =
                    factory.newDocumentBuilder()
                            .parse(new InputSource(new StringReader(wrapped)))
                            .getDocumentElement();
            Node child = root.getFirstChild();
            while (child != null) {
                emitNode(child);
                child = child.getNextSibling();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to append XML fragment", e);
        }
    }

    /**
     * Returns rendered XML output.
     *
     * @return serialized XML string
     */
    public String build() {
        return output.toString();
    }

    /**
     * Creates non-namespaced attribute.
     *
     * @param name attribute name
     * @param value attribute value
     * @return attribute descriptor
     */
    public static XmlAttribute attr(String name, String value) {
        return new XmlAttribute("", name, name, value == null ? "" : value);
    }

    /**
     * Creates namespaced attribute.
     *
     * @param namespaceUri namespace URI or null
     * @param localName local name or null
     * @param qName qualified name
     * @param value attribute value
     * @return attribute descriptor
     */
    public static XmlAttribute attr(
            String namespaceUri, String localName, String qName, String value) {
        return new XmlAttribute(
                namespaceUri == null ? "" : namespaceUri,
                localName == null || localName.isBlank() ? qName : localName,
                qName,
                value == null ? "" : value);
    }

    private Attributes toAttributes(List<XmlAttribute> attributes) {
        AttributesImpl result = new AttributesImpl();
        if (attributes == null) {
            return result;
        }
        for (XmlAttribute attribute : attributes) {
            result.addAttribute(
                    safeNamespace(attribute.namespaceUri()),
                    safeLocal(attribute.localName(), attribute.qName()),
                    attribute.qName(),
                    "CDATA",
                    attribute.value());
        }
        return result;
    }

    private void emitNode(Node node) throws SAXException {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> emitElement(node);
            case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text(node.getNodeValue());
            case Node.COMMENT_NODE -> comment(node.getNodeValue());
            default -> {
                // Ignore processing instructions and unsupported nodes in template fragments.
            }
        }
    }

    private void emitElement(Node node) throws SAXException {
        NamedNodeMap nodeAttributes = node.getAttributes();
        List<String> startedPrefixes = new ArrayList<>();
        String namespaceUri = safeNamespace(node.getNamespaceURI());
        String qName = node.getNodeName();
        String localName = safeLocal(node.getLocalName(), qName);
        String elementPrefix = prefixOf(qName);
        if (!namespaceUri.isBlank() && !startedPrefixes.contains(elementPrefix)) {
            handler.startPrefixMapping(elementPrefix, namespaceUri);
            startedPrefixes.add(elementPrefix);
        }
        AttributesImpl attributes = new AttributesImpl();
        if (nodeAttributes != null) {
            for (int i = 0; i < nodeAttributes.getLength(); i++) {
                Node attribute = nodeAttributes.item(i);
                String attrNamespace = safeNamespace(attribute.getNamespaceURI());
                String attrQName = attribute.getNodeName();
                String attrLocal = safeLocal(attribute.getLocalName(), attrQName);
                if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attrNamespace)) {
                    String prefix = XMLConstants.XMLNS_ATTRIBUTE.equals(attrQName) ? "" : attrLocal;
                    handler.startPrefixMapping(prefix, attribute.getNodeValue());
                    startedPrefixes.add(prefix);
                } else {
                    String attrPrefix = prefixOf(attrQName);
                    if (!attrNamespace.isBlank()
                            && !attrPrefix.isBlank()
                            && !startedPrefixes.contains(attrPrefix)) {
                        handler.startPrefixMapping(attrPrefix, attrNamespace);
                        startedPrefixes.add(attrPrefix);
                    }
                    attributes.addAttribute(
                            attrNamespace, attrLocal, attrQName, "CDATA", attribute.getNodeValue());
                }
            }
        }
        elementPrefixMappings.push(startedPrefixes);
        handler.startElement(namespaceUri, localName, qName, attributes);

        Node child = node.getFirstChild();
        while (child != null) {
            emitNode(child);
            child = child.getNextSibling();
        }

        handler.endElement(namespaceUri, localName, qName);
        List<String> declared = elementPrefixMappings.pop();
        for (int i = declared.size() - 1; i >= 0; i--) {
            handler.endPrefixMapping(declared.get(i));
        }
    }

    private String wrapFragment(String fragment, Map<String, String> namespaceHints) {
        StringBuilder xml = new StringBuilder("<fragment");
        if (namespaceHints != null) {
            for (Map.Entry<String, String> mapping : namespaceHints.entrySet()) {
                String prefix = mapping.getKey() == null ? "" : mapping.getKey();
                String uri = mapping.getValue() == null ? "" : mapping.getValue();
                if (prefix.isBlank()) {
                    xml.append(" xmlns=\"").append(escapeAttribute(uri)).append("\"");
                } else {
                    xml.append(" xmlns:")
                            .append(prefix)
                            .append("=\"")
                            .append(escapeAttribute(uri))
                            .append("\"");
                }
            }
        }
        xml.append(">").append(fragment).append("</fragment>");
        return xml.toString();
    }

    private String safeNamespace(String namespaceUri) {
        return namespaceUri == null ? "" : namespaceUri;
    }

    private String safeLocal(String localName, String qName) {
        if (localName != null && !localName.isBlank()) {
            return localName;
        }
        int separator = qName.indexOf(':');
        return separator >= 0 && separator + 1 < qName.length()
                ? qName.substring(separator + 1)
                : qName;
    }

    private String prefixOf(String qName) {
        int separator = qName.indexOf(':');
        return separator > 0 ? qName.substring(0, separator) : "";
    }

    private String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Attribute descriptor used by element helper methods.
     *
     * @param namespaceUri attribute namespace URI
     * @param localName attribute local name
     * @param qName attribute qualified name
     * @param value attribute value
     */
    public record XmlAttribute(String namespaceUri, String localName, String qName, String value) {}
}
