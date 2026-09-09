package name.jurgenei.gradle.xml.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import name.jurgenei.xml.sexpr.SExpressionSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SAX content handler that writes canonical JSON element representation.
 */
public final class JsonCanonicalSerializer implements ContentHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Writer writer;
    private final SExpressionSerializer.OutputFormat format;
    private final Deque<ElementFrame> stack = new ArrayDeque<>();

    /**
     * Creates serializer with compact output.
     *
     * @param writer destination writer
     */
    public JsonCanonicalSerializer(Writer writer) {
        this(writer, SExpressionSerializer.OutputFormat.COMPACT);
    }

    /**
     * Creates serializer with explicit output formatting.
     *
     * @param writer destination writer
     * @param format compact or beautified output
     */
    public JsonCanonicalSerializer(Writer writer, SExpressionSerializer.OutputFormat format) {
        this.writer = writer;
        this.format = format == null ? SExpressionSerializer.OutputFormat.COMPACT : format;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new SAXException("Failed to flush canonical JSON output", e);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
    }

    @Override
    public void endPrefixMapping(String prefix) {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        String name = (qName != null && !qName.isBlank()) ? qName : localName;
        ElementFrame frame = new ElementFrame(name);
        for (int i = 0; i < atts.getLength(); i++) {
            String attrName = atts.getQName(i);
            if (attrName == null || attrName.isBlank()) {
                attrName = atts.getLocalName(i);
            }
            frame.attributes.put(attrName, atts.getValue(i));
        }
        stack.push(frame);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        ElementFrame frame = stack.pop();
        if (stack.isEmpty()) {
            writeRoot(frame);
            return;
        }
        stack.peek().children.add(Child.element(frame));
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (length == 0 || stack.isEmpty()) {
            return;
        }
        String text = new String(ch, start, length);
        if (text.isBlank()) {
            return;
        }
        stack.peek().children.add(Child.text(text));
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    @Override
    public void processingInstruction(String target, String data) {
    }

    @Override
    public void skippedEntity(String name) {
    }

    private void writeRoot(ElementFrame frame) throws SAXException {
        try {
            ObjectNode root = toElementNode(frame);
            if (format == SExpressionSerializer.OutputFormat.BEAUTIFIED) {
                writer.write(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            } else {
                writer.write(MAPPER.writeValueAsString(root));
            }
            writer.write(System.lineSeparator());
        } catch (JsonProcessingException e) {
            throw new SAXException("Failed to serialize canonical JSON output", e);
        } catch (IOException e) {
            throw new SAXException("Failed to write canonical JSON output", e);
        }
    }

    private ObjectNode toElementNode(ElementFrame frame) {
        ObjectNode elementNode = MAPPER.createObjectNode();
        elementNode.put("type", "element");
        elementNode.put("name", frame.name);

        if (!frame.attributes.isEmpty()) {
            ObjectNode attributesNode = MAPPER.createObjectNode();
            for (Map.Entry<String, String> entry : frame.attributes.entrySet()) {
                attributesNode.put(entry.getKey(), entry.getValue());
            }
            elementNode.set("attributes", attributesNode);
        }

        ArrayNode childrenNode = MAPPER.createArrayNode();
        for (Child child : frame.children) {
            if (child.element != null) {
                childrenNode.add(toElementNode(child.element));
            } else {
                ObjectNode textNode = MAPPER.createObjectNode();
                textNode.put("type", "text");
                textNode.put("value", child.text);
                childrenNode.add(textNode);
            }
        }
        elementNode.set("children", childrenNode);
        return elementNode;
    }

    private static final class ElementFrame {
        private final String name;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<Child> children = new ArrayList<>();

        private ElementFrame(String name) {
            this.name = name;
        }
    }

    private static final class Child {
        private final ElementFrame element;
        private final String text;

        private Child(ElementFrame element, String text) {
            this.element = element;
            this.text = text;
        }

        private static Child element(ElementFrame element) {
            return new Child(element, null);
        }

        private static Child text(String text) {
            return new Child(null, text);
        }
    }
}

